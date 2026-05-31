package vn.edu.vnu.auction.model.entity.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ElectronicsTest {

  private Electronics electronics;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @BeforeEach
  void setUp() {
    // Initialize mock dates for testing
    startTime = LocalDateTime.of(2026, 7, 1, 9, 0);
    endTime = LocalDateTime.of(2026, 7, 10, 18, 0);

    // Initialize an Electronics object before each test (using the constructor without ID)
    electronics = new Electronics("Gaming Laptop", 301, "High performance gaming laptop", 1500.0,
        startTime, endTime);
  }

  @Test
  void testGetCategory() {
    // Verify that the overridden method returns exactly "Electronics"
    assertEquals("Electronics", electronics.getCategory(),
        "The category of the object must be 'Electronics'");
  }

  @Test
  void testConstructorWithoutId() {
    // Verify that the super() call passed the data correctly to the Item class
    assertEquals(-1, electronics.getId(), "Default ID should be -1 from Entity");
    assertEquals("Gaming Laptop", electronics.getItemName());
    assertEquals(301, electronics.getSellerId());
    assertEquals("High performance gaming laptop", electronics.getDescription());
    assertEquals(1500.0, electronics.getStartingPrice());
    assertEquals(startTime, electronics.getStartTime());
    assertEquals(endTime, electronics.getEndTime());
  }

  @Test
  void testConstructorWithId() {
    // Verify the constructor that includes an ID
    Electronics electronicsWithId = new Electronics(404, "Smartphone", 302,
        "Latest flagship smartphone", 800.0, startTime, endTime);

    assertEquals(404, electronicsWithId.getId(),
        "The ID should match the value passed in the constructor");
    assertEquals("Smartphone", electronicsWithId.getItemName());
    assertEquals(302, electronicsWithId.getSellerId());
    assertEquals(800.0, electronicsWithId.getStartingPrice());
  }
}