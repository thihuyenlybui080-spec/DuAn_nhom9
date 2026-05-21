package org.example.loginregister.server.model.entity.auto_bidding;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidConfigTest {


    @Test
    void testConstructor_ValidData_CreatesSuccessfully() {

        AutoBidConfig config = new AutoBidConfig(1000.0, 50.0);


        assertEquals(1000.0, config.getMaxBid(), "Max bid phải khớp với giá trị truyền vào");
        assertEquals(50.0, config.getIncrement(), "Increment phải khớp với giá trị truyền vào");


        assertNotNull(config.getRegisteredAt(), "Thời gian đăng ký không được null");
        assertTrue(config.getRegisteredAt().compareTo(LocalDateTime.now()) <= 0,
                "Thời gian đăng ký không thể nằm ở thì tương lai");
    }


    @Test
    void testConstructor_MaxBidZero_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new AutoBidConfig(0.0, 50.0);
        }, "Phải ném lỗi IllegalArgumentException nếu maxBid = 0");

        assertEquals("maxBid must be > 0", exception.getMessage(), "Message lỗi phải chính xác");
    }

    @Test
    void testConstructor_MaxBidNegative_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new AutoBidConfig(-100.0, 50.0);
        }, "Phải ném lỗi IllegalArgumentException nếu maxBid số âm");

        assertEquals("maxBid must be > 0", exception.getMessage());
    }

    @Test
    void testConstructor_IncrementZero_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new AutoBidConfig(1000.0, 0.0);
        }, "Phải ném lỗi IllegalArgumentException nếu increment = 0");

        assertEquals("increment must be > 0", exception.getMessage(), "Message lỗi phải chính xác");
    }

    @Test
    void testConstructor_IncrementNegative_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new AutoBidConfig(1000.0, -10.0);
        }, "Phải ném lỗi IllegalArgumentException nếu increment số âm");

        assertEquals("increment must be > 0", exception.getMessage());
    }


    @Test
    void testToString_ReturnsCorrectFormat() {
        AutoBidConfig config = new AutoBidConfig(500.0, 20.0);
        String result = config.toString();


        assertTrue(result.contains("maxBid=500.0"), "toString phải chứa giá trị maxBid");
        assertTrue(result.contains("increment=20.0"), "toString phải chứa giá trị increment");
        assertTrue(result.contains("registeredAt="), "toString phải chứa trường registeredAt");
    }
}