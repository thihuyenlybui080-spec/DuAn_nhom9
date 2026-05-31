package vn.edu.vnu.auction.model.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Lớp kiểm thử (Unit Test) dành cho Record {@link UserStatusRecord}.
 * <p>
 * Đảm bảo tính năng khởi tạo và các hàm truy xuất dữ liệu tự động của Java Record
 * hoạt động chính xác.
 * </p>
 */
class UserStatusRecordTest {

  /**
   * Kiểm tra hàm khởi tạo của Record và các hàm truy xuất (accessor).
   * Lưu ý: Đối với Java Record, các hàm getter không có tiền tố "get".
   */
  @Test
  void testConstructorAndGetters() {
    // Bước 1: Tạo một đối tượng giả lập (mock) Admin
    // Vì chỉ test UserStatusRecord nên dùng mock Admin là tối ưu nhất
    Admin mockAdmin = Mockito.mock(Admin.class);

    // Bước 2: Khởi tạo UserStatusRecord với trạng thái BANNED và truyền vào mock Admin
    UserStatusRecord record = new UserStatusRecord(UserStatus.BANNED, mockAdmin);

    // Bước 3: Xác minh dữ liệu trả về chính xác thông qua các hàm tự sinh của Record
    assertEquals(UserStatus.BANNED, record.status(), "The status should be BANNED");
    assertEquals(mockAdmin, record.changedBy(),
        "The changedBy field should match the injected mock Admin");
  }

  /**
   * Kiểm tra hàm khởi tạo tĩnh (static factory) cấu hình trạng thái mặc định.
   */
  @Test
  void testDefaultActive() {
    // Bước 1: Gọi hàm static factory tạo trạng thái Active
    UserStatusRecord defaultRecord = UserStatusRecord.defaultActive();

    // Bước 2: Xác minh các giá trị mặc định được gán đúng như logic class
    assertEquals(UserStatus.ACTIVE, defaultRecord.status(), "The default status must be ACTIVE");
    assertNull(defaultRecord.changedBy(), "The default changedBy admin must be null initially");
  }
}