package org.example.loginregister.server.model.entity.auto_bidding;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lưu cấu hình auto-bid mà một Bidder đăng ký cho một phiên đấu giá.
 * Immutable sau khi tạo (chỉ đọc từ bên ngoài).
 */
public class AutoBidConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    private final double maxBid;
    private final double increment;
    private final LocalDateTime registeredAt;

    public AutoBidConfig(double maxBid, double increment) {
        if (maxBid <= 0)      throw new IllegalArgumentException("maxBid must be > 0");
        if (increment <= 0)   throw new IllegalArgumentException("increment must be > 0");
        this.maxBid       = maxBid;
        this.increment    = increment;
        this.registeredAt = LocalDateTime.now();
    }

    public double getMaxBid()              { return maxBid; }
    public double getIncrement()           { return increment; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }

    @Override
    public String toString() {
        return "AutoBidConfig{maxBid=" + maxBid + ", increment=" + increment + ", registeredAt=" + registeredAt + "}";
    }
}