package vn.edu.vnu.auction.model.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.item.Other;

class OtherFactoryTest {

  private OtherFactory otherFactory;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  @BeforeEach
  void setUp() {
    // Initialize the factory before each test
    otherFactory = new OtherFactory();

    // Prepare standard time objects for testing
    startTime = LocalDateTime.of(2026, 11, 1, 10, 0);
    endTime = LocalDateTime.of(2026, 11, 10, 15, 30);
  }

  @Test
  void testCreateItem() {
    // Step 1: Prepare the test data
    String expectedName = "Miscellaneous Collectible";
    int expectedCreatorId = 777;
    String expectedDescription = "A rare miscellaneous item that does not fit into other categories";
    double expectedPrice = 150.0;

    // Step 2: Use the factory to create the item
    Item createdItem = otherFactory.createItem(
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
    assertTrue(createdItem instanceof Other,
        "The created item must be an instance of the Other class");

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
    assertEquals("Other", createdItem.getCategory(),
        "The category of the created item must be 'Other'");
  }
}