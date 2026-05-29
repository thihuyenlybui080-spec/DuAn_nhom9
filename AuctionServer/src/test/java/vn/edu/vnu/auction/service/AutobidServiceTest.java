package vn.edu.vnu.auction.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.auction.ClientRegistry;
import vn.edu.vnu.auction.dao.AutoBidDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.auto_bidding.AutoBidConfig;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.util.AuctionManager;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link AutobidService}.
 * <p>
 * Kiểm tra các chức năng kích hoạt, hủy kích hoạt, và xử lý hàng đợi đặt giá tự động (Auto-bid),
 * đảm bảo luồng hoạt động chính xác khi có các thay đổi về giá, bao gồm cả các nhánh ngoại lệ.
 * </p>
 */
class AutobidServiceTest {

    private AutobidService autobidService;

    /**
     * Dọn dẹp trạng thái Singleton và làm trống các Map chứa luồng xử lý trước mỗi bài test.
     */
    @BeforeEach
    void setUp() throws Exception {
        // Use Reflection to reset the instance of AutobidService to null
        Field instanceField = AutobidService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        autobidService = AutobidService.getInstance();

        // Clear queues and processing Maps to avoid stale data
        Field queuesField = AutobidService.class.getDeclaredField("queues");
        queuesField.setAccessible(true);
        ((Map<?, ?>) queuesField.get(autobidService)).clear();

        Field processingField = AutobidService.class.getDeclaredField("processing");
        processingField.setAccessible(true);
        ((Map<?, ?>) processingField.get(autobidService)).clear();
    }

