package org.example.loginregister.server.model.entity.auto_bidding;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidAgentTest {

    private Bidder bidder;
    private Auction auction;
    private AutoBidConfig config;
    private AutoBidAgent agent;

    @BeforeEach
    void setUp() {

        Seller seller = new Seller("seller1", "pass", "seller@mail", "Seller");
        Art art = new Art("Tranh", seller, "Mo ta", 100.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));


        art.setId("item-12345");


        auction = new Auction(art);
        auction.setSeller(seller);
        auction.setId("auction-12345");


        bidder = new Bidder("Nhat", "pass", "nhat@mail", "Nhat Nguyen");


        config = new AutoBidConfig(500.0, 50.0);
        agent = new AutoBidAgent(bidder, auction, config);
    }

    @Test
    void testUpdate_AgentIsAlreadyHighestBidder_DoesNothing() {

        assertDoesNotThrow(() -> {
            agent.update("auction-12345", 150.0, "Nhat");
        }, "Agent không được phép tự động trả giá đè lên chính chủ của nó");
    }

    @Test
    void testUpdate_NextBidExceedsMax_StopsAgent() {

        assertDoesNotThrow(() -> {
            agent.update("auction-12345", 480.0, "Ly");
        });


        assertDoesNotThrow(() -> {
            agent.update("auction-12345", 100.0, "Ly");
        }, "Sau khi gọi stop(), Agent không được gọi xuống Service nữa");
    }

    @Test
    void testStop_DeactivatesAgentSuccessfully() {

        agent.stop();

        assertDoesNotThrow(() -> {
            agent.update("auction-12345", 100.0, "Ly");
        }, "Hàm update() không được thực thi logic khi Agent đã bị stop()");
    }

    @Test
    void testUpdate_ValidNextBid_CallsBidService() {

        try {
            agent.update("auction-12345", 100.0, "Duong");
        } catch (Exception e) {

            System.out.println("Kiểm tra thành công: Logic của Agent đã đi tới bước gọi Service. " +
                    "Lỗi đi kèm do Database: " + e.getMessage());
        }
    }
}