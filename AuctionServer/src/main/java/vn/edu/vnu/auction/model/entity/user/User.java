package vn.edu.vnu.auction.model.entity.user;


import java.io.Serial;
import vn.edu.vnu.auction.common.exception.AuthenticationException;
import vn.edu.vnu.auction.model.entity.Entity;

/**
 * Lớp cơ sở trừu tượng cho tất cả các loại người dùng trong hệ thống.
 * <p>
 * Đại diện cho người dùng với thông tin đăng nhập, trạng thái, và vai trò. Các lớp con định nghĩa
 * các vai trò người dùng cụ thể như Admin, Bidder, và Seller.
 * </p>
 */
public abstract class User extends Entity {

  @Serial
  private static final long serialVersionUID = 1L;
  protected String userName;
  private String email;
  protected String password;
  private String fullName;
  private UserStatusRecord statusRecord = UserStatusRecord.defaultActive();

  /**
   * Tạo một User mới không có ID.
   *
   * @param userName tên người dùng
   * @param password mật khẩu
   * @param email    địa chỉ email
   * @param fullName họ tên đầy đủ
   */
  public User(String userName, String password, String email, String fullName) {
    super();
    this.userName = userName;
    this.password = password;
    this.email = email;
    this.fullName = fullName;
  }

  /**
   * Tạo một User mới với ID cụ thể.
   *
   * @param id       ID người dùng
   * @param userName tên người dùng
   * @param password mật khẩu
   * @param email    địa chỉ email
   * @param fullName họ tên đầy đủ
   */
  public User(int id, String userName, String password, String email, String fullName) {
    super(id);
    this.userName = userName;
    this.password = password;
    this.email = email;
    this.fullName = fullName;
  }

  /**
   * Cập nhật bản ghi trạng thái người dùng.
   *
   * @param newRecord bản ghi trạng thái mới
   */
  public final void updateStatus(UserStatusRecord newRecord) {
    this.statusRecord = newRecord;
  }


  /**
   * Xác thực người dùng với thông tin đăng nhập được cung cấp.
   *
   * @param name     tên người dùng cần kiểm tra
   * @param password mật khẩu cần kiểm tra
   * @throws AuthenticationException nếu xác thực thất bại
   */
  public void logIn(String name, String password) throws AuthenticationException {

    if (!statusRecord.status().isActive()) {
      throw new AuthenticationException("Account is banned");
    }
    if (!this.userName.equals(name) || !this.password.equals(password)) {
      throw new AuthenticationException("Invalid username or password");
    }
  }

  /**
   * Lấy trạng thái người dùng.
   *
   * @return trạng thái
   */
  public UserStatus getStatus() {
    return statusRecord.status();
  }

  /**
   * Kiểm tra xem người dùng có hoạt động không.
   *
   * @return true nếu hoạt động, false nếu không
   */
  public boolean isActive() {
    return statusRecord.status().isActive();
  }

  /**
   * Lấy tên người dùng.
   *
   * @return tên người dùng
   */
  public String getName() {
    return userName;
  }

  /**
   * Lấy địa chỉ email.
   *
   * @return email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Lấy mật khẩu.
   *
   * @return mật khẩu
   */
  public String getPassword() {
    return password;
  }

  /**
   * Lấy họ tên đầy đủ.
   *
   * @return họ tên đầy đủ
   */
  public String getFullName() {
    return fullName;
  }

  /**
   * Đặt tên người dùng.
   *
   * @param name tên người dùng mới
   */
  public void setName(String name) {
    this.userName = name;
  }

  /**
   * Đặt địa chỉ email.
   *
   * @param email email mới
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * Đặt mật khẩu.
   *
   * @param password mật khẩu mới
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Đặt họ tên đầy đủ.
   *
   * @param fullName họ tên đầy đủ mới
   */
  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  /**
   * Lấy vai trò người dùng.
   *
   * @return chuỗi vai trò
   */
  public abstract String getRole();
}
