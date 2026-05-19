package org.example.loginregister.server.model.entity;

import org.example.loginregister.server.model.entity.item.Art;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidTransactionTest {

    private Bidder testBidder;
    private Art testItem;

    @BeforeEach
    void setUp() {

        Seller seller = new Seller("seller1", "pass", "seller@mail.com", "Test Seller");

        testItem = new Art("Bức tranh Mona Lisa", seller, "Mô tả", 100.0, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        testBidder = new Bidder("Nhat", "pass", "nhat@mail.com", "Nguyen Nhat");
    }

    @Test
    void testConstructorAndGetters_SetsValuesCorrectly() {

        BidTransaction transaction = new BidTransaction(testBidder, testItem, 550.0);


        assertEquals(testBidder, transaction.getBidder(), "Bidder phải khớp với đối tượng truyền vào");
        assertEquals(testItem, transaction.getItem(), "Item phải khớp với đối tượng truyền vào");
        assertEquals(550.0, transaction.getAmount(), "Số tiền (Amount) phải khớp với giá trị truyền vào");


        assertNotNull(transaction.getTimestamp(), "Timestamp tự động sinh ra không được null");


        assertTrue(transaction.getTimestamp().compareTo(LocalDateTime.now()) <= 0,
                "Timestamp không thể nằm ở thì tương lai");
    }


    @Test
    void testSetTimestamp_UpdatesTimestampSuccessfully() {
        BidTransaction transaction = new BidTransaction(testBidder, testItem, 550.0);


        LocalDateTime pastTime = LocalDateTime.now().minusDays(5);


        transaction.setTimestamp(pastTime);


        assertEquals(pastTime, transaction.getTimestamp(), "Timestamp phải được cập nhật đúng thông qua hàm setTimestamp()");
    }
}