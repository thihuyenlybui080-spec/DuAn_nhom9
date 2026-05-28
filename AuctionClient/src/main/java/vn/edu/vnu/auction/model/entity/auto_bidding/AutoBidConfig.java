package vn.edu.vnu.auction.model.entity.auto_bidding;

import java.io.Serializable;

/**
 * Lưu cấu hình auto-bid mà một Bidder đăng ký cho một phiên đấu giá.
 * Immutable sau khi tạo (chỉ đọc từ bên ngoài).
 */
public class AutoBidConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    private final double maxBid;
    private final double increment;

    public AutoBidConfig(double maxBid, double increment) {
        if (maxBid <= 0)      throw new IllegalArgumentException("maxBid must be > 0");
        if (increment <= 0)   throw new IllegalArgumentException("increment must be > 0");
        this.maxBid       = maxBid;
        this.increment    = increment;
    }

    @Override
    public String toString() {
        return "AutoBidConfig{maxBid=" + maxBid + ", increment=" + increment + "}";
    }
}