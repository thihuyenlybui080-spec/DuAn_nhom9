package org.example.loginregister.server.common.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void testConstructorAndGetters_SetsValuesCorrectly() {

        String expectedAction = Request.ACTION_PLACE_BID;
        String expectedData = "{\"auctionId\":\"auction-123\", \"amount\":500.0}";


        Request request = new Request(expectedAction, expectedData);


        assertEquals(expectedAction, request.getAction(), "Action phải khớp với hằng số đã chọn");
        assertEquals(expectedData, request.getData(), "Data phải khớp với payload truyền vào");
    }

    @Test
    void testActionConstants_AreCorrect() {

        assertEquals("LOGIN", Request.ACTION_LOGIN);
        assertEquals("REGISTER", Request.ACTION_REGISTER);
        assertEquals("PLACE_BID", Request.ACTION_PLACE_BID);
        assertEquals("CANCEL_AUCTION", Request.ACTION_CANCEL_AUCTION);
        assertEquals("GET_ALL_USERS", Request.ACTION_GET_ALL_USERS);
    }


    @Test
    void testToString_FormatsCorrectly() {

        Request request = new Request(Request.ACTION_LOGIN, "user123");


        String expectedOutput = "Request{action='LOGIN', data =user123}";

        assertEquals(expectedOutput, request.toString(), "Hàm toString phải xuất ra format đúng cho log hệ thống");
    }
}