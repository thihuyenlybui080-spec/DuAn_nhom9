package vn.edu.vnu.auction.model.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lớp kiểm thử (Unit Test) dành cho Entity {@link Seller}.
 * <p>
 * Đảm bảo các chức năng khởi tạo, kế thừa thuộc tính từ lớp cha {@link User},
 * và phân quyền vai trò (role) hoạt động chính xác.
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
   * Đảm bảo các trường kế thừa từ lớp User được gán giá trị chính xác.
   */
  @Test
  void testConstructorWithoutId() {
    assertEquals(-1, seller.getId(), "The default ID from Entity should be -1");
    assertEquals("seller01", seller.getName());
    assertEquals("pass123", seller.getPassword());
    assertEquals("seller@vnu.edu.vn", seller.getEmail());
    assertEquals("Nguyen Van Seller", seller.getFullName());
  }

  /**
   * Kiểm tra hàm khởi tạo có tham số ID.
   * Đảm bảo hệ thống ghi nhận đúng định danh ID và các thông tin khác.
   */
  @Test
  void testConstructorWithId() {
    Seller sellerWithId = new Seller(10, "proSeller", "pro123", "pro@vnu.edu.vn", "Pro Seller");

    assertEquals(10, sellerWithId.getId(), "The ID should match the value passed in the constructor");
    assertEquals("proSeller", sellerWithId.getName());
    assertEquals("pro@vnu.edu.vn", sellerWithId.getEmail());
  }
}