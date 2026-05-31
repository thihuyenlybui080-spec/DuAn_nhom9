package vn.edu.vnu.auction.model.entity;

/**
 * Enum đại diện cho các trạng thái có thể của phiên đấu giá.
 */
public enum AuctionStatus {
  /**
   * Phiên đấu giá đang mở để đặt giá nhưng chưa bắt đầu
   */
  OPEN,
  /**
   * Phiên đấu giá đang hoạt động và chấp nhận đặt giá
   */
  RUNNING,
  /**
   * Phiên đấu giá đã kết thúc bình thường với người thắng
   */
  FINISHED,
  /**
   * Phiên đấu giá bị hủy bởi người bán hoặc admin
   */
  CANCELED,
  /**
   * Phiên đấu giá đã được thanh toán bởi người thắng
   */
  PAID
}