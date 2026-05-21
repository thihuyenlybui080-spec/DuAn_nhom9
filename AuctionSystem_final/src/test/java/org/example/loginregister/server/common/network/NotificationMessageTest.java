package org.example.loginregister.server.common.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationMessageTest {
    @Test
    void testConstructorAndGetters_SetsValuesCorrectly() {

        String expectedType = NotificationMessage.TYPE_BID_UPDATED;
        String expectedAuctionId = "auction-12345";
        String expectedData = "Dữ liệu payload bất kỳ";

        NotificationMessage message = new NotificationMessage(expectedType, expectedAuctionId, expectedData);


        assertEquals(expectedType, message.getType(), "Type phải khớp với giá trị truyền vào");
        assertEquals(expectedAuctionId, message.getAuctionId(), "AuctionId phải khớp với giá trị truyền vào");
        assertEquals(expectedData, message.getData(), "Data phải khớp với giá trị truyền vào");
    }

    @Test
    void testConstants_HaveCorrectValues() {

        assertEquals("BID_UPDATED", NotificationMessage.TYPE_BID_UPDATED);
        assertEquals("AUCTION_STARTED", NotificationMessage.TYPE_AUCTION_STARTED);
        assertEquals("AUCTION_ENDED", NotificationMessage.TYPE_AUCTION_ENDED);
    }


    @Test
    void testToString_FormatsCorrectly() {

        NotificationMessage message = new NotificationMessage(
                NotificationMessage.TYPE_AUCTION_ENDED,
                "auction-999",
                null
        );


        String expectedOutput = "Notification{type='AUCTION_ENDED',auctionId='auction-999'}";


        assertEquals(expectedOutput, message.toString(), "Hàm toString phải format chuẩn xác để ghi log");
    }
}