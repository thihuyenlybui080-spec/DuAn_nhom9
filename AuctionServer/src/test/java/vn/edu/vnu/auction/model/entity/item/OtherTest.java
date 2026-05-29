package vn.edu.vnu.auction.model.entity.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OtherTest {

    private Other otherItem;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        // Initialize mock dates for testing
        startTime = LocalDateTime.of(2026, 9, 1, 8, 0);
        endTime = LocalDateTime.of(2026, 9, 15, 20, 0);

        // Initialize an Other object before each test (using the constructor without ID)
        otherItem = new Other("Mystery Box", 601, "A box full of surprises", 50.0, startTime, endTime);
    }

    @Test
    void testGetCategory() {
        // Verify that the overridden method returns exactly "Other"
        assertEquals("Other", otherItem.getCategory(), "The category of the object must be 'Other'");
    }

    @Test
    void testConstructorWithoutId() {
        // Verify that the super() call passed the data correctly to the Item class
        assertEquals(-1, otherItem.getId(), "Default ID should be -1 from Entity");
        assertEquals("Mystery Box", otherItem.getItemName());
        assertEquals(601, otherItem.getSellerId());
        assertEquals("A box full of surprises", otherItem.getDescription());
        assertEquals(50.0, otherItem.getStartingPrice());
        assertEquals(startTime, otherItem.getStartTime());
        assertEquals(endTime, otherItem.getEndTime());
    }

    @Test
    void testConstructorWithId() {
        // Verify the constructor that includes an ID
        Other otherWithId = new Other(605, "Vintage Stamp", 602, "Rare 19th-century stamp", 200.0, startTime, endTime);

        assertEquals(605, otherWithId.getId(), "The ID should match the value passed in the constructor");
        assertEquals("Vintage Stamp", otherWithId.getItemName());
        assertEquals(602, otherWithId.getSellerId());
        assertEquals(200.0, otherWithId.getStartingPrice());
    }
}