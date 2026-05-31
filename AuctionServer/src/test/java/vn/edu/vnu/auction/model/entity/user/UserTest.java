package vn.edu.vnu.auction.model.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.lang.reflect.Field;

/**
 * Lớp kiểm thử (Unit Test) dành cho lớp trừu tượng {@link User}.
 * <p>
 * Vì User là một abstract class, lớp kiểm thử này sử dụng một lớp con giả lập (DummyUser)
 * để khởi tạo và kiểm chứng các logic dùng chung cho mọi loại người dùng (khởi tạo, cập nhật trạng thái).
 * </p>
 */
class UserTest {

  /**
   * Lớp con giả lập (Dummy) kế thừa từ User nhằm mục đích phục vụ Unit Test.
   */
  private static class DummyUser extends User {

    public DummyUser(String userName, String password, String email, String fullName) {
      super(userName, password, email, fullName);
    }

    public DummyUser(int id, String userName, String password, String email, String fullName) {
      super(id, userName, password, email, fullName);
    }

    @Override
    public String getRole() {
      return "DummyRole";
    }
  }

  private User user;

  /**
   * Khởi tạo dữ liệu giả lập trước mỗi kịch bản kiểm thử.
   */
  @BeforeEach
  void setUp() {
    user = new DummyUser("testUser", "password123", "test@vnu.edu.vn", "Test Full Name");
  }

  /**
   * Kiểm tra hàm tạo mặc định (không ID) của User.
   * Xác minh ID mặc định (-1), trạng thái mặc định (Active) và sử dụng Reflection
   * để đọc các biến bị đóng gói bảo mật.
   * * @throws Exception nếu có lỗi truy cập vùng nhớ qua Reflection
   */
  @Test
  void testConstructorWithoutId() throws Exception {
    // Kiểm tra các hàm Getter công khai
    assertEquals(-1, user.getId());
    assertEquals("testUser", user.getName());
    assertEquals("Test Full Name", user.getFullName());
    assertTrue(user.isActive());

    // Dùng Reflection kiểm tra biến private/protected
    Field emailField = User.class.getDeclaredField("email");
    emailField.setAccessible(true);
    assertEquals("test@vnu.edu.vn", emailField.get(user));

    Field passwordField = User.class.getDeclaredField("password");
    passwordField.setAccessible(true);
    assertEquals("password123", passwordField.get(user));
  }

  /**
   * Kiểm tra hàm tạo có truyền tham số ID.
   * Đảm bảo hệ thống ghi nhận đúng định danh ID từ cơ sở dữ liệu.
   */
  @Test
  void testConstructorWithId() {
    User userWithId = new DummyUser(5, "user5", "pass5", "user5@vnu.edu.vn", "User Five");
    assertEquals(5, userWithId.getId());
    assertEquals("user5", userWithId.getName());
  }

  /**
   * Kiểm tra các hàm Setter và Getter công khai được phép sử dụng.
   */
  @Test
  void testSettersAndGetters() {
    // Vì class gốc chỉ cung cấp hàm setName(), ta tiến hành kiểm chứng hàm này
    user.setName("newName");
    assertEquals("newName", user.getName());
  }

  /**
   * Kiểm tra tính năng cập nhật trạng thái hoạt động của người dùng.
   * Sử dụng Mockito để giả lập một trạng thái bị cấm (Banned/Inactive) và kiểm chứng
   * xem hệ thống có ghi nhận sự thay đổi này không.
   */
  @Test
  void testUpdateStatus() {
    // Giả lập trạng thái Inactive (không hoạt động)
    UserStatus mockStatus = Mockito.mock(UserStatus.class);
    Mockito.when(mockStatus.isActive()).thenReturn(false);

    UserStatusRecord mockRecord = Mockito.mock(UserStatusRecord.class);
    Mockito.when(mockRecord.status()).thenReturn(mockStatus);

    // Cập nhật trạng thái cho user
    user.updateStatus(mockRecord);

    // Kiểm tra xem User đã bị vô hiệu hóa thành công chưa
    assertFalse(user.isActive(), "User should be inactive after status update");
  }
}