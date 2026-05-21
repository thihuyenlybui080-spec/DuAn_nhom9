package org.example.loginregister.server.model.entity;

import org.example.loginregister.server.common.exception.AuctionClosedException;
import org.example.loginregister.server.common.exception.InvalidBidException;
import org.example.loginregister.server.common.observer.Observer;
import org.example.loginregister.server.model.entity.item.Electronics;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Auction auction;
    private Item item;
    private Seller seller1;
    private Bidder bidder1;
    private Bidder bidder2;

    @BeforeEach
    void setUp() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);

        seller1 = new Seller("Seller1", "987654321","sell@gmail.com","Nguye Van Sell");
        item = new Electronics("Laptop Dell", seller1.getId(), item.getDescription(), 1000, start, end);
        seller1.addItem(item);
        // Khởi tạo phiên đấu giá kéo dài 3600 giây (1 giờ)
        auction = new Auction(item);

        bidder1 = new Bidder("Duong", "123456789", "duong@gmail.com","Tran Lam Duong");
        bidder2 = new Bidder("Duong2", "1234567890", "duong2@gmail.com","Tran Lam Duong2");
    }

    @Test
    void testInitialAuctionState() {
        // Kiểm tra trạng thái ban đầu khi mới tạo phiên đấu giá
        assertEquals(AuctionStatus.OPEN, auction.getStatus(), "Trạng thái ban đầu phải là OPEN");
        assertEquals(1000.0, auction.getCurrentPrice(), "Giá hiện tại phải bằng giá khởi điểm");
        assertNull(auction.getHighestBidder(), "Chưa có người đấu giá thì highestBidder phải là null");
        assertTrue(auction.getSecondsRemaining() > 0, "Thời gian còn lại phải lớn hơn 0");
    }

    @Test
    void testPlaceValidBidSuccess() {
        // Kiểm tra đặt giá hợp lệ (lớn hơn giá hiện tại)
        assertDoesNotThrow(() -> {
            auction.placeBid(new BidTransaction(bidder1, item,1500.0));
        });

        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        assertEquals(1500.0, auction.getCurrentPrice());
        assertEquals(bidder1, auction.getHighestBidder());
        assertEquals(1, auction.getBids().size());
    }

    @Test
    void testPlaceInvalidBid_ThrowsException() {
        // Kiểm tra ném ngoại lệ khi đặt giá thấp hơn hoặc bằng giá hiện tại
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(new BidTransaction(bidder1, item,500.0)); // 500 < 1000 (giá khởi điểm)
        });

        // Trạng thái và giá không được thay đổi
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(1000.0, auction.getCurrentPrice());
    }

    @Test
    void testPlaceBidWhenAuctionClosed_ThrowsException() {
        // Kết thúc phiên đấu giá
        auction.finishAuction(AuctionStatus.FINISHED);

        // Cố tình đặt giá khi phiên đã đóng
        assertThrows(AuctionClosedException.class, () -> {
            auction.placeBid(new BidTransaction(bidder1, item, 20000.0));
        });
    }

    @Test
    void testProcessBidWrapperMethod() {
        // Hàm processBid bắt try/catch và trả về boolean, tiện cho UI
        boolean resultSuccess = auction.processBid(bidder1, 1200.0);
        assertTrue(resultSuccess, "Đặt giá hợp lệ phải trả về true");

        boolean resultFail = auction.processBid(bidder2, 1000.0); // Bằng giá hiện tại
        assertFalse(resultFail, "Đặt giá không hợp lệ phải trả về false");
    }

    @Test
    void testFinishAuction() {
        // Đặt giá trước khi đóng
        auction.processBid(bidder1, 2000.0);

        auction.finishAuction(AuctionStatus.RUNNING);

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals(bidder1, auction.getHighestBidder(), "Vẫn phải giữ nguyên người thắng cuộc");//???????
    }

    @Test
    void testExtendEndTime() {
        long initialTime = auction.getSecondsRemaining();

        // Gia hạn thêm 60 giây
        auction.extendEndTime(60);
        long extendedTime = auction.getSecondsRemaining();

        // Thời gian mới phải lớn hơn hoặc bằng thời gian cũ + 60s
        assertEquals(initialTime + 60, extendedTime );
    }

    @Test
    void testObserverPattern() {
        // Tạo một lớp Observer nặc danh (Anonymous class) để test
        var testObserver = new Observer() {
            public boolean isNotified = false;

            @Override
            public void update(String auctionId, double currentPrice, String highestBidderName) {
                isNotified = true;
                assertEquals("A01", auctionId);
                assertEquals(1000000, currentPrice);
                assertEquals("Duong", highestBidderName);
            }
        };

        auction.addObserver(testObserver);

        // Khi gọi placeBid, hệ thống phải tự động gọi hàm update() của testObserver
        auction.processBid(bidder1, 1000000);

        assertTrue(testObserver.isNotified, "Observer phải được thông báo (notify) khi có bid mới");
    }
}