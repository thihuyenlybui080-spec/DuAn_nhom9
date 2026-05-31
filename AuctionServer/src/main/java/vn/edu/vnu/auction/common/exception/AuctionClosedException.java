package vn.edu.vnu.auction.common.exception;

/**
 * Ngoại lệ được ném khi cố gắng thực hiện thao tác trên phiên đấu giá đã đóng.
 * <p>
 * Ngoại lệ này được ném khi cố gắng đặt giá hoặc thực hiện các hành động khác trên phiên đấu giá đã
 * kết thúc hoặc bị đóng.
 * </p>
 */
public class AuctionClosedException extends Exception {

  /**
   * Tạo một AuctionClosedException mới với thông điệp chi tiết được chỉ định.
   *
   * @param message thông điệp chi tiết giải thích việc đóng phiên đấu giá
   */
  public AuctionClosedException(String message) {
    super(message);
  }
}
