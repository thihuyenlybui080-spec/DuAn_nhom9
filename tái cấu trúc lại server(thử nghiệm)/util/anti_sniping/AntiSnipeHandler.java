package org.example.loginregister.server.util.anti_sniping;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionStatus;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AntiSnipeHandler – Chịu trách nhiệm duy nhất: phát hiện và xử lý snipe.
 *
 * "Sniping" xảy ra khi bidder đặt giá trong những giây cuối của phiên,
 * khiến các bidder khác không còn thời gian phản ứng.
 * Giải pháp: nếu bid được chấp nhận khi thời gian còn lại < THRESHOLD,
 * tự động gia hạn phiên thêm EXTENSION giây và lên lịch lại timer kết thúc.
 *
 * Trách nhiệm:
 *  1. Kiểm tra xem bid vừa được chấp nhận có kích hoạt anti-snipe không.
 *  2. Nếu có: extend endTime của auction và reschedule timer kết thúc.
 *
 * KHÔNG chịu trách nhiệm:
 *  - Validate / apply bid  → BidProcessor
 *  - Kết thúc phiên / lưu kết quả → AuctionManager
 */
public class AntiSnipeHandler {

    // ===== CONSTANTS =====

    /** Nếu thời gian còn lại dưới ngưỡng này (giây) sẽ kích hoạt anti-snipe. */
    private static final long THRESHOLD_SECONDS  = 30;

    /** Thời gian gia hạn khi anti-snipe kích hoạt (giây). */
    private static final long EXTENSION_SECONDS  = 60;


    // ===== SINGLETON =====
    private static volatile AntiSnipeHandler instance;

    private AntiSnipeHandler() {}

    public static AntiSnipeHandler getInstance() {
        if (instance == null) {
            synchronized (AntiSnipeHandler.class) {
                if (instance == null) {
                    instance = new AntiSnipeHandler();
                }
            }
        }
        return instance;
    }


    // ===== PUBLIC API =====

    /**
     * Kiểm tra và xử lý anti-snipe sau khi một bid được chấp nhận.
     *
     * Toàn bộ thao tác extend + reschedule nằm trong lock của Auction để
     * đảm bảo atomic: không thể xảy ra race giữa "extend endTime" và
     * "endAuction() đang chạy" trên scheduler thread khác.
     *
     * @param auction   Phiên đấu giá vừa nhận bid thành công.
     * @param scheduler ScheduledExecutorService của AuctionManager để reschedule.
     * @param endTask   Runnable kết thúc phiên (endAuction) dùng để lên lịch lại.
     * @return true nếu anti-snipe kích hoạt và phiên được gia hạn, false nếu không.
     */
    public boolean handleAfterBid(Auction auction,ScheduledExecutorService scheduler,Runnable endTask) {
        if (auction == null) return false;

        ReentrantLock lock = auction.getLock();
        lock.lock();
        try {
            if (!isEligible(auction)) return false;

            // Gia hạn endTime của auction
            auction.extendEndTimeInternal(EXTENSION_SECONDS);

            // Lên lịch lại timer kết thúc phiên
            ScheduledFuture<?> newTimer = scheduler.schedule(
                    endTask, EXTENSION_SECONDS, TimeUnit.SECONDS);
            auction.setTimer(newTimer);

            System.out.println("[AntiSnipe] Auction '" + auction.getId() + "' extended by " + EXTENSION_SECONDS + "s (threshold=" + THRESHOLD_SECONDS + "s remaining).");
            return true;

        } finally {
            lock.unlock();
        }
    }


    // ===== PRIVATE HELPERS =====

    /**
     * Điều kiện kích hoạt anti-snipe:
     *  - Phiên đang OPEN hoặc RUNNING (không xử lý khi đã đóng).
     *  - Thời gian còn lại nhỏ hơn THRESHOLD_SECONDS.
     * @return true nếu bid đủ điều kiện kích hoạt anti-snipe, false nếu không.
     */
    private boolean isEligible(Auction auction) {
        AuctionStatus status = auction.getStatus();
        if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
            return false;
        }
        return auction.getSecondsRemaining() < THRESHOLD_SECONDS;
    }


    // ===== GETTERS (for testing / display) =====

    public long getThresholdSeconds()  { return THRESHOLD_SECONDS;  }
    public long getExtensionSeconds()  { return EXTENSION_SECONDS;  }
}