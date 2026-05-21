package org.example.loginregister.server.util;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionResult;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionHistoryManagerTest {

    private AuctionHistoryManager manager;
    private AuctionResult testResult1;
    private AuctionResult testResult2;
    private Bidder winner1;

    @BeforeEach
    void setUp() {
        manager = AuctionHistoryManager.getInstance();
        manager.clearHistory();
        Seller seller = new Seller("seller1", "pass", "sel@email.com", "Test Seller");
        Art art1 = new Art("Tranh 1", seller, "Mô tả 1", 100.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        art1.setId("item-111");

        Auction auction1 = new Auction(art1);
        auction1.setId("auction-111");

        winner1 = new Bidder("Winner1", "pass", "w1@email.com", "Winner One");
        auction1.processBid(winner1, 500.0);
        auction1.finishAuction(AuctionStatus.FINISHED);

        testResult1 = new AuctionResult(auction1);


        Art art2 = new Art("Tranh 2", seller, "Mô tả 2", 200.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        art2.setId("item-222");

        Auction auction2 = new Auction(art2);
        auction2.setId("auction-222");

        auction2.finishAuction(AuctionStatus.CANCELED);

        testResult2 = new AuctionResult(auction2);
    }

    @AfterEach
    void tearDown() {
        manager.clearHistory();
    }

    @Test
    void testGetInstance_ReturnsSingleton() {
        AuctionHistoryManager instance1 = AuctionHistoryManager.getInstance();
        AuctionHistoryManager instance2 = AuctionHistoryManager.getInstance();
        assertSame(instance1, instance2, "Hàm getInstance() phải luôn trả về cùng 1 object duy nhất");
    }


    @Test
    void testSaveAndGetResult_ValidResult_StoresCorrectly() {

        manager.saveResult(testResult1);


        AuctionResult retrieved = manager.getResult("auction-111");
        assertNotNull(retrieved);
        assertEquals(testResult1, retrieved, "Kết quả lấy ra phải khớp với kết quả đã lưu");
    }

    @Test
    void testSaveResult_NullValue_DoesNotThrow() {

        manager.saveResult(null);


        assertTrue(manager.getAllResults().isEmpty());
    }

    @Test
    void testGetAllResults_ReturnsAllSavedResults() {
        manager.saveResult(testResult1);
        manager.saveResult(testResult2);

        List<AuctionResult> allResults = manager.getAllResults();
        assertEquals(2, allResults.size(), "Phải chứa đủ 2 kết quả");
        assertTrue(allResults.contains(testResult1));
        assertTrue(allResults.contains(testResult2));
    }

    @Test
    void testGetResultsByWinner_ReturnsOnlyMatchingResults() {
        manager.saveResult(testResult1);
        manager.saveResult(testResult2);


        List<AuctionResult> wonList = manager.getResultsByWinner(winner1);

        assertEquals(1, wonList.size(), "Winner1 chỉ thắng 1 phiên");
        assertEquals("auction-111", wonList.get(0).getAuctionId());


        Bidder otherBidder = new Bidder("Other", "pass", "mail", "Other");
        List<AuctionResult> emptyList = manager.getResultsByWinner(otherBidder);

        assertTrue(emptyList.isEmpty(), "Người chưa thắng bao giờ thì danh sách phải rỗng");
    }

    @Test
    void testClearHistory_RemovesAllEntries() {
        manager.saveResult(testResult1);
        manager.saveResult(testResult2);

        manager.clearHistory();

        assertTrue(manager.getAllResults().isEmpty(), "Lịch sử phải hoàn toàn trống rỗng sau khi gọi clearHistory()");
    }

    @Test
    void testUpdateStatus_ChangesStatusSuccessfully() {
        manager.saveResult(testResult1);


        manager.updateStatus("auction-111", AuctionStatus.PAID);

        AuctionResult updatedResult = manager.getResult("auction-111");
        assertEquals(AuctionStatus.PAID, updatedResult.getStatus(), "Trạng thái phải được chuyển thành PAID");
    }
}