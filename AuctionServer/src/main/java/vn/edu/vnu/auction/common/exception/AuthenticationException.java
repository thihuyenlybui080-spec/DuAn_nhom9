package vn.edu.vnu.auction.common.exception;

/**
 * Ngoại lệ được ném khi xác thực thất bại.
 * <p>
 * Ngoại lệ này được ném khi người dùng không thể được xác thực do thông tin đăng nhập không hợp lệ
 * hoặc các vấn đề liên quan đến xác thực khác.
 * </p>
 */
public class AuthenticationException extends Exception {

  /**
   * Tạo một AuthenticationException mới với thông điệp chi tiết được chỉ định.
   *
   * @param msg thông điệp chi tiết giải thích lý do xác thực thất bại
   */
  public AuthenticationException(String msg) {
    super(msg);
  }
}

