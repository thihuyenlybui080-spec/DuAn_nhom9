package vn.edu.vnu.auction.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResponseTest {

  @Test
  void testOk_WithDataOnly() {
    String testData = "UserList";
    Response response = Response.ok((Object) testData);

    assertTrue(response.isSuccess(), "The ok() response must set success to true");
    assertEquals("OK", response.getMessage(), "The default message must be 'OK'");
    assertEquals(testData, response.getData(), "The data must match the provided input payload");
    assertNull(response.getRequestId(), "The requestId must be null if not explicitly provided");
  }

  @Test
  void testOk_WithMessageAndData() {
    String testMessage = "Login Successful";
    String testData = "Token-123";
    Response response = Response.ok(testMessage, (Object) testData);

    assertTrue(response.isSuccess(), "The response must indicate success");
    assertEquals(testMessage, response.getMessage(), "The message must match the custom input");
    assertEquals(testData, response.getData(), "The data must match the provided input payload");
    assertNull(response.getRequestId(), "The requestId must remain null");
  }

  @Test
  void testOk_WithDataAndRequestId() {
    Object testData = "AuctionDetails";
    String testReqId = "req-999";
    Response response = Response.ok(testData, testReqId);

    assertTrue(response.isSuccess(), "The response must indicate success");
    assertEquals("OK", response.getMessage(), "The default message must be 'OK'");
    assertEquals(testData, response.getData(), "The data must match the provided input payload");
    assertEquals(testReqId, response.getRequestId(), "The requestId must match the provided UUID");
  }

  @Test
  void testOk_WithMessageDataAndRequestId() {
    String testMessage = "Bid Placed";
    Double testData = 5000.0;
    String testReqId = "req-111";
    Response response = Response.ok(testMessage, testData, testReqId);

    assertTrue(response.isSuccess(), "The response must indicate success");
    assertEquals(testMessage, response.getMessage(), "The message must match the custom input");
    assertEquals(testData, response.getData(), "The data must match the provided input payload");
    assertEquals(testReqId, response.getRequestId(), "The requestId must match the provided UUID");
  }

  @Test
  void testError_WithMessageOnly() {
    String errorMessage = "Invalid username or password";
    Response response = Response.error(errorMessage);

    assertFalse(response.isSuccess(), "The error() response must set success to false");
    assertEquals(errorMessage, response.getMessage(), "The error message must match the input");
    assertNull(response.getData(), "The payload data must be null for error responses");
    assertNull(response.getRequestId(), "The requestId must be null if not explicitly provided");
  }

  @Test
  void testError_WithMessageAndRequestId() {
    String errorMessage = "Auction already finished";
    String testReqId = "req-404";
    Response response = Response.error(errorMessage, testReqId);

    assertFalse(response.isSuccess(), "The error() response must set success to false");
    assertEquals(errorMessage, response.getMessage(), "The error message must match the input");
    assertNull(response.getData(), "The payload data must be null for error responses");
    assertEquals(testReqId, response.getRequestId(), "The requestId must match the provided UUID");
  }

  @Test
  void testSetRequestId() {
    Response response = Response.ok("SomeData");
    assertNull(response.getRequestId(), "Initial requestId should be null");

    // Verify that the setter updates the requestId field correctly
    response.setRequestId("new-req-id");
    assertEquals("new-req-id", response.getRequestId(),
        "The setter must correctly update the requestId");
  }

  @Test
  void testToString() {
    Response response = Response.ok("Custom Message", (Object) "MyData");

    // Expected format: "Response{success=true, message='Custom Message', data=MyData}"
    String expectedString = "Response{success=true, message='Custom Message', data=MyData}";

    assertEquals(expectedString, response.toString(),
        "The toString() output must match the expected format string");
  }
}