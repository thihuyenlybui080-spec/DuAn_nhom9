package vn.edu.vnu.auction.model.entity.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.model.entity.item.Item;

/**
 * Lớp kiểm thử (Unit Test) dành cho Entity {@link Bidder}.
 * <p>
 * Đảm bảo các chức năng khởi tạo, kế thừa từ lớp cha {@link User}, phân quyền vai trò (role),
 * và logic ghi nhận lịch sử đặt giá (recordBid) hoạt động chính xác.
 * </p>
 */
class BidderTest {

  private Bidder bidder;
  private Item mockItem;

  /**
   * Thiết lập dữ liệu trước mỗi kịch bản kiểm thử.
   * Khởi tạo đối tượng Bidder mẫu và Mock đối tượng Item để phục vụ cho hàm đặt giá.
   */
  @BeforeEach
  void setUp() {
    bidder = new Bidder("bidder01", "pass123", "bidder@vnu.edu.vn", "Nguyen Van Bidder");

    // Tạo đối tượng giả lập (Mock) cho Item
    mockItem = Mockito.mock(Item.class);
    Mockito.when(mockItem.getItemName()).thenReturn("Antique Vase");
  }

  /**
   * Kiểm tra phương thức lấy vai trò của người dùng.
   * Kỳ vọng: Chuỗi trả về bắt buộc phải là "Bidder".
   */
  @Test
  void testGetRole() {
    assertEquals("Bidder", bidder.getRole(), "The role of the object must be 'Bidder'");
  }

  /**
   * Kiểm tra hàm khởi tạo (Constructor) không chứa ID.
   * Sử dụng Java Reflection để kiểm tra các trường bị đóng gói (email, password) không có Getter.
   *
   * @throws Exception nếu có lỗi truy cập vùng nhớ qua Reflection
   */
  @Test
  void testConstructorWithoutId() throws Exception {
    // Kiểm tra các trường có sẵn Getter công khai
    assertEquals(-1, bidder.getId(), "The default ID from Entity should be -1");
    assertEquals("bidder01", bidder.getName());
    assertEquals("Nguyen Van Bidder", bidder.getFullName());

    // Dùng Reflection để "soi" các trường private/protected của lớp cha (User)
    Field emailField = User.class.getDeclaredField("email");
    emailField.setAccessible(true);
    assertEquals("bidder@vnu.edu.vn", emailField.get(bidder), "Email must match constructor input");

    Field passwordField = User.class.getDeclaredField("password");
    passwordField.setAccessible(true);
    assertEquals("pass123", passwordField.get(bidder), "Password must match constructor input");
  }

  /**
   * Kiểm tra hàm khởi tạo có tham số ID.
   *
   * @throws Exception nếu có lỗi khi dùng Reflection
   */
  @Test
  void testConstructorWithId() throws Exception {
    Bidder bidderWithId = new Bidder(5, "vipBidder", "vip123", "vip@vnu.edu.vn", "Vip User");

    assertEquals(5, bidderWithId.getId(), "The ID should match the value passed in the constructor");
    assertEquals("vipBidder", bidderWithId.getName());

    // Dùng Reflection soi trường email bị đóng gói
    Field emailField = User.class.getDeclaredField("email");
    emailField.setAccessible(true);
    assertEquals("vip@vnu.edu.vn", emailField.get(bidderWithId));
  }

  /**
   * Kiểm tra logic ghi nhận đặt giá (recordBid) khi tài khoản đang hoạt động bình thường.
   * Kỳ vọng: Không ném ra bất kỳ ngoại lệ nào và dữ liệu được ghi vào lịch sử.
   */
  @Test
  void testRecordBid_Success() {
    assertDoesNotThrow(() -> bidder.recordBid(mockItem, 500.0),
        "Active bidder should be able to place a bid without throwing exceptions");
  }

  /**
   * Kiểm tra logic ghi nhận đặt giá (recordBid) khi tài khoản bị khóa (Banned/Inactive).
   * Kỳ vọng: Hệ thống phải chặn lại và ném ra ngoại lệ IllegalStateException.
   */
  @Test
  void testRecordBid_Fail_AccountLocked() {
    // Giả lập trạng thái tài khoản bị vô hiệu hóa
    UserStatus mockStatus = Mockito.mock(UserStatus.class);
    Mockito.when(mockStatus.isActive()).thenReturn(false);

    UserStatusRecord mockRecord = Mockito.mock(UserStatusRecord.class);
    Mockito.when(mockRecord.status()).thenReturn(mockStatus);

    // Cập nhật trạng thái cho bidder
    bidder.updateStatus(mockRecord);

    // Kiểm tra xem ngoại lệ có được ném ra đúng như thiết kế không
    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> bidder.recordBid(mockItem, 500.0));

    assertEquals("Account is locked and cannot place bids", exception.getMessage());
  }
}