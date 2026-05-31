package vn.edu.vnu.auction.common.network;

import java.io.Serializable;

/**
 * Đại diện cho thông báo được gửi từ server đến clients.
 * <p>
 * Lớp này được sử dụng để phát sóng cập nhật thời gian thực về các sự kiện đấu giá như cập nhật
 * giá, thay đổi trạng thái đấu giá, và gia hạn thời gian.
 * </p>
 */
public class NotificationMessage implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Loại thông báo khi giá được cập nhật
   */
  public static final String TYPE_BID_UPDATED = "BID_UPDATED";
  /**
   * Loại thông báo khi phiên đấu giá bắt đầu
   */
  public static final String TYPE_AUCTION_STARTED = "AUCTION_STARTED";
  /**
   * Loại thông báo khi phiên đấu giá kết thúc
   */
  public static final String TYPE_AUCTION_ENDED = "AUCTION_ENDED";
  /**
   * Loại thông báo khi thời gian đấu giá được gia hạn (chống snipe)
   */
  public static final String TYPE_TIME_EXTENDED = "TIME_EXTENDED";
  /**
   * Loại thông báo khi phiên đấu giá kết thúc do auto-bid
   */
  public static final String TYPE_AUTO_BID_AUCTION_ENDED = "AUTO_BID_AUCTION_ENDED";
  /**
   * Loại thông báo khi người dùng bị khóa
   */
  public static final String TYPE_USER_LOCKED = "USER_LOCKED";
  /**
   * Loại thông báo khi người dùng được mở khóa
   */
  public static final String TYPE_USER_UNLOCKED = "USER_UNLOCKED";

  private final String type;
  private final int auctionId;

  private final Object data;

  /**
   * Tạo một NotificationMessage mới.
   *
   * @param type      loại thông báo
   * @param auctionId ID của phiên đấu giá liên quan đến thông báo này
   * @param data      dữ liệu liên quan đến thông báo này
   */
  public NotificationMessage(String type, int auctionId, Object data) {
    this.type = type;
    this.auctionId = auctionId;
    this.data = data;
  }

  /**
   * Lấy loại thông báo.
   *
   * @return loại thông báo
   */
  public String getType() {
    return type;
  }

  /**
   * Lấy ID phiên đấu giá.
   *
   * @return ID phiên đấu giá
   */
  public int getAuctionId() {
    return auctionId;
  }

  /**
   * Lấy dữ liệu thông báo.
   *
   * @return dữ liệu thông báo
   */
  public Object getData() {
    return data;
  }

  @Override
  public String toString() {
    return "Notification{type='" + type + "',auctionId='" + auctionId + "'}";
  }
}
