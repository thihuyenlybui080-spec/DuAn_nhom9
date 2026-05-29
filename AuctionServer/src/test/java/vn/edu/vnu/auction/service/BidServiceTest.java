package vn.edu.vnu.auction.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import vn.edu.vnu.auction.common.exception.AuctionClosedException;
import vn.edu.vnu.auction.common.exception.InvalidBidException;
import vn.edu.vnu.auction.dao.BidDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.util.AuctionManager;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử (Unit Test) dành cho {@link BidService}.
 * <p>
 * Đảm bảo các chức năng đặt giá, chống bắn tỉa (anti-snipe) và cơ chế hoàn tác (rollback)
 * khi lỗi cơ sở dữ liệu hoạt động chính xác.
 * </p>
 */
class BidServiceTest {

    private BidService bidService;
    private AuctionManager mockAuctionManager;
    private AuctionService mockAuctionService;

    /**
     * Dọn dẹp trạng thái và giả lập các Manager/Service phụ thuộc bằng Reflection trước mỗi bài test.
     */
    @BeforeEach
    void setUp() throws Exception {
        BidService.resetForTesting();
        AuctionManager.resetForTesting();
        AuctionService.resetForTesting();

        bidService = BidService.getInstance();

        // Giả lập các dependencies
        mockAuctionManager = Mockito.mock(AuctionManager.class);
        mockAuctionService = Mockito.mock(AuctionService.class);

        // Tiêm AuctionManager giả vào BidService
        Field managerField = BidService.class.getDeclaredField("auctionManager");
        managerField.setAccessible(true);
        managerField.set(bidService, mockAuctionManager);

        // Tiêm AuctionService giả vào BidService
        Field serviceField = BidService.class.getDeclaredField("auctionService");
        serviceField.setAccessible(true);
        serviceField.set(bidService, mockAuctionService);
    }

    /**
     * Dọn dẹp lại hệ thống sau khi test xong.
     */
    @AfterEach
    void tearDown() {
        BidService.resetForTesting();
        AuctionManager.resetForTesting();
        AuctionService.resetForTesting();
    }

    /**
     * Kiểm tra cơ chế Singleton của lớp BidService.
     */
    @Test
    void testGetInstance() {
        BidService instance1 = BidService.getInstance();
        BidService instance2 = BidService.getInstance();

        assertNotNull(instance1, "Instance không được để null.");
        assertSame(instance1, instance2, "Chỉ được phép tồn tại một đối tượng BidService duy nhất.");
    }

    /**
     * Kiểm tra chức năng đặt giá sẽ thất bại nếu phiên đấu giá không tồn tại trong bộ nhớ.
     */
    @Test
    void testPlaceBid_AuctionNotActive() throws Exception {
        Mockito.when(mockAuctionManager.getActive(99)).thenReturn(null);

        Bidder mockBidder = Mockito.mock(Bidder.class);
        boolean result = bidService.placeBid(99, mockBidder, 100.0);

        assertFalse(result, "Hàm phải trả về false nếu không tìm thấy phiên đấu giá.");
    }

    /**
     * Kiểm tra chức năng đặt giá sẽ ném ngoại lệ nếu phiên đấu giá chưa bắt đầu (không phải trạng thái RUNNING).
     */
    @Test
    void testPlaceBid_AuctionNotRunning() {
        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getStatus()).thenReturn(AuctionStatus.OPEN); // Trạng thái chưa chạy
        Mockito.when(mockAuctionManager.getActive(1)).thenReturn(mockAuction);

        Bidder mockBidder = Mockito.mock(Bidder.class);

