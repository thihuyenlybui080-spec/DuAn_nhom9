package org.example.loginregister.server.common.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTest {

    @Test
    void testResponseOk_WithDataOnly() {
        String testData = "Login successful";
        Response response = Response.ok(testData);

        assertTrue(response.isSuccess(), "Response thành công phải trả về true");
        assertEquals("OK", response.getMessage(), "Message mặc định phải là 'OK'");
        assertEquals(testData, response.getData(), "Data phải khớp với dữ liệu truyền vào");
    }

    @Test
    void testResponseOk_WithMessageAndData() {
        String testMessage = "Update complete";
        Integer testData = 200;
        Response response = Response.ok(testMessage, testData);

        assertTrue(response.isSuccess());
        assertEquals(testMessage, response.getMessage());
        assertEquals(testData, response.getData());
    }

    @Test
    void testResponseError() {
        String errorMessage = "Invalid username";
        Response response = Response.error(errorMessage);

        assertFalse(response.isSuccess(), "Response thất bại phải trả về false");
        assertEquals(errorMessage, response.getMessage());
        assertNull(response.getData(), "Response lỗi thường không trả về data");
    }

    @Test
    void testToString_FormatsCorrectly() {
        Response response = Response.error("Not found");


        String expectedOutput = "Response{success=false, message='Not found', data=null}";

        assertEquals(expectedOutput, response.toString(), "Hàm toString phải định dạng đúng để debug/ghi log");
    }
}