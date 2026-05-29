package vn.edu.vnu.auction.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.User;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuctionResultTest {

    private AuctionResult auctionResult;
    private Auction mockAuction;
    private Item mockItem;
    private User mockWinner;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        // Step 1: Mock the Item
        mockItem = Mockito.mock(Item.class);
        Mockito.when(mockItem.getItemName()).thenReturn("Rolex Watch");
        endTime = LocalDateTime.of(2026, 12, 31, 23, 59);
        Mockito.when(mockItem.getEndTime()).thenReturn(endTime);

        // Step 2: Mock the Winner (User)
        mockWinner = Mockito.mock(User.class);
        Mockito.when(mockWinner.getName()).thenReturn("ProBidder99");

        // Step 3: Mock the BidHistory list
        BidTransaction mockBid1 = Mockito.mock(BidTransaction.class);
        BidTransaction mockBid2 = Mockito.mock(BidTransaction.class);
        List<BidTransaction> mockBids = Arrays.asList(mockBid1, mockBid2);

        // Step 4: Mock the Auction itself, which acts as the source of truth
        mockAuction = Mockito.mock(Auction.class);
        Mockito.when(mockAuction.getId()).thenReturn(101);
        Mockito.when(mockAuction.getItem()).thenReturn(mockItem);
        // Note: Using a sneaky cast because Auction's getHighestBidder returns Bidder, which is a User
        Mockito.when(mockAuction.getHighestBidder()).thenReturn((vn.edu.vnu.auction.model.entity.user.Bidder) mockWinner);
        Mockito.when(mockAuction.getCurrentPrice()).thenReturn(15000.0);
        Mockito.when(mockAuction.getStatus()).thenReturn(AuctionStatus.FINISHED);
        Mockito.when(mockAuction.getBids()).thenReturn(mockBids);

        // Step 5: Initialize the target object using the mock Auction
        auctionResult = new AuctionResult(mockAuction);
    }

    @Test
    void testConstructorAndGetters() {
        // Verify that AuctionResult properly extracted all fields from the Mock Auction
        assertEquals(101, auctionResult.getAuctionId(), "Auction ID should be 101");
        assertEquals(mockItem, auctionResult.getItem(), "Item should match the mock item");
        assertEquals(mockWinner, auctionResult.getWinner(), "Winner should match the mock winner");
        assertEquals(15000.0, auctionResult.getFinalPrice(), "Final price should be 15000.0");
        assertEquals(AuctionStatus.FINISHED, auctionResult.getStatus(), "Status should be FINISHED");
        assertEquals(endTime, auctionResult.getEndTime(), "End time should match the item's end time");

        // Verify bid history was copied correctly
        List<BidTransaction> history = auctionResult.getBidHistory();
        assertNotNull(history, "Bid history should not be null");
        assertEquals(2, history.size(), "Bid history should contain 2 mock bids");
    }

    @Test
    void testSetStatus() {
        // Test updating the status directly
        auctionResult.setStatus(AuctionStatus.PAID);
        assertEquals(AuctionStatus.PAID, auctionResult.getStatus(), "Status should be updated to PAID");
    }

    @Test
    void testToString_WithWinner() {
        // Test the custom toString format when there is a winner
        String expectedString = "Session 101 | Item: Rolex Watch | Winner: ProBidder99 | Final Price: 15000";
        assertEquals(expectedString, auctionResult.toString(), "toString should format correctly with a winner");
    }

    @Test
    void testToString_WithoutWinner() {
        // Re-mock to simulate an auction that ended with NO bids (winner is null)
        Mockito.when(mockAuction.getHighestBidder()).thenReturn(null);
        AuctionResult noWinnerResult = new AuctionResult(mockAuction);

        String expectedString = "Session 101 | Item: Rolex Watch | Winner: None | Final Price: 15000";
        assertEquals(expectedString, noWinnerResult.toString(), "toString should handle null winner safely and output 'None'");
    }
}