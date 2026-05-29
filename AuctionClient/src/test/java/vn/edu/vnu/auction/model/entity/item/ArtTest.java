package vn.edu.vnu.auction.model.entity.item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtTest {

    private Art art;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        // Initialize mock dates for testing
        startTime = LocalDateTime.of(2026, 6, 1, 8, 0);
        endTime = LocalDateTime.of(2026, 6, 15, 20, 0);

        // Initialize an Art object before each test (using the constructor without ID)
        art = new Art("Mona Lisa Replica", 201, "High quality oil painting", 5000.0, startTime, endTime);
    }

    @Test
    void testGetCategory() {
        // Verify that the overridden method returns exactly "Art"
        assertEquals("Art", art.getCategory(), "The category of the object must be 'Art'");
    }

    @Test
    void testConstructorWithoutId() {
        // Verify that the super() call passed the data correctly to the Item class
        assertEquals(-1, art.getId(), "Default ID should be -1 from Entity");
        assertEquals("Mona Lisa Replica", art.getItemName());
        assertEquals(201, art.getSellerId());
        assertEquals("High quality oil painting", art.getDescription());
        assertEquals(5000.0, art.getStartingPrice());
        assertEquals(startTime, art.getStartTime());
        assertEquals(endTime, art.getEndTime());
    }

    @Test
    void testConstructorWithId() {
        // Verify the constructor that includes an ID
        Art artWithId = new Art(88, "Starry Night Print", 202, "Beautiful canvas print", 300.0, startTime, endTime);

        assertEquals(88, artWithId.getId(), "The ID should match the value passed in the constructor");
        assertEquals("Starry Night Print", artWithId.getItemName());
        assertEquals(202, artWithId.getSellerId());
        assertEquals(300.0, artWithId.getStartingPrice());
    }
}