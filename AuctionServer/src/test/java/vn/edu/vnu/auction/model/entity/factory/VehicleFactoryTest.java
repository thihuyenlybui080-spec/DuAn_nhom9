package vn.edu.vnu.auction.model.entity.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.item.Vehicle;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VehicleFactoryTest {

    private VehicleFactory vehicleFactory;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        // Initialize the factory before each test
        vehicleFactory = new VehicleFactory();

        // Prepare standard time objects for testing
        startTime = LocalDateTime.of(2026, 12, 1, 9, 0);
        endTime = LocalDateTime.of(2026, 12, 15, 17, 30);
    }

    @Test
    void testCreateItem() {
        // Step 1: Prepare the test data
        String expectedName = "Toyota Camry 2025";
        int expectedCreatorId = 555;
        String expectedDescription = "A brand new sedan in excellent condition";
        double expectedPrice = 25000.0;

        // Step 2: Use the factory to create the item
        Item createdItem = vehicleFactory.createItem(
                expectedName,
                expectedCreatorId,
                expectedDescription,
                expectedPrice,
                startTime,
                endTime
        );

        // Step 3: Verify the core factory logic
        assertNotNull(createdItem, "The factory must successfully create and return an item (cannot be null)");
        assertTrue(createdItem instanceof Vehicle, "The created item must be an instance of the Vehicle class");

        // Step 4: Verify that all data fields were passed correctly into the object
        assertEquals(expectedName, createdItem.getItemName(), "The item name must match the input");
        assertEquals(expectedCreatorId, createdItem.getSellerId(), "The creator/seller ID must match the input");
        assertEquals(expectedDescription, createdItem.getDescription(), "The description must match the input");
        assertEquals(expectedPrice, createdItem.getStartingPrice(), "The starting price must match the input");
        assertEquals(startTime, createdItem.getStartTime(), "The start time must match the input");
        assertEquals(endTime, createdItem.getEndTime(), "The end time must match the input");

        // Verify the category dynamically
        assertEquals("Vehicle", createdItem.getCategory(), "The category of the created item must be 'Vehicle'");
    }
}