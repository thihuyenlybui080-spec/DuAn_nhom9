package vn.edu.vnu.auction.model.entity.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.edu.vnu.auction.common.exception.AuthenticationException;

/**
 * Lớp kiểm thử (Unit Test) dành cho lớp trừu tượng {@link User}.
 * <p>
 * Kiểm tra các tính năng cốt lõi của người dùng như khởi tạo, cập nhật thông tin,
 * và đặc biệt là logic xác thực đăng nhập (logIn).
 * </p>
 */
class UserTest {

  /**
   * Lớp con giả lập (Dummy) để kiểm thử abstract class User.
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

  @BeforeEach
  void setUp() {
    // Khởi tạo đối tượng DummyUser trước mỗi bài test
    user = new DummyUser("testUser", "password123", "test@vnu.edu.vn", "Test Full Name");
  }

  @Test
  void testConstructorWithoutId() {
    // Kiểm tra ID mặc định và các trường được gán đúng
    assertEquals(-1, user.getId(), "Default ID should be -1 from Entity");
    assertEquals("testUser", user.getName());
    assertEquals("password123", user.getPassword());
    assertEquals("test@vnu.edu.vn", user.getEmail());
    assertEquals("Test Full Name", user.getFullName());

    // Kiểm tra tài khoản được kích hoạt mặc định
    assertTrue(user.isActive(), "User must be active by default upon creation");
  }

  @Test
  void testConstructorWithId() {
    User userWithId = new DummyUser(5, "user5", "pass5", "user5@vnu.edu.vn", "User Five");
    assertEquals(5, userWithId.getId());
    assertEquals("user5", userWithId.getName());
  }

  @Test
  void testSettersAndGetters() {
    // Kiểm tra việc cập nhật thông tin người dùng
    user.setName("newName");
    user.setPassword("newPass");
    user.setEmail("new@vnu.edu.vn");
    user.setFullName("New Full Name");

    assertEquals("newName", user.getName());
    assertEquals("newPass", user.getPassword());
    assertEquals("new@vnu.edu.vn", user.getEmail());
    assertEquals("New Full Name", user.getFullName());
  }

  @Test
  void testLogIn_Success() {
    // Đăng nhập thành công sẽ không ném ra ngoại lệ
    assertDoesNotThrow(() -> user.logIn("testUser", "password123"),
        "Login should succeed with correct credentials");
  }

  @Test
  void testLogIn_Fail_WrongPassword() {
    // Đăng nhập sai mật khẩu
    AuthenticationException exception = assertThrows(AuthenticationException.class,
        () -> user.logIn("testUser", "wrongPass"));

    assertEquals("Invalid username or password", exception.getMessage());
  }

  @Test
  void testLogIn_Fail_WrongUsername() {
    // Đăng nhập sai tên tài khoản
    AuthenticationException exception = assertThrows(AuthenticationException.class,
        () -> user.logIn("wrongUser", "password123"));

    assertEquals("Invalid username or password", exception.getMessage());
  }

  @Test
  void testLogIn_Fail_AccountBanned() {
    // Bước 1: Mock trạng thái UserStatus bị vô hiệu hóa (Banned)
    UserStatus mockStatus = Mockito.mock(UserStatus.class);
    Mockito.when(mockStatus.isActive()).thenReturn(false);

    // Bước 2: Mock UserStatusRecord và trả về status vừa tạo (Sửa lỗi getStatus -> status)
    UserStatusRecord mockRecord = Mockito.mock(UserStatusRecord.class);
    Mockito.when(mockRecord.status()).thenReturn(mockStatus);

    // Bước 3: Áp dụng trạng thái cấm cho người dùng
    user.updateStatus(mockRecord);

    // Bước 4: Đăng nhập phải bị chặn lại với thông báo Account is banned
    AuthenticationException exception = assertThrows(AuthenticationException.class,
        () -> user.logIn("testUser", "password123"));

    assertEquals("Account is banned", exception.getMessage(),
        "An exception must be thrown if the account is not active");
  }
}