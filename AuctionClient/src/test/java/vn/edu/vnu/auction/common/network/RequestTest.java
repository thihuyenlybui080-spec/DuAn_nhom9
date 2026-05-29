package vn.edu.vnu.auction.common.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void testConstructorAndGetters() {
        // Step 1: Prepare the test data using the constants defined in the class
        String expectedAction = Request.ACTION_PLACE_BID;
        String expectedData = "BidAmount:5000";

        // Step 2: Initialize the Request object
        Request request = new Request(expectedAction, expectedData);

        // Step 3: Verify that the fields are assigned correctly
        assertEquals(expectedAction, request.getAction(), "The action must match the provided input");
        assertEquals(expectedData, request.getData(), "The data payload must match the provided input");

        // Verify that the UUID is generated automatically
        assertNotNull(request.getRequestId(), "The requestId (UUID) must not be null");
        assertFalse(request.getRequestId().isEmpty(), "The requestId (UUID) must not be empty");
    }

    @Test
    void testUniqueRequestIds() {
        // Create two requests with the exact same content
        Request request1 = new Request(Request.ACTION_GET_AUCTIONS, null);
        Request request2 = new Request(Request.ACTION_GET_AUCTIONS, null);

        // Ensure that their unique identifiers are completely different (UUID characteristic)
        assertNotEquals(request1.getRequestId(), request2.getRequestId(), "Two different requests must have distinct UUIDs");
    }

    @Test
    void testToString() {
        // Test if the custom toString() method returns the expected format
        String action = Request.ACTION_LOGIN;
        String data = "UserCredentials";

        Request request = new Request(action, data);
        String expectedString = "Request{action='LOGIN', data =UserCredentials}";

        assertEquals(expectedString, request.toString(), "The toString() output must match the expected format string");
    }
}