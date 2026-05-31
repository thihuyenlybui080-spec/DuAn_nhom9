package vn.edu.vnu.auction.common.observer;

/**
 * Interface Observer cho mẫu Observer.
 * <p>
 * Các lớp implement interface này có thể đăng ký với Subject để nhận thông báo về thay đổi trạng
 * thái đấu giá.
 * </p>
 */
public interface Observer {

  /**
   * Được gọi khi subject được quan sát thay đổi trạng thái.
   *
   * @param auctionId     ID của phiên đấu giá được cập nhật
   * @param newPrice      giá hiện tại mới của phiên đấu giá
   * @param highestBidder tên của người trả giá cao nhất hiện tại
   */
  public abstract void update(int auctionId, double newPrice, String highestBidder);
}

