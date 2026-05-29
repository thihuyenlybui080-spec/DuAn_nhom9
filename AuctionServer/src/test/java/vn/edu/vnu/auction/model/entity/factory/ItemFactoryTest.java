package vn.edu.vnu.auction.model.entity.factory;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.item.Item;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void testAbstractCreateItemContract() {
        // Step 1: Create a mock Item to represent the output of our factory
        Item mockItem = Mockito.mock(Item.class);

        // Step 2: Create an anonymous subclass to implement the abstract ItemFactory
        // This proves that any class extending ItemFactory must implement this exact signature.
        ItemFactory anonymousFactory = new ItemFactory() {
            @Override
            public Item createItem(String itemName, int createdBy, String description,
                                   double startingPrice, LocalDateTime startingTime, LocalDateTime endTime) {

                // Inside this anonymous implementation, we verify that the parameters
                // passed from the caller are received correctly.
                assertEquals("Generic Item", itemName, "The parameter 'itemName' must be passed correctly");
                assertEquals(101, createdBy, "The parameter 'createdBy' must be passed correctly");
                assertEquals(500.0, startingPrice, "The parameter 'startingPrice' must be passed correctly");

                return mockItem; // Return the mock item we created earlier
            }
        };

        // Step 3: Prepare the test data
        String testName = "Generic Item";
        int testCreatorId = 101;
        String testDescription = "Just a generic test item description";
        double testPrice = 500.0;
        LocalDateTime testStartTime = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime testEndTime = LocalDateTime.of(2026, 6, 10, 18, 0);

        // Step 4: Invoke the method on our anonymous subclass
        Item resultItem = anonymousFactory.createItem(
                testName,
                testCreatorId,
                testDescription,
                testPrice,
                testStartTime,
                testEndTime
        );

        // Step 5: Verify the final output
        assertNotNull(resultItem, "The abstract factory implementation must be able to return an Item");
        assertEquals(mockItem, resultItem, "The returned item must exactly match our mock object");
    }
}