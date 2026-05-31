package vn.edu.vnu.auction.model.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.item.Item;

/**
 * Lớp kiểm thử (Unit Test) dành cho Entity {@link Seller}.
 * <p>
 * Đảm bảo các chức năng khởi tạo, kế thừa từ lớp cha {@link User},
 * phân quyền vai trò (role), và quản lý danh sách sản phẩm (ownedItems) hoạt động chính xác.
 * </p>
 */
class SellerTest {

  private Seller seller;

  /**
   * Thiết lập dữ liệu trước mỗi kịch bản kiểm thử.
   * Khởi tạo một đối tượng Seller mẫu để tránh ảnh hưởng chéo giữa các bài test.
   */
  @BeforeEach
  void setUp() {
    seller = new Seller("seller01", "pass123", "seller@vnu.edu.vn", "Nguyen Van Seller");
  }

  /**
   * Kiểm tra phương thức lấy vai trò của người dùng.
   * Kỳ vọng: Chuỗi trả về bắt buộc phải là "Seller".
   */
  @Test
  void testGetRole() {
    assertEquals("Seller", seller.getRole(), "The role of the object must be 'Seller'");
  }

  /**
   * Kiểm tra hàm khởi tạo (Constructor) không chứa ID.
   * Sử dụng Java Reflection để kiểm tra các trường bị đóng gói (email, password) không có Getter ở lớp cha.
   *
   * @throws Exception nếu có lỗi truy cập vùng nhớ qua Reflection
   */
  @Test
  void testConstructorWithoutId() throws Exception {
    // Kiểm tra các trường có sẵn Getter công khai
    assertEquals(-1, seller.getId(), "The default ID from Entity should be -1");
    assertEquals("seller01", seller.getName());
    assertEquals("Nguyen Van Seller", seller.getFullName());

    // Dùng Reflection để "soi" các trường private/protected của lớp cha (User)
    Field emailField = User.class.getDeclaredField("email");
    emailField.setAccessible(true);
    assertEquals("seller@vnu.edu.vn", emailField.get(seller), "Email must match constructor input");

    Field passwordField = User.class.getDeclaredField("password");
    passwordField.setAccessible(true);
    assertEquals("pass123", passwordField.get(seller), "Password must match constructor input");

    // Danh sách ownedItems ban đầu phải là null nếu chưa được cấu hình
    assertNull(seller.getOwnedItems(), "The owned items list should be null initially");
  }

  /**
   * Kiểm tra hàm khởi tạo có ID.
   * Đảm bảo truyền tham số chính xác xuống lớp cha.
   *
   * @throws Exception nếu có lỗi khi dùng Reflection
   */
  @Test
  void testConstructorWithId() throws Exception {
    Seller sellerWithId = new Seller(10, "proSeller", "pro123", "pro@vnu.edu.vn", "Pro Seller");

    assertEquals(10, sellerWithId.getId());
    assertEquals("proSeller", sellerWithId.getName());

    // Dùng Reflection soi trường email bị đóng gói
    Field emailField = User.class.getDeclaredField("email");
    emailField.setAccessible(true);
    assertEquals("pro@vnu.edu.vn", emailField.get(sellerWithId));
  }

  /**
   * Kiểm tra chức năng gán (Setter) và lấy (Getter) danh sách sản phẩm sở hữu (ownedItems).
   * Sử dụng Mockito để tạo danh sách Item giả lập.
   */
  @Test
  void testSetAndGetOwnedItems() {
    // Tạo các mock object cho Item thay vì dùng dữ liệu thật
    Item mockItem1 = Mockito.mock(Item.class);
    Item mockItem2 = Mockito.mock(Item.class);

    List<Item> mockItemList = Arrays.asList(mockItem1, mockItem2);

    // Gán danh sách cho đối tượng seller
    seller.setOwnedItems(mockItemList);

    // Lấy danh sách ra và kiểm tra tính toàn vẹn
    List<Item> retrievedItems = seller.getOwnedItems();

    assertNotNull(retrievedItems, "The retrieved list should not be null after being set");
    assertEquals(2, retrievedItems.size(), "The list should contain exactly 2 items");
    assertTrue(retrievedItems.contains(mockItem1), "The list must contain the first mock item");
    assertTrue(retrievedItems.contains(mockItem2), "The list must contain the second mock item");
  }
}