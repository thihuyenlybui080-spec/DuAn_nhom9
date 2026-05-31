package vn.edu.vnu.auction.model.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;

/**
 * Lớp kiểm thử (Unit Test) dành cho Entity {@link Admin}.
 * <p>
 * Đảm bảo các chức năng khởi tạo đối tượng, kế thừa thuộc tính từ lớp cha {@link User}
 * và vai trò (role) của quản trị viên hoạt động chính xác mà không vi phạm tính đóng gói.
 * </p>
 */
class AdminTest {

  private Admin admin;

  /**
   * Phương thức thiết lập dữ liệu (Setup) được chạy trước mỗi kịch bản test.
   * Khởi tạo một đối tượng Admin mẫu để đảm bảo tính cô lập giữa các bài test.
   */
  @BeforeEach
  void setUp() {
    admin = new Admin("adminRoot", "admin123", "admin@vnu.edu.vn", "Nguyen Van Admin");
  }

  /**
   * Kiểm tra phương thức lấy vai trò của người dùng.
   * Kỳ vọng: Chuỗi trả về bắt buộc phải là "Admin".
   */
  @Test
  void testGetRole() {
    assertEquals("Admin", admin.getRole(), "The role of the object must be 'Admin'");
  }

  /**
   * Kiểm tra hàm khởi tạo (Constructor) không chứa tham số ID.
   * Sử dụng Java Reflection để kiểm chứng các trường private/protected không có hàm Getter.
   * * @throws Exception nếu xảy ra lỗi trong quá trình dùng Reflection can thiệp vào bộ nhớ
   */
  @Test
  void testConstructorWithoutId() throws Exception {
    // Kiểm tra các trường được kế thừa có sẵn Getter
    assertEquals(-1, admin.getId(), "The default ID from Entity should be -1");
    assertEquals("adminRoot", admin.getName());
    assertEquals("Nguyen Van Admin", admin.getFullName());
    assertTrue(admin.isActive(), "The default initialized account must be in the Active state");

    // Dùng Reflection để "soi" các trường không có Getter (email, password)
    Field emailField = User.class.getDeclaredField("email");
    emailField.setAccessible(true);
    assertEquals("admin@vnu.edu.vn", emailField.get(admin), "Email must match constructor input");

    Field passwordField = User.class.getDeclaredField("password");
    passwordField.setAccessible(true);
    assertEquals("admin123", passwordField.get(admin), "Password must match constructor input");
  }

  /**
   * Kiểm tra hàm khởi tạo (Constructor) có chứa tham số ID.
   * Đảm bảo ID được truyền chính xác xuống lớp cha và các thuộc tính khác được gán đúng.
   * * @throws Exception nếu xảy ra lỗi trong quá trình dùng Reflection
   */
  @Test
  void testConstructorWithId() throws Exception {
    Admin adminWithId = new Admin(99, "superAdmin", "pass", "super@vnu.edu.vn", "Super Admin");

    assertEquals(99, adminWithId.getId());
    assertEquals("superAdmin", adminWithId.getName());

    // Dùng Reflection soi trường email bị đóng gói
    Field emailField = User.class.getDeclaredField("email");
    emailField.setAccessible(true);
    assertEquals("super@vnu.edu.vn", emailField.get(adminWithId));
  }
}