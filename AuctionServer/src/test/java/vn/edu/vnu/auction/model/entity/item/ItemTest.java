package vn.edu.vnu.auction.model.entity.item;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemTest {

  // Concrete dummy subclass to test the abstract Item class
  private static class DummyItem extends Item {

    public DummyItem(String itemName, int createdBy, String description, double startingPrice,
        LocalDateTime startTime, LocalDateTime endTime) {
      super(itemName, createdBy, description, startingPrice, startTime, endTime);
    }

    public DummyItem(int id, String itemName, int createdBy, String description,
        double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
      super(id, itemName, createdBy, description, startingPrice, startTime, endTime);
    }

    @Override
    public String getCategory() {
      return "DummyCategory";
    }
  }

  private Item item;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @BeforeEach
  void setUp() {
    // Initialize mock dates for testing
    startTime = LocalDateTime.of(2026, 5, 28, 10, 0);
    endTime = LocalDateTime.of(2026, 5, 30, 10, 0);

    // Initialize the dummy item object before each test
    item = new DummyItem("Vintage Clock", 101, "A beautiful vintage clock", 150.0, startTime,
        endTime);
  }

  @Test
  void testConstructorWithoutId() {
    // Verify default ID and fields are assigned correctly
    assertEquals(-1, item.getId(), "Default ID should be -1 from Entity");
    assertEquals("Vintage Clock", item.getItemName());
    assertEquals("A beautiful vintage clock", item.getDescription());
    assertEquals(150.0, item.getStartingPrice());
    assertEquals(startTime, item.getStartTime());
    assertEquals(endTime, item.getEndTime());

    // createdBy should map to sellerId
    assertEquals(101, item.getSellerId(), "Seller ID must match the createdBy value");

    // imagePath is not in the constructor, so it should be null initially
    assertNull(item.getImagePath(), "Image path should be null initially");
  }

  @Test
  void testConstructorWithId() {
    Item itemWithId = new DummyItem(99, "Antique Vase", 102, "Ming dynasty vase", 5000.0, startTime,
        endTime);

    assertEquals(99, itemWithId.getId());
    assertEquals("Antique Vase", itemWithId.getItemName());
    assertEquals(102, itemWithId.getSellerId());
  }

  @Test
  void testSettersAndGetters() {
    LocalDateTime newStartTime = LocalDateTime.of(2026, 6, 1, 9, 0);
    LocalDateTime newEndTime = LocalDateTime.of(2026, 6, 5, 18, 0);

    // Update all fields
    item.setItemName("New Name");
    item.setDescription("New Description");
    item.setStartingPrice(200.0);
    item.setStartTime(newStartTime);
    item.setEndTime(newEndTime);
    item.setImagePath("/images/new_image.png");

    // Verify that the updates took effect
    assertEquals("New Name", item.getItemName());
    assertEquals("New Description", item.getDescription());
    assertEquals(200.0, item.getStartingPrice());
    assertEquals(newStartTime, item.getStartTime());
    assertEquals(newEndTime, item.getEndTime());
    assertEquals("/images/new_image.png", item.getImagePath());
  }

  @Test
  void testGetCategory() {
    // Verify that the overridden method works correctly
    assertEquals("DummyCategory", item.getCategory(),
        "Should return the category defined in the dummy class");
  }

  @Test
  void testPrintInfo() {
    // Since printInfo only logs information via SLF4J and returns void,
    // we assert that calling it does not throw any exceptions
    assertDoesNotThrow(() -> item.printInfo(),
        "printInfo should execute without throwing exceptions");
  }
}