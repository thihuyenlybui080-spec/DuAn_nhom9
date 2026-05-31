package vn.edu.vnu.auction.model.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.item.Item;

class BidderTest {

  private Bidder bidder;
  private Item mockItem;

  @BeforeEach
  void setUp() {

    bidder = new Bidder("bidder01", "pass123", "bidder@vnu.edu.vn", "Nguyen Van Bidder");

    mockItem = Mockito.mock(Item.class);
    Mockito.when(mockItem.getItemName()).thenReturn("Antique Vase");
  }

  @Test
  void testGetRole() {

    assertEquals("Bidder", bidder.getRole(), "The role of the object must be 'Bidder'");
  }

  @Test
  void testConstructorWithoutId() {

    assertEquals(-1, bidder.getId(), "The default ID from Entity should be -1");
    assertEquals("bidder01", bidder.getName());
    assertEquals("pass123", bidder.getPassword());
    assertEquals("bidder@vnu.edu.vn", bidder.getEmail());
    assertEquals("Nguyen Van Bidder", bidder.getFullName());
  }

  @Test
  void testConstructorWithId() {

    Bidder bidderWithId = new Bidder(5, "vipBidder", "vip123", "vip@vnu.edu.vn", "Vip User");

    assertEquals(5, bidderWithId.getId(),
        "The ID should match the value passed in the constructor");
    assertEquals("vipBidder", bidderWithId.getName());
    assertEquals("vip@vnu.edu.vn", bidderWithId.getEmail());
  }

}