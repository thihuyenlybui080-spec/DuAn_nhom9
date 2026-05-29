package vn.edu.vnu.auction.model.entity.auto_bidding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.user.Bidder;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidTest {

    private AutoBid autoBid;
    private Bidder mockBidder;
    private AutoBidConfig mockConfig;
    private final int testAuctionId = 101;

    @BeforeEach
    void setUp() {
        // Step 1: Mock the dependencies to isolate the AutoBid class
        mockBidder = Mockito.mock(Bidder.class);
        mockConfig = Mockito.mock(AutoBidConfig.class);

        // Step 2: Initialize the AutoBid object before each test
        autoBid = new AutoBid(mockBidder, testAuctionId, mockConfig);
    }

    @Test
    void testConstructorAndGetters() {
        // Verify that the dependencies and primitives were assigned correctly
        assertEquals(mockBidder, autoBid.getBidder(), "The bidder should match the injected mock");
        assertEquals(testAuctionId, autoBid.getAuctionId(), "The auction ID should be 101");
        assertEquals(mockConfig, autoBid.getConfig(), "The config should match the injected mock");

        // Verify that the registration timestamp was generated automatically and is not null
        assertNotNull(autoBid.getRegisteredAt(), "The registeredAt timestamp must be generated automatically");

        // Verify the default active state is true
        assertTrue(autoBid.isActive(), "A new AutoBid must be active by default");
    }

    @Test
    void testDeactivate() {
        // Verify that calling deactivate() changes the state from true to false
        autoBid.deactivate();

        assertFalse(autoBid.isActive(), "The AutoBid should not be active after calling deactivate()");
    }
}