    /**
     * Dọn dẹp lại hệ thống sau khi test xong.
     */
    @AfterEach
    void tearDown() throws Exception {
        Field instanceField = AutobidService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    /**
     * Kiểm tra cơ chế Singleton của lớp AutobidService.
     */
    @Test
    void testGetInstance() {
        AutobidService instance1 = AutobidService.getInstance();
        AutobidService instance2 = AutobidService.getInstance();

        assertNotNull(instance1, "Instance must not be null.");
        assertSame(instance1, instance2, "Only one instance of AutobidService is allowed.");
    }

    /**
     * Kiểm tra chức năng kích hoạt đặt giá tự động (enableAutoBid) khi người dùng chưa phải là người dẫn đầu.
     */
    @Test
    void testEnableAutoBid_NotLeading_PlacesInitialBid() {
        Bidder mockBidder = Mockito.mock(Bidder.class);
        Mockito.when(mockBidder.getId()).thenReturn(1);
        Mockito.when(mockBidder.getName()).thenReturn("Tester");

        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getId()).thenReturn(100);
        Mockito.when(mockAuction.getCurrentPrice()).thenReturn(50.0);
        Mockito.when(mockAuction.getHighestBidder()).thenReturn(null); // No one is leading

        AutoBidConfig config = new AutoBidConfig(500.0, 10.0);

        BidService mockBidService = Mockito.mock(BidService.class);

        try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class);
             MockedStatic<BidService> mockedBidServiceStatic = Mockito.mockStatic(BidService.class)) {

            mockedBidServiceStatic.when(BidService::getInstance).thenReturn(mockBidService);

            autobidService.enableAutoBid(mockBidder, mockAuction, config);

            // Verify configuration is saved to DB
            mockedDao.verify(() -> AutoBidDAO.saveAutoBid(100, 1, 500.0, 10.0), Mockito.times(1));
            // Verify BidService is called to increase bid immediately (50 + 10 = 60)
            Mockito.verify(mockBidService, Mockito.times(1)).processAutoBid(mockBidder, mockAuction, 60.0);

            assertTrue(autobidService.isAutoBidActive(100, 1), "Auto-bid must be recorded as active.");
        }
    }

    /**
     * Kiểm tra chức năng kích hoạt đặt giá tự động khi người dùng ĐÃ LÀ người dẫn đầu.
     */
    @Test
    void testEnableAutoBid_AlreadyLeading_SkipsInitialBid() {
        Bidder mockBidder = Mockito.mock(Bidder.class);
        Mockito.when(mockBidder.getId()).thenReturn(2);
        Mockito.when(mockBidder.getName()).thenReturn("Leading Bidder");

        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getId()).thenReturn(200);
        Mockito.when(mockAuction.getCurrentPrice()).thenReturn(100.0);
        Mockito.when(mockAuction.getHighestBidder()).thenReturn(mockBidder); // This person is currently leading

        AutoBidConfig config = new AutoBidConfig(1000.0, 20.0);

        BidService mockBidService = Mockito.mock(BidService.class);

        try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class);
             MockedStatic<BidService> mockedBidServiceStatic = Mockito.mockStatic(BidService.class)) {

            mockedBidServiceStatic.when(BidService::getInstance).thenReturn(mockBidService);

            autobidService.enableAutoBid(mockBidder, mockAuction, config);

            mockedDao.verify(() -> AutoBidDAO.saveAutoBid(200, 2, 1000.0, 20.0), Mockito.times(1));
            // Must absolutely not call processAutoBid
            Mockito.verify(mockBidService, Mockito.never()).processAutoBid(Mockito.any(), Mockito.any(), Mockito.anyDouble());
        }
    }

    /**
     * Kiểm tra chức năng xử lý hàng đợi (processQueue) sẽ tự động dừng (deactivate)
     * nếu giá tiếp theo vượt quá mức giá trần (maxBid) mà người dùng cài đặt.
     */
    @Test
    void testProcessQueue_MaxBidReached() {
        Bidder mockBidder = Mockito.mock(Bidder.class);
        Mockito.when(mockBidder.getId()).thenReturn(3);

        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getId()).thenReturn(300);
        // Current price is 95, increment is 10 -> Next bid is 105
        Mockito.when(mockAuction.getCurrentPrice()).thenReturn(95.0);

        // User only allows maxBid of 100
        AutoBidConfig config = new AutoBidConfig(100.0, 10.0);

        BidService mockBidService = Mockito.mock(BidService.class);
        AuctionManager mockAuctionManager = Mockito.mock(AuctionManager.class);

        try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class);
             MockedStatic<BidService> mockedBidServiceStatic = Mockito.mockStatic(BidService.class);
             MockedStatic<AuctionManager> mockedManagerStatic = Mockito.mockStatic(AuctionManager.class)) {

            mockedManagerStatic.when(AuctionManager::getInstance).thenReturn(mockAuctionManager);
            Mockito.when(mockAuctionManager.getActive(300)).thenReturn(mockAuction);

            // Enable AutoBid
            autobidService.enableAutoBid(mockBidder, mockAuction, config);

            // Call queue processing function
            autobidService.processQueue(300, "Another Guy");

            // Verify AutoBid was deactivated due to hitting the ceiling
            assertFalse(autobidService.isAutoBidActive(300, 3), "Auto-bid must be deactivated due to exceeding the maximum bid.");
        }
    }

    /**
     * Kiểm tra chức năng vô hiệu hóa (disableAutoBid) và dọn dẹp hàng đợi (clearAuction).
     */
    @Test
    void testDisableAndClearAuction() {
        Bidder mockBidder = Mockito.mock(Bidder.class);
        Mockito.when(mockBidder.getId()).thenReturn(4);

        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getId()).thenReturn(400);

        AutoBidConfig config = new AutoBidConfig(500.0, 10.0);

        try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class)) {
            // Register
            autobidService.enableAutoBid(mockBidder, mockAuction, config);
            assertTrue(autobidService.isAutoBidActive(400, 4));

            // Disable
            autobidService.disableAutoBid(400, 4);
            assertFalse(autobidService.isAutoBidActive(400, 4), "Active status must be successfully deactivated.");

            // Expected to be called twice: once inside enableAutoBid and once explicitly here
            mockedDao.verify(() -> AutoBidDAO.deleteAutoBid(400, 4), Mockito.times(2));

            // Clear the entire auction
            autobidService.clearAuction(400);
            assertFalse(autobidService.isAutoBidActive(400, 4), "Queue must be completely empty after clearing.");
        }
    }

    // ===================================================================================
    // ADDITIONAL COVERAGE TESTS (Các test mở rộng để bao phủ toàn bộ code)
    // ===================================================================================

    /**
     * Kiểm tra chức năng xử lý hàng đợi (processQueue) khi đặt giá tự động thành công.
     * Hệ thống phải gửi thông báo cập nhật (notifyAll) tới các Client.
     */
    @Test
    void testProcessQueue_Success_NotifiesClients() {
        Bidder mockBidder = Mockito.mock(Bidder.class);
        Mockito.when(mockBidder.getId()).thenReturn(5);

        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getId()).thenReturn(500);
        Mockito.when(mockAuction.getCurrentPrice()).thenReturn(200.0);

        AutoBidConfig config = new AutoBidConfig(1000.0, 50.0); // Next bid will be 250

        BidService mockBidService = Mockito.mock(BidService.class);
        // Simulate a successful auto-bid
        Mockito.when(mockBidService.processAutoBid(mockBidder, mockAuction, 250.0)).thenReturn(true);

        AuctionManager mockAuctionManager = Mockito.mock(AuctionManager.class);
        ClientRegistry mockRegistry = Mockito.mock(ClientRegistry.class);

        try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class);
             MockedStatic<BidService> mockedBidServiceStatic = Mockito.mockStatic(BidService.class);
             MockedStatic<AuctionManager> mockedManagerStatic = Mockito.mockStatic(AuctionManager.class);
             MockedStatic<ClientRegistry> mockedRegistryStatic = Mockito.mockStatic(ClientRegistry.class)) {

            mockedManagerStatic.when(AuctionManager::getInstance).thenReturn(mockAuctionManager);
            Mockito.when(mockAuctionManager.getActive(500)).thenReturn(mockAuction);

            mockedBidServiceStatic.when(BidService::getInstance).thenReturn(mockBidService);
            mockedRegistryStatic.when(ClientRegistry::getInstance).thenReturn(mockRegistry);

            autobidService.enableAutoBid(mockBidder, mockAuction, config);

            // Execute processing queue
            autobidService.processQueue(500, "Another Guy");

            // Verify ClientRegistry was called to broadcast the update
            Mockito.verify(mockRegistry, Mockito.times(1)).notifyAll(Mockito.eq(500), Mockito.any());
        }
    }

    /**
     * Kiểm tra hàm xử lý hàng đợi (processQueue) khi xảy ra ngoại lệ.
     * Hệ thống phải bắt (catch) lỗi an toàn và không làm sập chương trình.
     */
    @Test
    void testProcessQueue_HandlesException() {
        Bidder mockBidder = Mockito.mock(Bidder.class);
        Mockito.when(mockBidder.getId()).thenReturn(6);

        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getId()).thenReturn(600);
        Mockito.when(mockAuction.getCurrentPrice()).thenReturn(300.0);

        AutoBidConfig config = new AutoBidConfig(1000.0, 50.0);

        BidService mockBidService = Mockito.mock(BidService.class);
        // Simulate a system crash during bidding
        Mockito.when(mockBidService.processAutoBid(Mockito.any(), Mockito.any(), Mockito.anyDouble()))
                .thenThrow(new RuntimeException("Database offline"));

        AuctionManager mockAuctionManager = Mockito.mock(AuctionManager.class);

        try (MockedStatic<AutoBidDAO> mockedDao = Mockito.mockStatic(AutoBidDAO.class);
             MockedStatic<BidService> mockedBidServiceStatic = Mockito.mockStatic(BidService.class);
             MockedStatic<AuctionManager> mockedManagerStatic = Mockito.mockStatic(AuctionManager.class)) {

            mockedManagerStatic.when(AuctionManager::getInstance).thenReturn(mockAuctionManager);
            Mockito.when(mockAuctionManager.getActive(600)).thenReturn(mockAuction);
            mockedBidServiceStatic.when(BidService::getInstance).thenReturn(mockBidService);

            autobidService.enableAutoBid(mockBidder, mockAuction, config);

            // This should safely catch the exception and not throw it outwards
            assertDoesNotThrow(() -> autobidService.processQueue(600, "Another Guy"),
                    "Exceptions within the queue processing must be caught and logged safely.");
        }
    }

    /**
     * Kiểm tra nhánh thoát sớm (early return) khi hàng đợi rỗng hoặc phiên bị null.
     */
    @Test
    void testProcessQueue_EmptyOrNull() {
        AuctionManager mockAuctionManager = Mockito.mock(AuctionManager.class);

        try (MockedStatic<AuctionManager> mockedManagerStatic = Mockito.mockStatic(AuctionManager.class)) {
            mockedManagerStatic.when(AuctionManager::getInstance).thenReturn(mockAuctionManager);
            Mockito.when(mockAuctionManager.getActive(999)).thenReturn(null);

            // Attempt to process a non-existent queue or a null auction
            assertDoesNotThrow(() -> autobidService.processQueue(999, "NoOne"),
                    "Processing an empty queue or null auction must safely return without errors.");
        }
    }

    /**
     * Kiểm tra tính năng cập nhật từ Observer (hàm update).
     * Hệ thống phải gửi một tác vụ (Runnable) vào ExecutorService mà không bị chặn (block).
     */
    @Test
    void testUpdate_TriggersExecutor() {
        // Calling update should dispatch a thread via executor and clear the processing flag
        assertDoesNotThrow(() -> autobidService.update(888, 100.0, "WinnerX"),
                "The update trigger from Observer must dispatch properly without exceptions.");
    }
}