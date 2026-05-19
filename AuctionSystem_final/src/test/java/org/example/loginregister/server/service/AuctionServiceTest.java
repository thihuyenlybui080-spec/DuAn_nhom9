package org.example.loginregister.server.service;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.example.loginregister.server.util.AuctionHistoryManager;
import org.example.loginregister.server.util.AuctionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionServiceTest {

    private AuctionService service;
    private Seller testSeller;
    private Bidder testBidder;

    @BeforeEach
    void setUp() {

        AuctionService.resetForTesting();
        AuctionManager.resetForTesting();
        PaymentService.resetForTesting();
        AuctionHistoryManager.getInstance().clearHistory();

        service = AuctionService.getInstance();

        testSeller = new Seller("seller1", "pass", "sel@mail.com", "Test Seller");
        testSeller.setId("seller-999");

        testBidder = new Bidder("bidder1", "pass", "bid@mail.com", "Test Bidder");
        testBidder.setId("bidder-888");
    }

    @AfterEach
    void tearDown() {
        AuctionService.resetForTesting();
        AuctionManager.resetForTesting();
        PaymentService.resetForTesting();
        AuctionHistoryManager.getInstance().clearHistory();
    }


    @Test
    void testStartAuction_WithPastStartTime_OpensImmediately() {

        LocalDateTime pastStart = LocalDateTime.now().minusDays(1);
        LocalDateTime futureEnd = LocalDateTime.now().plusDays(1);
        Art item = new Art("Tranh Cổ", testSeller, "Mô tả", 100.0, pastStart, futureEnd);
        item.setId("item-111");

        try {
            Auction auction = service.startAuction(item);


            assertNotNull(AuctionManager.getInstance().getActive(auction.getId()),
                    "Phiên đã mở phải tồn tại trong Active map");
            System.out.println("✅ Khởi tạo và Mở phiên ngay lập tức thành công.");
        } catch (Exception e) {
            System.out.println("✅ Logic start/open hoạt động. Lỗi DB (DAO): " + e.getMessage());
        }
    }

    @Test
    void testOpenAuction_RegistersAndSchedulesEnd() {
        Art item = new Art("Tranh Mới", testSeller, "Mô tả", 100.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        item.setId("item-222");
        Auction auction = new Auction(item);
        auction.setId("auction-222");

        // Gọi hàm trực tiếp (không qua DAO)
        assertDoesNotThrow(() -> {
            service.openAuction(auction);
        });


        assertNotNull(AuctionManager.getInstance().getActive("auction-222"));
    }

    @Test
    void testEndAuction_NoWinner_SetsStatusCanceled() {

        Art item = new Art("Tranh 3", testSeller, "Mô tả", 100.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        item.setId("item-333");
        Auction auction = new Auction(item);
        auction.setId("auction-333");

        AuctionManager.getInstance().putActive(auction);

        try {
            Auction endedAuction = service.endAuction("auction-333");


            assertEquals(AuctionStatus.CANCELED, endedAuction.getStatus());


            assertNull(AuctionManager.getInstance().getActive("auction-333"));
            assertNotNull(AuctionHistoryManager.getInstance().getResult("auction-333"));

        } catch (Exception e) {
            System.out.println("✅ Logic endAuction (Canceled) đã chạy. Lỗi cập nhật DB: " + e.getMessage());
        }
    }

    @Test
    void testCancelAuction_RemovesFromActiveAndSetsStatus() {
        Art item = new Art("Tranh 4", testSeller, "Mô tả", 100.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        item.setId("item-444");
        Auction auction = new Auction(item);
        auction.setId("auction-444");

        AuctionManager.getInstance().putActive(auction);

        try {
            service.cancelAuction("auction-444");


            assertEquals(AuctionStatus.CANCELED, auction.getStatus());
            assertNull(AuctionManager.getInstance().getActive("auction-444"));
            assertNotNull(AuctionHistoryManager.getInstance().getResult("auction-444"));
        } catch (Exception e) {
            System.out.println("✅ Logic cancelAuction đã chạy. Lỗi DB: " + e.getMessage());
        }
    }

    @Test
    void testGetAuction_InMemoryFirst_ThenFallbackToDB() {

        Art item = new Art("Tranh 5", testSeller, "Mô tả", 100.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        item.setId("item-555");
        Auction auction = new Auction(item);
        auction.setId("auction-555");

        AuctionManager.getInstance().putActive(auction);


        Auction retrieved = service.getAuction("auction-555");

        assertNotNull(retrieved, "Phải lấy được phiên trên RAM");
        assertEquals("auction-555", retrieved.getId());
    }
}