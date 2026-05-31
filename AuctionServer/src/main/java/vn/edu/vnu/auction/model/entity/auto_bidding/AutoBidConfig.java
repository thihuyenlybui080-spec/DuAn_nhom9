package vn.edu.vnu.auction.model.entity.auto_bidding;

import java.io.Serial;
import java.io.Serializable;

/**
 * Lưu cấu hình auto-bid mà một Bidder đăng ký cho một phiên đấu giá. Immutable sau khi tạo (chỉ đọc
 * từ bên ngoài).
 */
public record AutoBidConfig(double maxBid, double increment) implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public AutoBidConfig {
    if (maxBid <= 0) {
      throw new IllegalArgumentException("maxBid must be > 0");
    }
    if (increment <= 0) {
      throw new IllegalArgumentException("increment must be > 0");
    }
  }

  @Override
  public String toString() {
    return "AutoBidConfig{maxBid=" + maxBid + ", increment=" + increment + "}";
  }
}