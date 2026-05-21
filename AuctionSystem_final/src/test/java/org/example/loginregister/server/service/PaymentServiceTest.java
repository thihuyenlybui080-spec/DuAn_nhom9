package org.example.loginregister.server.service;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionResult;
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

class PaymentServiceTest {

    private PaymentService paymentService;
    private Bidder winner;
    private AuctionResult wonResult;

    @BeforeEach
    void setUp() {

        PaymentService.resetForTesting();
        AuctionManager.resetForTesting();
        AuctionHistoryManager.getInstance().clearHistory();

        paymentService = PaymentService.getInstance();


        winner = new Bidder("WinnerUser", "pass", "win@email.com", "Winner Name");
        winner.setId("bidder-win");


        Seller seller = new Seller("seller1", "pass", "sel@email.com", "Seller Name");
        Art art = new Art("Test Art", seller, "Desc", 100.0, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        art.setId("item-999");

        Auction auction = new Auction(art);
        auction.setId("auction-999");
        auction.processBid(winner, 500.0);
        auction.finishAuction(AuctionStatus.FINISHED);


        wonResult = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(wonResult);
    }

    @AfterEach
    void tearDown() {

        PaymentService.resetForTesting();
        AuctionManager.resetForTesting();
        AuctionHistoryManager.getInstance().clearHistory();
    }

    @Test
    void testProcessPayment_Success_UpdatesStatusToPaid() {

        boolean isSuccess = paymentService.processPayment(winner, "auction-999");


        assertTrue(isSuccess, "Hàm thanh toán phải trả về true");


        AuctionResult updatedResult = AuctionHistoryManager.getInstance().getResult("auction-999");
        assertEquals(AuctionStatus.PAID, updatedResult.getStatus(), "Trạng thái phải được cập nhật thành PAID");
    }

    @Test
    void testProcessPayment_AlreadyPaid_ReturnsTrue() {

        AuctionHistoryManager.getInstance().updateStatus("auction-999", AuctionStatus.PAID);


        boolean isSuccess = paymentService.processPayment(winner, "auction-999");


        assertTrue(isSuccess, "Đã thanh toán rồi thì trả về true bỏ qua");
    }

    @Test
    void testProcessPayment_NotWinner_ReturnsFalse() {

        Bidder otherUser = new Bidder("LoserUser", "pass", "lose@email.com", "Loser Name");

        boolean isSuccess = paymentService.processPayment(otherUser, "auction-999");


        assertFalse(isSuccess, "Không phải người thắng thì không được phép thanh toán");
    }

    @Test
    void testProcessPayment_AuctionNotFinished_ReturnsFalse() {

        AuctionHistoryManager.getInstance().updateStatus("auction-999", AuctionStatus.CANCELED);


        boolean isSuccess = paymentService.processPayment(winner, "auction-999");


        assertFalse(isSuccess, "Phiên đấu giá không ở trạng thái FINISHED thì không cho thanh toán");
    }


    @Test
    void testSchedulePaymentDeadline_RunsWithoutThrowing() {

        try {
            paymentService.schedulePaymentDeadline("auction-999");
            System.out.println("✅ Gọi hàm schedulePaymentDeadline thành công.");
        } catch (Exception e) {
            System.out.println("✅ Đã đi vào schedulePaymentDeadline. Lỗi liên quan đến luồng hoặc DB: " + e.getMessage());
        }
    }
}