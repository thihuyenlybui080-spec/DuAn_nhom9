package org.example.loginregister.server.model.entity;

import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionResultTest {

    private Auction auction;
    private Art testArt;
    private Seller testSeller;

    @BeforeEach
    void setUp() {

        testSeller = new Seller("seller1", "pass", "seller@mail", "Test Seller");
        testArt = new Art("Tranh Mona Lisa", testSeller, "Siêu phẩm", 100.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        testArt.setId("item-12345"); // Đảm bảo không bị lỗi substring


        auction = new Auction(testArt);
        auction.setSeller(testSeller);
        auction.setId("auction-12345");
    }


    @Test
    void testConstructor_WithWinner_MapsDataCorrectly() {

        Bidder winner = new Bidder("Nhat", "pass", "nhat@mail", "Nhat Nguyen");
        auction.processBid(winner, 500.0);
        auction.finishAuction(AuctionStatus.FINISHED);


        AuctionResult result = new AuctionResult(auction);


        assertEquals("auction-12345", result.getAuctionId());
        assertEquals(testArt, result.getItem());
        assertEquals(winner, result.getWinner());
        assertEquals(500.0, result.getFinalPrice());
        assertEquals(AuctionStatus.FINISHED, result.getStatus());


        assertNotNull(result.getBidHistory());
        assertEquals(1, result.getBidHistory().size(), "Phải copy được 1 giao dịch bid sang lịch sử");


        assertNotNull(result.getEndTime());
    }


    @Test
    void testConstructor_NoWinner_MapsDataCorrectly() {

        auction.finishAuction(AuctionStatus.CANCELED);


        AuctionResult result = new AuctionResult(auction);


        assertNull(result.getWinner(), "Không có ai mua thì winner phải là null");
        assertEquals(100.0, result.getFinalPrice(), "Giá cuối cùng phải giữ nguyên giá khởi điểm");
        assertEquals(AuctionStatus.CANCELED, result.getStatus());
        assertTrue(result.getBidHistory().isEmpty(), "Lịch sử bid phải trống");
    }

    @Test
    void testSetStatus_UpdatesStatusCorrectly() {

        Bidder winner = new Bidder("Ly", "pass", "ly@mail", "Ly Tran");
        auction.processBid(winner, 200.0);
        auction.finishAuction(AuctionStatus.FINISHED);

        AuctionResult result = new AuctionResult(auction);


        result.setStatus(AuctionStatus.PAID);


        assertEquals(AuctionStatus.PAID, result.getStatus(), "Trạng thái phải được cập nhật thành PAID");
    }

    //
    @Test
    void testToString_WithWinner_FormatsCorrectly() {
        Bidder winner = new Bidder("Thuy", "pass", "thuy@mail", "Thuy");
        auction.processBid(winner, 1200.0);
        auction.finishAuction(AuctionStatus.FINISHED);

        AuctionResult result = new AuctionResult(auction);

        String output = result.toString();

        assertTrue(output.contains("Session auction-12345"));
        assertTrue(output.contains("Item: Tranh Mona Lisa"));
        assertTrue(output.contains("Winner: Thuy"));
        assertTrue(output.contains("Final Price: 1200"));
    }

    @Test
    void testToString_NoWinner_FormatsCorrectly() {
        auction.finishAuction(AuctionStatus.CANCELED);

        AuctionResult result = new AuctionResult(auction);

        String output = result.toString();

        assertTrue(output.contains("Winner: None"));
    }
}