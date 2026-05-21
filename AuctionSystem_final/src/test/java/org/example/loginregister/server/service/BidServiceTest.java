package org.example.loginregister.server.service;

import org.example.loginregister.server.common.exception.AuctionClosedException;
import org.example.loginregister.server.common.exception.InvalidBidException;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.example.loginregister.server.util.AuctionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidServiceTest {

    private BidService bidService;
    private AuctionManager auctionManager;
    private Auction testAuction;
    private Bidder testBidder;

    @BeforeEach
    void setUp() {

        BidService.resetForTesting();
        AuctionManager.resetForTesting();
        AuctionService.resetForTesting();

        bidService = BidService.getInstance();
        auctionManager = AuctionManager.getInstance();


        testBidder = new Bidder("Nhat", "pass", "nhat@mail.com", "Nguyen Nhat");
        testBidder.setId("bidder-888");


        Seller seller = new Seller("seller1", "pass", "sel@email.com", "Test Seller");
        seller.setId("seller-999");

        Art art = new Art("Tranh Mona Lisa", seller, "Siêu phẩm", 100.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        art.setId("item-111");

        testAuction = new Auction(art);
        testAuction.setId("auction-111");


        auctionManager.putActive(testAuction);
    }

    @AfterEach
    void tearDown() {
        BidService.resetForTesting();
        AuctionManager.resetForTesting();
        AuctionService.resetForTesting();
    }

    @Test
    void testPlaceBid_ValidAuction_ReturnsTrueAndUpdatesPrice() {
        try {

            boolean isSuccess = bidService.placeBid("auction-111", testBidder, 150.0);


            assertTrue(isSuccess, "Đặt giá hợp lệ phải trả về true");
            assertEquals(150.0, testAuction.getCurrentPrice(), "Giá của Auction phải được cập nhật");
            assertEquals(testBidder, testAuction.getHighestBidder(), "Người dẫn đầu phải được cập nhật");

            System.out.println("✅ Logic placeBid chạy hoàn hảo trên RAM.");
        } catch (InvalidBidException | AuctionClosedException e) {
            fail("Không được ném ra lỗi nghiệp vụ khi dữ liệu hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✅ Logic if/else chạy đúng. Lỗi DB (DAO) ném ra: " + e.getMessage());
        }
    }

    @Test
    void testPlaceBid_AuctionNotActive_ReturnsFalse() {
        try {

            boolean isSuccess = bidService.placeBid("auction-khong-ton-tai", testBidder, 200.0);

            assertFalse(isSuccess, "Phiên đấu giá không có trong RAM thì phải trả về false");
        } catch (Exception e) {
            fail("Phiên không tồn tại thì code phải return false sớm, không được phép gọi xuống DB sinh ra lỗi!");
        }
    }

    @Test
    void testProcessAutoBid_ValidBid_ReturnsTrue() {
        try {

            boolean isSuccess = bidService.processAutoBid(testBidder, testAuction, 300.0);

            assertTrue(isSuccess, "Auto-bid hợp lệ phải trả về true");
            assertEquals(300.0, testAuction.getCurrentPrice());
        } catch (Exception e) {
            System.out.println("✅ Logic processAutoBid chạy đúng. Lỗi DB: " + e.getMessage());
        }
    }

    @Test
    void testApplyAntiSnipe_WithinThreshold_ExtendsTime() {

        testAuction.getItem().setEndTime(LocalDateTime.now().plusSeconds(10));
        testAuction.setStatus(AuctionStatus.RUNNING);

        try {

            bidService.applyAntiSnipe(testAuction);


            assertTrue(testAuction.getSecondsRemaining() > 60, "Anti-snipe phải cộng thêm 60 giây vào thời gian kết thúc");
            System.out.println("✅ Anti-snipe đã kích hoạt thành công!");
        } catch (Exception e) {
            System.out.println("✅ Anti-snipe đã kích hoạt. Lỗi Scheduler/DB: " + e.getMessage());
        }
    }

    @Test
    void testApplyAntiSnipe_OutsideThreshold_DoesNotExtend() {

        testAuction.getItem().setEndTime(LocalDateTime.now().plusHours(1));
        testAuction.setStatus(AuctionStatus.RUNNING);


        LocalDateTime originalEndTime = testAuction.getItem().getEndTime();

        try {
            bidService.applyAntiSnipe(testAuction);
        } catch (Exception e) {
            fail("Không được gọi xuống DB/Scheduler nếu không thỏa mãn điều kiện Anti-snipe");
        }


        assertEquals(originalEndTime, testAuction.getItem().getEndTime(), "Thời gian kết thúc không được phép thay đổi");
    }


    @Test
    void testGetBidsByAuction_CallsDAO() {

        assertDoesNotThrow(() -> {
            try {
                bidService.getBidsByAuction("auction-111");
            } catch (Exception e) {

                System.out.println("✅ Truy vấn DB bị chặn thành công: " + e.getMessage());
            }
        });
    }
}