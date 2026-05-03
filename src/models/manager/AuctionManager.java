package models.manager;

import base.Item;
import models.auction.Auction;
import models.auction.AuctionResult;
import models.auction.AuctionStatus;
import models.user.Bidder;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AuctionManager – Singleton quản lý tất cả phiên đấu giá đang hoạt động.
 * * - Anti-sniping (gia hạn + đặt lại timer) thực hiện bên trong lock của Auction
 * để tránh race condition giữa extend và endAuction.
 * - Singleton vẫn dùng double-checked locking với volatile (không đổi).
 * - endAuction() dùng ConcurrentHashMap.remove() – đủ thread-safe, không cần lock riêng.
 */
public class AuctionManager {

    // ===== SINGLETON =====
    private static volatile AuctionManager instance;

    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        scheduler      = Executors.newScheduledThreadPool(10);
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            synchronized (AuctionManager.class) {
                if (instance == null) {
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    // ===== FIELDS =====
    /** ConcurrentHashMap: đọc/xoá không cần lock ngoài. */
    private final Map<String, Auction> activeAuctions;
    private final ScheduledExecutorService scheduler;

    /** Thời gian còn lại dưới ngưỡng này sẽ kích hoạt anti-sniping (giây). */
    private static final long ANTI_SNIPE_THRESHOLD_SECONDS = 30;

    /** Thời gian gia hạn khi anti-sniping kích hoạt (giây). */
    private static final long ANTI_SNIPE_EXTENSION_SECONDS = 60;

    // ===== PUBLIC API =====

    public void startAuction(String auctionId, Item item) {
        long durationInSeconds = ChronoUnit.SECONDS.between(item.getStartTime(), item.getEndTime());

        if (durationInSeconds <= 0) {
            System.err.println("[AuctionManager] Invalid duration for: " + item.getItemName());
            return;
        }

        // putIfAbsent đảm bảo không tạo trùng phiên, không cần synchronized block
        Auction newAuction = new Auction(auctionId, item);
        newAuction.setStatus(AuctionStatus.RUNNING);

        if (activeAuctions.putIfAbsent(auctionId, newAuction) != null) {
            System.err.println("Auction session " + auctionId + " already exists!");
            return;
        }

        System.out.println("Opened auction session " + auctionId + " for " + durationInSeconds + " seconds.");
        scheduleEnd(newAuction, durationInSeconds);
    }

    /**
     * Đặt giá thầu.
     *
     * Thread-safety:
     * - Đọc auction từ ConcurrentHashMap (lock-free).
     * - Gọi auction.processBid() – bên trong có ReentrantLock riêng của Auction.
     * - Anti-sniping dùng auction.getLock() để extend + reschedule timer
     * trong cùng một critical section, tránh race với endAuction().
     */
    public boolean placeBid(String auctionId, Bidder user, double amount) {
        Auction auction = activeAuctions.get(auctionId);

        if (auction == null || !AuctionStatus.RUNNING.equals(auction.getStatus())) {
            System.err.println("Error: Auction session does not exist or has already ended!");
            return false;
        }

        boolean success = auction.processBid(user, amount);

        if (success) {
            tryAntiSnipe(auction);
        }

        return success;
    }

    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    public Auction getAuction(String auctionId) {
        return activeAuctions.get(auctionId);
    }

    public void removeAuction(String auctionId) {
        activeAuctions.remove(auctionId);
    }


    /**
     * Dừng scheduler khi application tắt để tránh thread leak.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ===== PRIVATE HELPERS =====

    /**
     * Anti-sniping: nếu còn ít hơn THRESHOLD giây, gia hạn và lên lịch lại.
     * Toàn bộ thực hiện bên trong lock của Auction để đảm bảo:
     * - Không race với endAuction() đang chạy.
     * - extend và setTimer là 1 atomic operation.
     */
    private void tryAntiSnipe(Auction auction) {
        boolean extended = auction.tryExtendForAntiSnipe(ANTI_SNIPE_THRESHOLD_SECONDS, ANTI_SNIPE_EXTENSION_SECONDS);
        if (extended) {
            scheduleEnd(auction, ANTI_SNIPE_EXTENSION_SECONDS);
        }
    }

    /** Lên lịch kết thúc auction (không giữ lock). */
    private void scheduleEnd(Auction auction, long delaySeconds) {
        ScheduledFuture<?> timer = scheduler.schedule(
                () -> endAuction(auction.getId()), delaySeconds, TimeUnit.SECONDS);
        auction.setTimer(timer);
    }

    /**
     * Lên lịch lại timer khi đã giữ lock của auction từ bên ngoài.
     * Dùng setTimer() nội bộ (setTimer cũng acquire lock → phải dùng non-locking variant).
     * Ở đây ta gọi trực tiếp để tránh re-entrant deadlock nếu lock không phải reentrant.
     * Vì ReentrantLock cho phép re-entrant, gọi setTimer() vẫn an toàn.
     */
    private void scheduleEndLocked(Auction auction, long delaySeconds) {
        ScheduledFuture<?> timer = scheduler.schedule(
                () -> endAuction(auction.getId()), delaySeconds, TimeUnit.SECONDS);
        // setTimer() sẽ acquire lại lock – OK vì ReentrantLock là reentrant
        auction.setTimer(timer);
    }

    /** Kết thúc phiên đấu giá, lưu kết quả, xoá khỏi map. */
    private void endAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

        auction.finishAuction(); // thread-safe bên trong

        AuctionResult result = new AuctionResult(
                auction.getId(),
                auction.getItem(),
                auction.getHighestBidder(),
                auction.getCurrentPrice(),
                auction.getBids()
        );
        AuctionHistoryManager.getInstance().saveResult(result);

        activeAuctions.remove(auctionId);
    }
}