import me.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionManagerTest {

    private AuctionManager auctionManager;
    private AuctionHistoryManager historyManager;
    private Bidder bidder1;
    private Item dummyItem;

    @BeforeEach
    void setUp() {
        auctionManager = AuctionManager.getInstance();
        historyManager = AuctionHistoryManager.getInstance();

        // Dọn dẹp lịch sử cũ để không ảnh hưởng bài test mới
        historyManager.clearHistory();

        // Khởi tạo dữ liệu mẫu
        bidder1 = new Bidder("P01","Duong","123456789","duong@gmail.com","Tran Lam Duong");
        LocalDateTime now = LocalDateTime.now();
        dummyItem = new Electronics(
                "SP01", "Laptop Dell", "Dell đời mới", 1000.0, 1000.0, now, now.plusHours(1)
        );
    }

    @Test
    void testSingletonInstance() {
        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();
        assertSame(instance1, instance2, "getInstance() phải trả về cùng một đối tượng");
    }

    @Test
    void testStartAuction() {
        String auctionId = "TEST_START_01";

        // Mở phiên đấu giá 60 giây
        auctionManager.startAuction(auctionId, dummyItem, 60);

        Auction auction = auctionManager.getAuction(auctionId);
        assertNotNull(auction, "Phiên đấu giá phải được thêm vào danh sách active");
        assertEquals(Auction.RUNNING, auction.getStatus(), "Trạng thái phiên phải là RUNNING");
    }

    @Test
    void testStartDuplicateAuction() {
        String auctionId = "TEST_DUP_01";

        auctionManager.startAuction(auctionId, dummyItem, 60);

        // Cố tình mở thêm một phiên trùng ID
        // Không thể dùng assertThrows vì code của bạn chỉ in ra System.err và return
        auctionManager.startAuction(auctionId, dummyItem, 120);

        Auction auction = auctionManager.getAuction(auctionId);

        // Cấu hình của phiên đầu tiên phải được giữ nguyên (thời gian vẫn là 60 thay vì 120)
        //lấy thời gian còn lại, nếu nó <= 60 thì chứng tỏ phiên sau không đè lên phiên trước
        assertTrue(auction.getSecondsRemaining() <= 60, "Phiên mới không được ghi đè phiên đã tồn tại");
    }

    @Test
    void testPlaceBidSuccess() {
        String auctionId = "TEST_BID_01";
        auctionManager.startAuction(auctionId, dummyItem, 3600); // 1 giờ

        // Đặt giá hợp lệ (1500 > 1000)
        boolean isSuccess = auctionManager.placeBid(auctionId, bidder1, 1500.0);

        assertTrue(isSuccess, "Đặt giá hợp lệ phải trả về true");
        assertEquals(1500.0, auctionManager.getAuction(auctionId).getCurrentPrice(), "Giá phải được cập nhật");
        assertEquals(bidder1, auctionManager.getAuction(auctionId).getHighestBidder());
    }

    @Test
    void testPlaceBidAuctionNotFoundOrClosed() {
        // Cố tình đặt giá vào ID không tồn tại
        boolean isSuccess = auctionManager.placeBid("ID_KHONG_TON_TAI", bidder1, 2000.0);
        assertFalse(isSuccess, "Đặt giá vào phiên không tồn tại phải trả về false");
    }

    @Test
    void testAntiSnipingTriggers() {
        String auctionId = "TEST_SNIPE_01";

        // Mở phiên đấu giá chỉ với 10 giây (nhỏ hơn ngưỡng 30 giây của Anti-snipe)
        auctionManager.startAuction(auctionId, dummyItem, 10);

        Auction auction = auctionManager.getAuction(auctionId);
        long initialRemaining = auction.getSecondsRemaining();
        assertTrue(initialRemaining <= 10);

        // Đặt giá hợp lệ để kích hoạt Anti-snipe
        auctionManager.placeBid(auctionId, bidder1, 2000.0);

        // Kiểm tra thời gian sau khi đặt giá
        long newRemaining = auction.getSecondsRemaining();

        // Code cộng thêm 60s. Nên newRemaining phải loanh quanh mức 70s.
        assertTrue(newRemaining >= 60, "Thời gian phải được gia hạn thêm 60 giây do anti-sniping");
    }

    @Test
    void testAuctionAutoEndAndSaveHistory() throws InterruptedException {
        String auctionId = "TEST_AUTO_END_01";

        // Mở phiên đấu giá CỰC NGẮN: chỉ 1 giây
        auctionManager.startAuction(auctionId, dummyItem, 1);

        // Đặt giá để có người thắng cuộc

        Auction activeAuction = auctionManager.getAuction(auctionId);
        activeAuction.processBid( bidder1, 3000.0);

        // Chờ 2 giây để Scheduler có đủ thời gian kích hoạt hàm endAuction()
        Thread.sleep(2000);

        // 1. Kiểm tra xem phiên đã bị xóa khỏi danh sách Active chưa
        Auction finishedAuction = auctionManager.getAuction(auctionId);
        assertNull(finishedAuction, "Phiên đấu giá phải bị xóa khỏi activeAuctions sau khi kết thúc");

        // 2. Kiểm tra xem kết quả đã được lưu vào History Manager chưa
        AuctionResult savedResult = historyManager.getResult(auctionId);
        assertNotNull(savedResult, "Kết quả đấu giá phải được lưu vào HistoryManager");
        assertEquals(bidder1, savedResult.getWinner(), "Người thắng phải là bidder1");
        assertEquals(3000.0, savedResult.getFinalPrice(), "Giá chốt phải là 3000.0");
    }
    @AfterEach
    void tearDown() {
        // Dọn dẹp toàn bộ rác và tạo lại ThreadPool mới cho bài test tiếp theo
        auctionManager.resetForTesting();
    }

    @Test
    void testShutdown() {
        // Gọi thẳng lệnh tắt
        auctionManager.shutdown();

        // Vì scheduler là private và ta không có hàm getter cho nó,
        // ta có thể test gián tiếp bằng cách thử chạy một timer mới,
        // nếu ném lỗi RejectedExecutionException nghĩa là nó đã tắt thành công.
        assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> {
            auctionManager.startAuction("TEST_SHUTDOWN", dummyItem, 10);
        }, "Sau khi shutdown, không thể nhận thêm phiên đấu giá mới");
    }
}
