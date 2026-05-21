package org.example.loginregister.server.model.entity;

import org.example.loginregister.server.common.observer.Observer;
import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Art testArt;
    private Auction auction;

    @BeforeEach
    void setUp() {

        Seller testSeller = new Seller("seller1", "pass123", "seller@email.com", "Nguyen Seller");
        LocalDateTime now = LocalDateTime.now();


        testArt = new Art("Bức tranh Test", testSeller, "Mô tả", 100.0, now, now.plusMinutes(10));


        testArt.setId("item-123456789");


        auction = new Auction(testArt);


        auction.setSeller(testSeller);
    }


    @Test
    void testAuctionInitialization() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus(), "Trạng thái ban đầu phải là OPEN");
        assertEquals(100.0, auction.getCurrentPrice(), "Giá hiện tại phải bằng giá khởi điểm của Item");
        assertNull(auction.getHighestBidder(), "Chưa có ai đấu giá thì highestBidder phải là null");
        assertEquals("auction-123456789", auction.getId(), "ID sinh ra phải lấy từ Item cắt bỏ chữ 'item-'");
        assertTrue(auction.getBids().isEmpty(), "Danh sách lịch sử bid ban đầu phải rỗng");
    }


    @Test
    void testProcessBid_ValidBid_UpdatesPriceAndStatus() {
        Bidder b1 = new Bidder("Nhat", "pass", "nhat@mail", "Nguyen Nhat");

        boolean isSuccess = auction.processBid(b1, 150.0);

        assertTrue(isSuccess, "Đặt giá hợp lệ phải thành công (true)");
        assertEquals(150.0, auction.getCurrentPrice(), "Giá phải được cập nhật");
        assertEquals(b1, auction.getHighestBidder(), "Người dẫn đầu phải được cập nhật");
        assertEquals(AuctionStatus.RUNNING, auction.getStatus(), "Trạng thái phải chuyển thành RUNNING");
        assertEquals(1, auction.getBids().size());
    }

    @Test
    void testProcessBid_InvalidBidLowerThanCurrent_Fails() {
        Bidder b1 = new Bidder("Nhat", "pass", "nhat@mail", "Nguyen Nhat");
        Bidder b2 = new Bidder("Ly", "pass", "ly@mail", "Tran Ly");

        auction.processBid(b1, 200.0); // Nhat trả 200

        boolean isSuccess = auction.processBid(b2, 150.0);

        assertFalse(isSuccess, "Đặt giá thấp hơn giá hiện tại phải bị từ chối");
        assertEquals(200.0, auction.getCurrentPrice(), "Giá không được thay đổi");
        assertEquals(b1, auction.getHighestBidder(), "Người dẫn đầu vẫn phải là người cũ");
    }

    @Test
    void testProcessBid_OnClosedAuction_Fails() {
        Bidder b1 = new Bidder("Nhat", "pass", "mail", "Nhat");


        auction.finishAuction(AuctionStatus.FINISHED);

        boolean isSuccess = auction.processBid(b1, 500.0);

        assertFalse(isSuccess, "Không được phép bid khi phiên đã kết thúc");
        assertTrue(auction.getBids().isEmpty());
    }


    @Test
    void testCancelBidsFrom_HighestBidderRemoved_RevertsToSecondHighest() {
        Bidder b1 = new Bidder("Thuy", "pass", "mail1", "Thuy");
        Bidder b2 = new Bidder("Duong", "pass", "mail2", "Duong");

        auction.processBid(b1, 150.0); // Thủy đặt 150
        auction.processBid(b2, 300.0); // Dương đặt 300 (Đang dẫn đầu)


        auction.cancelBidsFrom(b2);


        assertEquals(b1, auction.getHighestBidder(), "Người dẫn đầu phải lùi về Thủy");
        assertEquals(150.0, auction.getCurrentPrice(), "Giá phải lùi về 150.0");
        assertEquals(1, auction.getBids().size(), "Chỉ còn lại 1 lịch sử của Thủy");
    }

    @Test
    void testCancelBidsFrom_OnlyBidderRemoved_ResetsToStartingPrice() {
        Bidder b1 = new Bidder("Thuy", "pass", "mail", "Thuy");
        auction.processBid(b1, 250.0);


        auction.cancelBidsFrom(b1);

        assertNull(auction.getHighestBidder(), "Không còn ai bid");
        assertEquals(100.0, auction.getCurrentPrice(), "Giá phải quay về giá gốc (100.0)");
    }

    @Test
    void testObserverIsNotifiedOnNewBid() {

        class TestObserver implements Observer {
            boolean isNotified = false;
            double notifiedPrice = 0;
            String notifiedBidder = "";

            @Override
            public void update(String subjectId, double currentPrice, String bidderName) {
                this.isNotified = true;
                this.notifiedPrice = currentPrice;
                this.notifiedBidder = bidderName;
            }
        }

        TestObserver observer = new TestObserver();
        auction.addObserver(observer);


        Bidder b1 = new Bidder("Nhat", "pass", "mail", "Nhat");
        auction.processBid(b1, 500.0);


        assertTrue(observer.isNotified, "Observer phai nhan duoc thong bao");
        assertEquals(500.0, observer.notifiedPrice, "Gia gui cho observer phai chinh xac");
        assertEquals("Nhat", observer.notifiedBidder, "Ten gui cho observer phai chinh xac");
    }}