package org.example.loginregister.server.model.entity.auto_bidding;

import java.time.LocalDateTime;

/**
 * Lưu cấu hình auto-bid mà một Bidder đăng ký cho một phiên đấu giá.
 * Immutable sau khi tạo (chỉ đọc từ bên ngoài).
 */
public class AutoBidConfig {

    private final double maxBid;          // Giá tối đa sẵn sàng trả
    private final double increment;       // Bước giá mỗi lần tự động đặt
    private final LocalDateTime registeredAt; // Thời điểm đăng ký (dùng để ưu tiên khi tie)

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