package vn.edu.vnu.auction.model.entity.auto_bidding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoBidConfigTest {

    @Test
    void testConstructorAndGetters_Success() {
        // Step 1: Create a valid configuration
        AutoBidConfig config = new AutoBidConfig(1000.0, 50.0);

        // Step 2: Verify the getters return exactly what we passed in
        assertEquals(1000.0, config.maxBid(), "The max bid should be 1000.0");
        assertEquals(50.0, config.increment(), "The increment should be 50.0");
    }

    @Test
    void testConstructor_Fail_MaxBidZeroOrNegative() {
        // Test when maxBid is exactly 0
        IllegalArgumentException exceptionZero = assertThrows(IllegalArgumentException.class,
                () -> new AutoBidConfig(0, 50.0),
                "Constructor must throw an exception if maxBid is 0");
        assertEquals("maxBid must be > 0", exceptionZero.getMessage());

        // Test when maxBid is negative
        IllegalArgumentException exceptionNegative = assertThrows(IllegalArgumentException.class,
                () -> new AutoBidConfig(-100.0, 50.0),
                "Constructor must throw an exception if maxBid is negative");
        assertEquals("maxBid must be > 0", exceptionNegative.getMessage());
    }

    @Test
    void testConstructor_Fail_IncrementZeroOrNegative() {
        // Test when increment is exactly 0
        IllegalArgumentException exceptionZero = assertThrows(IllegalArgumentException.class,
                () -> new AutoBidConfig(1000.0, 0),
                "Constructor must throw an exception if increment is 0");
        assertEquals("increment must be > 0", exceptionZero.getMessage());

        // Test when increment is negative
        IllegalArgumentException exceptionNegative = assertThrows(IllegalArgumentException.class,
                () -> new AutoBidConfig(1000.0, -25.0),
                "Constructor must throw an exception if increment is negative");
        assertEquals("increment must be > 0", exceptionNegative.getMessage());
    }

    @Test
    void testToString() {
        // Verify the custom toString format
        AutoBidConfig config = new AutoBidConfig(500.0, 10.0);
        String expectedString = "AutoBidConfig{maxBid=500.0, increment=10.0}";

        assertEquals(expectedString, config.toString(), "The toString output must match the expected format");
    }
}