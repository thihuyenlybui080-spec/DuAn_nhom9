package vn.edu.vnu.auction.model.entity.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.auction.model.entity.item.Electronics;
import vn.edu.vnu.auction.model.entity.item.Item;

class ElectronicsFactoryTest {

  private ElectronicsFactory electronicsFactory;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @BeforeEach
  void setUp() {
    // Initialize the factory before each test
    electronicsFactory = new ElectronicsFactory();

    // Prepare standard time objects for testing
    startTime = LocalDateTime.of(2026, 10, 1, 8, 30);
    endTime = LocalDateTime.of(2026, 10, 5, 22, 0);
  }

  @Test
  void testCreateItem() {
    // Step 1: Prepare the test data
    String expectedName = "Alienware Gaming Laptop";
    int expectedCreatorId = 404;
    String expectedDescription = "High-end gaming laptop with RTX 4090 and 64GB RAM";
    double expectedPrice = 3500.0;

    // Step 2: Use the factory to create the item
    Item createdItem = electronicsFactory.createItem(
        expectedName,
        expectedCreatorId,
        expectedDescription,
        expectedPrice,
        startTime,
        endTime
    );

    // Step 3: Verify the core factory logic
    assertNotNull(createdItem,
        "The factory must successfully create and return an item (cannot be null)");
    assertTrue(createdItem instanceof Electronics,
        "The created item must be an instance of the Electronics class");

    // Step 4: Verify that all data fields were passed correctly into the object
    assertEquals(expectedName, createdItem.getItemName(), "The item name must match the input");
    assertEquals(expectedCreatorId, createdItem.getSellerId(),
        "The creator/seller ID must match the input");
    assertEquals(expectedDescription, createdItem.getDescription(),
        "The description must match the input");
    assertEquals(expectedPrice, createdItem.getStartingPrice(),
        "The starting price must match the input");
    assertEquals(startTime, createdItem.getStartTime(), "The start time must match the input");
    assertEquals(endTime, createdItem.getEndTime(), "The end time must match the input");

    // Verify the category dynamically
    assertEquals("Electronics", createdItem.getCategory(),
        "The category of the created item must be 'Electronics'");
  }
}