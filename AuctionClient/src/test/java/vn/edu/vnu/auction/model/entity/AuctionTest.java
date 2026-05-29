package vn.edu.vnu.auction.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.common.exception.AuctionClosedException;
import vn.edu.vnu.auction.common.exception.InvalidBidException;
import vn.edu.vnu.auction.common.observer.Observer;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Auction auction;
    private Item mockItem;
    private Bidder mockBidder1;
    private Bidder mockBidder2;

    @BeforeEach
    void setUp() {
        // Step 1: Mock the Item with a starting price and future end time
        mockItem = Mockito.mock(Item.class);
        Mockito.when(mockItem.getStartingPrice()).thenReturn(100.0);

        // Set an end time far in the future so the auction is considered active
        LocalDateTime futureEndTime = LocalDateTime.now().plusDays(1);
        Mockito.when(mockItem.getEndTime()).thenReturn(futureEndTime);

        // Step 2: Initialize the Auction object
        auction = new Auction(1, mockItem);

        // Step 3: Mock Bidders
        mockBidder1 = Mockito.mock(Bidder.class);
        Mockito.when(mockBidder1.getName()).thenReturn("Bidder One");

        mockBidder2 = Mockito.mock(Bidder.class);
        Mockito.when(mockBidder2.getName()).thenReturn("Bidder Two");
    }

    @Test
    void testConstructorAndInitialState() {
        assertEquals(1, auction.getId(), "Auction ID should be 1");
        assertEquals(AuctionStatus.OPEN, auction.getStatus(), "Default status should be OPEN");
        assertEquals(100.0, auction.getCurrentPrice(), "Current price should equal item's starting price");
        assertNull(auction.getHighestBidder(), "Highest bidder should be null initially");
        assertTrue(auction.getBids().isEmpty(), "Bids list should be empty initially");
    }

    @Test
    void testProcessBid_Success() {
        // Test the public wrapper method processBid
        boolean result = auction.processBid(mockBidder1, 150.0);

        assertTrue(result, "processBid should return true for a valid bid");
        assertEquals(150.0, auction.getCurrentPrice(), "Current price should be updated");
        assertEquals(mockBidder1, auction.getHighestBidder(), "Highest bidder should be Bidder One");
        assertEquals(1, auction.getBids().size(), "There should be exactly 1 bid in the history");
    }

    @Test
    void testProcessBid_Fail_InvalidUser() {
        boolean result = auction.processBid(null, 200.0);
        assertFalse(result, "processBid should return false if bidder is null");
    }

    @Test
    void testPlaceBid_Fail_InvalidAmount() {
        // Create a mock BidTransaction with an amount lower than the current price (100.0)
        BidTransaction invalidBid = Mockito.mock(BidTransaction.class);
        Mockito.when(invalidBid.getAmount()).thenReturn(50.0);

        // Expect InvalidBidException to be thrown
        InvalidBidException exception = assertThrows(InvalidBidException.class,
                () -> auction.placeBid(invalidBid));

        assertEquals("Bid amount must be greater than current price!", exception.getMessage());
    }

    @Test
    void testPlaceBid_Fail_AuctionClosed() {
        // Change status to FINISHED
        auction.setStatus(AuctionStatus.FINISHED);

        BidTransaction validBid = Mockito.mock(BidTransaction.class);
        Mockito.when(validBid.getAmount()).thenReturn(200.0);

        // Expect AuctionClosedException to be thrown
        AuctionClosedException exception = assertThrows(AuctionClosedException.class,
                () -> auction.placeBid(validBid));

        assertEquals("Auction is already closed!", exception.getMessage());
    }

    @Test
    void testFinishAuction() {
        auction.finishAuction(AuctionStatus.FINISHED);
        assertEquals(AuctionStatus.FINISHED, auction.getStatus(), "Status should be updated to FINISHED");
    }

    @Test
    void testObserverPattern() {
        // Mock an observer
        Observer mockObserver = Mockito.mock(Observer.class);

        // Add observer to auction
        auction.addObserver(mockObserver);

        // Place a successful bid (this triggers notifyObservers internally)
        auction.processBid(mockBidder1, 250.0);

        // Verify that the observer's update() method was called exactly once with the correct arguments
        Mockito.verify(mockObserver, Mockito.times(1)).update(1, 250.0, "Bidder One");
    }

    @Test
    void testCancelBidsFrom_HighestBidderRemoved() {
        // Scenario: Bidder 1 bids 150, Bidder 2 bids 200. Then Bidder 2 gets canceled.
        // The highest bidder should fallback to Bidder 1, and price to 150.

        // Setup raw BidTransactions (since placeBid uses them)
        BidTransaction bid1 = Mockito.mock(BidTransaction.class);
        Mockito.when(bid1.getBidder()).thenReturn(mockBidder1);
        Mockito.when(bid1.getAmount()).thenReturn(150.0);

        BidTransaction bid2 = Mockito.mock(BidTransaction.class);
        Mockito.when(bid2.getBidder()).thenReturn(mockBidder2);
        Mockito.when(bid2.getAmount()).thenReturn(200.0);

        assertDoesNotThrow(() -> {
            auction.placeBid(bid1, false);
            auction.placeBid(bid2, false);
        });

        assertEquals(mockBidder2, auction.getHighestBidder(), "Bidder Two should be the highest initially");
        assertEquals(200.0, auction.getCurrentPrice());

        // Action: Cancel all bids from Bidder 2
        auction.cancelBidsFrom(mockBidder2);

        // Verify: Bidder 1 is now the king again
        assertEquals(mockBidder1, auction.getHighestBidder(), "Highest bidder should fallback to Bidder One");
        assertEquals(150.0, auction.getCurrentPrice(), "Price should fallback to 150.0");
        assertEquals(1, auction.getBids().size(), "Only Bidder One's bid should remain in the list");
    }
}