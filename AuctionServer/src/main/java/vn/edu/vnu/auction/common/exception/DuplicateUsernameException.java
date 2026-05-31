package vn.edu.vnu.auction.common.exception;

/**
 * Ngoại lệ được ném khi cố gắng đăng ký người dùng với tên người dùng đã tồn tại.
 * <p>
 * Ngoại lệ này được ném trong quá trình đăng ký người dùng khi tên người dùng được cung cấp đã được
 * người dùng khác trong hệ thống sử dụng.
 * </p>
 */
public class DuplicateUsernameException extends Exception {

  /**
   * Tạo một DuplicateUsernameException mới với thông điệp chi tiết được chỉ định.
   *
   * @param message thông điệp chi tiết giải thích vấn đề trùng lặp tên người dùng
   */
  public DuplicateUsernameException(String message) {
    super(message);
  }
}