        assertThrows(InvalidBidException.class, () -> bidService.placeBid(1, mockBidder, 500.0),
                "Phải ném ra InvalidBidException nếu phiên đấu giá chưa bắt đầu.");
    }

    /**
     * Kiểm tra kịch bản đặt giá thành công.
     */
    @Test
    void testPlaceBid_Success() throws Exception {
        int auctionId = 10;
        int bidderId = 5;
        double bidAmount = 1000.0;

        Item mockItem = Mockito.mock(Item.class);
        Bidder mockBidder = Mockito.mock(Bidder.class);
        Mockito.when(mockBidder.getId()).thenReturn(bidderId);

        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getId()).thenReturn(auctionId);
        Mockito.when(mockAuction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        Mockito.when(mockAuction.getItem()).thenReturn(mockItem);
        Mockito.when(mockAuction.getBids()).thenReturn(new ArrayList<>());

        Mockito.when(mockAuctionManager.getActive(auctionId)).thenReturn(mockAuction);

        try (MockedStatic<BidDAO> mockedDao = Mockito.mockStatic(BidDAO.class)) {
            // Giả lập lưu DB thành công
            mockedDao.when(() -> BidDAO.insertBid(auctionId, bidderId, bidAmount)).thenReturn(true);

            boolean result = bidService.placeBid(auctionId, mockBidder, bidAmount);

            assertTrue(result, "Hàm phải trả về true khi đặt giá thành công.");
            // Xác minh đối tượng auction được gọi hàm lưu giá mới
            Mockito.verify(mockAuction, Mockito.times(1)).placeBid(Mockito.any(), Mockito.eq(false));
            // Xác minh hệ thống đã gửi thông báo (notify) đến các người xem khác
            Mockito.verify(mockAuction, Mockito.times(1)).notifyObservers();
        }
    }

    /**
     * Kiểm tra cơ chế hoàn tác (rollback) khi người dùng đặt giá nhưng hệ thống Database bị lỗi không lưu được.
     */
    @Test
    void testPlaceBid_DatabaseFailure_Rollback() {
        int auctionId = 20;
        int bidderId = 7;
        double bidAmount = 1500.0;

        Item mockItem = Mockito.mock(Item.class);
        Bidder mockBidder = Mockito.mock(Bidder.class);
        Mockito.when(mockBidder.getId()).thenReturn(bidderId);

        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getId()).thenReturn(auctionId);
        Mockito.when(mockAuction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        Mockito.when(mockAuction.getItem()).thenReturn(mockItem);
        Mockito.when(mockAuction.getCurrentPrice()).thenReturn(500.0); // Giá trước khi bid

        // Cần một list thực sự để test việc size thay đổi khi rollback
        List<vn.edu.vnu.auction.model.entity.BidTransaction> mockBids = new ArrayList<>();
        Mockito.when(mockAuction.getBids()).thenReturn(mockBids);

        Mockito.when(mockAuctionManager.getActive(auctionId)).thenReturn(mockAuction);

        try (MockedStatic<BidDAO> mockedDao = Mockito.mockStatic(BidDAO.class)) {
            // Giả lập lưu DB thất bại
            mockedDao.when(() -> BidDAO.insertBid(auctionId, bidderId, bidAmount)).thenReturn(false);

            assertThrows(InvalidBidException.class, () -> bidService.placeBid(auctionId, mockBidder, bidAmount),
                    "Phải ném ra ngoại lệ InvalidBidException do lỗi DB.");

            // Xác minh dữ liệu giá được khôi phục về trạng thái cũ
            Mockito.verify(mockAuction, Mockito.times(1)).setCurrentPrice(500.0);
        }
    }

    /**
     * Kiểm tra tính năng đặt giá tự động (processAutoBid) xử lý lỗi gọn gàng khi có ngoại lệ xảy ra.
     */
    @Test
    void testProcessAutoBid_HandlesExceptions() {
        Auction mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getId()).thenReturn(30);
        Mockito.when(mockAuction.getStatus()).thenReturn(AuctionStatus.OPEN); // Sẽ gây lỗi InvalidBidException

        Mockito.when(mockAuctionManager.getActive(30)).thenReturn(mockAuction);

        Bidder mockBidder = Mockito.mock(Bidder.class);

        // processAutoBid không được ném văng lỗi ra ngoài mà phải catch lại và trả về false
        boolean result = bidService.processAutoBid(mockBidder, mockAuction, 2000.0);

        assertFalse(result, "Hàm phải bắt lỗi và trả về false nếu đặt giá tự động thất bại.");
    }

    /**
     * Kiểm tra tính năng chống bắn tỉa (Anti-Snipe) có gia hạn thêm thời gian hay không.
     */
    @Test
    void testApplyAntiSnipe_Extended() {
        Auction mockAuction = Mockito.mock(Auction.class);
        // Giả lập thỏa mãn điều kiện chống bắn tỉa
        Mockito.when(mockAuction.tryExtendForAntiSnipe(Mockito.anyLong(), Mockito.anyLong())).thenReturn(true);
        Mockito.when(mockAuction.getSecondsRemaining()).thenReturn(100L);

        bidService.applyAntiSnipe(mockAuction);

        // Xác minh AuctionService được gọi để thiết lập lịch kết thúc mới
        Mockito.verify(mockAuctionService, Mockito.times(1)).scheduleEnd(mockAuction, 100L);
    }

    /**
     * Kiểm tra chức năng lấy lịch sử đặt giá của một phiên từ Database.
     */
    @Test
    void testGetBidsByAuction() {
        int auctionId = 55;
        List<vn.edu.vnu.auction.model.entity.BidTransaction> expectedList = new ArrayList<>();

        try (MockedStatic<BidDAO> mockedDao = Mockito.mockStatic(BidDAO.class)) {
            mockedDao.when(() -> BidDAO.getBidsByAuction(auctionId)).thenReturn(expectedList);

            List<vn.edu.vnu.auction.model.entity.BidTransaction> actualList = bidService.getBidsByAuction(auctionId);

            assertSame(expectedList, actualList, "Danh sách trả về phải lấy từ DAO.");
        }
    }
}