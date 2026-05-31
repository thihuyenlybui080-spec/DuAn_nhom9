package vn.edu.vnu.auction.model.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.item.Item;

class SellerTest {

  private Seller seller;

  @BeforeEach
  void setUp() {
    // Initialize a Seller object before each test
    seller = new Seller("seller01", "pass123", "seller@vnu.edu.vn", "Nguyen Van Seller");
  }

  @Test
  void testGetRole() {
    // Verify that the role is exactly "Seller"
    assertEquals("Seller", seller.getRole(), "The role of the object must be 'Seller'");
  }

  @Test
  void testConstructorWithoutId() {
    // Verify inherited fields and default ID from Entity
    assertEquals(-1, seller.getId(), "The default ID from Entity should be -1");
    assertEquals("seller01", seller.getName());
    assertEquals("pass123", seller.getPassword());
    assertEquals("seller@vnu.edu.vn", seller.getEmail());
    assertEquals("Nguyen Van Seller", seller.getFullName());

    // By default, the ownedItems list is not initialized (it is null) unless explicitly set
    assertNull(seller.getOwnedItems(), "The owned items list should be null initially");
  }

  @Test
  void testConstructorWithId() {
    // Verify the constructor that includes an ID
    Seller sellerWithId = new Seller(10, "proSeller", "pro123", "pro@vnu.edu.vn", "Pro Seller");

    assertEquals(10, sellerWithId.getId(),
        "The ID should match the value passed in the constructor");
    assertEquals("proSeller", sellerWithId.getName());
    assertEquals("pro@vnu.edu.vn", sellerWithId.getEmail());
  }

  @Test
  void testSetAndGetOwnedItems() throws Exception {
    Item mockItem1 = Mockito.mock(Item.class);
    Item mockItem2 = Mockito.mock(Item.class);
    List<Item> mockItemList = Arrays.asList(mockItem1, mockItem2);

    // Dùng Reflection nhét dữ liệu vào thay cho hàm Setter không tồn tại
    java.lang.reflect.Field itemsField = Seller.class.getDeclaredField("ownedItems");
    itemsField.setAccessible(true);
    itemsField.set(seller, mockItemList);

    // Retrieve the list and verify its contents
    List<Item> retrievedItems = seller.getOwnedItems();

    assertNotNull(retrievedItems, "The retrieved list should not be null after being set");
    assertEquals(2, retrievedItems.size(), "The list should contain exactly 2 items");
    assertTrue(retrievedItems.contains(mockItem1), "The list must contain the first mock item");
    assertTrue(retrievedItems.contains(mockItem2), "The list must contain the second mock item");
  }
}