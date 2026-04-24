import me.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionHistoryManagerTest {

    private AuctionHistoryManager manager;
    private Bidder winner1;
    private Bidder winner2;
    private AuctionResult result1;
    private AuctionResult result2;
    private AuctionResult result3;

    @BeforeEach
    void setUp() {
        manager = AuctionHistoryManager.getInstance();

        // BẮT BUỘC: Reset dữ liệu vì đây là Singleton
        manager.clearHistory();

        // 1. Khởi tạo người thắng
        winner1 = new Bidder("P01","Duong","123456789","duong@gmail.com","Tran Lam Duong");
        winner2 = new Bidder("P02","Duong2","1234567890","duong@gmail.com","Tran Lam Duong2");

        // 2. Khởi tạo Item mẫu (Dùng Electronics để tránh NullPointerException khi gọi toString)
        LocalDateTime now = LocalDateTime.now();
        Item dummyItem = new Electronics(
                "SP01", "Laptop Dell", "Dell đời mới", 1000.0, 1000.0, now, now.plusHours(1)
        );

        // 3. Khởi tạo danh sách Bid giả định
        List<Bid> dummyBidHistory = new ArrayList<>();
        dummyBidHistory.add(new Bid(winner1, 1200.0));
        dummyBidHistory.add(new Bid(winner1, 1500.0));

        // 4. Khởi tạo các AuctionResult theo constructor mới
        result1 = new AuctionResult("A01", dummyItem, winner1, 1500.0, dummyBidHistory);
        result2 = new AuctionResult("A02", dummyItem, winner2, 2500.0, dummyBidHistory);
        result3 = new AuctionResult("A03", dummyItem, winner1, 3000.0, dummyBidHistory);
    }

    @Test
    void testSingletonInstance() {
        AuctionHistoryManager instance1 = AuctionHistoryManager.getInstance();
        AuctionHistoryManager instance2 = AuctionHistoryManager.getInstance();

        assertSame(instance1, instance2, "getInstance() phải luôn trả về cùng một đối tượng Singleton");
    }

    @Test
    void testSaveAndGetResult() {
        manager.saveResult(result1);

        AuctionResult retrievedResult = manager.getResult("A01");
        assertNotNull(retrievedResult, "Không được null khi tìm ID đã lưu");
        assertEquals("A01", retrievedResult.getAuctionId());
        assertEquals(winner1, retrievedResult.getWinner());
        assertEquals(1500.0, retrievedResult.getFinalPrice());

        // Kiểm tra xem bidHistory có được copy và lưu đúng không
        assertEquals(2, retrievedResult.getBidHistory().size(), "Lịch sử bid phải chứa 2 phần tử");
    }

    @Test
    void testGetResultNotFound() {
        AuctionResult retrievedResult = manager.getResult("ID_KHONG_TON_TAI");
        assertNull(retrievedResult, "Phải trả về null nếu không tìm thấy ID phiên đấu giá");
    }

    @Test
    void testGetAllResults() {
        manager.saveResult(result1);
        manager.saveResult(result2);

        List<AuctionResult> allResults = manager.getAllResults();

        assertEquals(2, allResults.size(), "Danh sách trả về phải có đúng 2 phần tử đã lưu");
        assertTrue(allResults.contains(result1));
        assertTrue(allResults.contains(result2));
    }

    @Test
    void testGetResultsByWinner() {
        manager.saveResult(result1); // Của winner1
        manager.saveResult(result2); // Của winner2
        manager.saveResult(result3); // Của winner1

        List<AuctionResult> resultsForWinner1 = manager.getResultsByWinner(winner1);

        assertEquals(2, resultsForWinner1.size(), "Winner1 phải có 2 phiên đấu giá thắng cuộc");
        assertTrue(resultsForWinner1.contains(result1));
        assertTrue(resultsForWinner1.contains(result3));

        List<AuctionResult> resultsForWinner2 = manager.getResultsByWinner(winner2);

        assertEquals(1, resultsForWinner2.size(), "Winner2 phải có 1 phiên đấu giá thắng cuộc");
        assertTrue(resultsForWinner2.contains(result2));
    }

    @Test
    void testClearHistory() {
        manager.saveResult(result1);
        manager.saveResult(result2);

        // Đảm bảo dữ liệu đã được nạp
        assertEquals(2, manager.getAllResults().size());

        // Thực thi lệnh xóa
        manager.clearHistory();

        // Kiểm tra sau khi xóa
        assertEquals(0, manager.getAllResults().size(), "Danh sách phải rỗng hoàn toàn sau khi clear");
        assertNull(manager.getResult("A01"));
    }
}