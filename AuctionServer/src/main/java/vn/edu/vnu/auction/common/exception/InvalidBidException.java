package vn.edu.vnu.auction.common.exception;

/**
 * Ngoại lệ được ném khi một giá đặt không hợp lệ.
 * <p>
 * Ngoại lệ này được ném khi giá đặt không đáp ứng các tiêu chí yêu cầu, như thấp hơn mức giá tối
 * thiểu hoặc thấp hơn giá cao nhất hiện tại.
 * </p>
 */
public class InvalidBidException extends Exception {

  /**
   * Tạo một InvalidBidException mới với thông điệp chi tiết được chỉ định.
   *
   * @param message thông điệp chi tiết giải thích lý do giá đặt không hợp lệ
   */
  public InvalidBidException(String message) {
    super(message);
  }
}