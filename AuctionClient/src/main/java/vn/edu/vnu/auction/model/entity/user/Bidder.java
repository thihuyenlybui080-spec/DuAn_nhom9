package vn.edu.vnu.auction.model.entity.user;

/**
 * Đại diện cho người tham gia đấu giá (Bidder).
 * <p>
 * Lớp này mở rộng từ User và cung cấp các chức năng cụ thể cho người tham gia đấu giá, bao gồm khả
 * năng đặt giá thầu và tham gia vào các phiên đấu giá.
 * </p>
 */
public class Bidder extends User {

  private static final long serialVersionUID = 1L;

  /**
   * Khởi tạo một Bidder mới mà không có ID.
   *
   * @param name     tên đăng nhập của người tham gia
   * @param password mật khẩu của người tham gia
   * @param email    địa chỉ email của người tham gia
   * @param fullName họ và tên đầy đủ của người tham gia
   */
  public Bidder(String name, String password, String email, String fullName) {
    super(name, password, email, fullName);
  }

  /**
   * Khởi tạo một Bidder mới với ID đã cho.
   *
   * @param id       ID của người tham gia
   * @param name     tên đăng nhập của người tham gia
   * @param password mật khẩu của người tham gia
   * @param email    địa chỉ email của người tham gia
   * @param fullName họ và tên đầy đủ của người tham gia
   */
  public Bidder(int id, String name, String password, String email, String fullName) {
    super(id, name, password, email, fullName);
  }

  /**
   * Lấy vai trò của người dùng.
   *
   * @return chuỗi "Bidder" đại diện cho vai trò người tham gia đấu giá
   */
  @Override
  public String getRole() {
    return "Bidder";
  }
}
