package vn.edu.vnu.auction.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;

class BidTransactionTest {

  private BidTransaction transaction;
  private Bidder mockBidder;
  private Item mockItem;

  @BeforeEach
  void setUp() {
    // Step 1: Create mock objects for Bidder and Item to isolate the test
    mockBidder = Mockito.mock(Bidder.class);
    mockItem = Mockito.mock(Item.class);

    // Step 2: Initialize the BidTransaction object with a bid amount of 500.0
    transaction = new BidTransaction(mockBidder, mockItem, 500.0);
  }

  @Test
  void testConstructorAndInitialGetters() {
    // Verify that the injected mocks and values are stored correctly
    assertEquals(mockBidder, transaction.getBidder(), "The bidder should match the injected mock");
    assertEquals(mockItem, transaction.getItem(), "The item should match the injected mock");
    assertEquals(500.0, transaction.getAmount(), "The bid amount should be 500.0");

    // The timestamp should be generated automatically in the constructor
    assertNotNull(transaction.getTimestamp(), "The timestamp must be generated and not null");

    // auctionId is not set in the constructor, so for an int primitive, it defaults to 0
    assertEquals(0, transaction.getAuctionId(), "The default primitive int auctionId should be 0");
  }
}
