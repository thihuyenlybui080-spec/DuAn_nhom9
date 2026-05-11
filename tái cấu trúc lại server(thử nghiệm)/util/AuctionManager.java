package org.example.loginregister.server.util;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionResult;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AuctionManager – Singleton điều phối vòng đời của các phiên đấu giá.
 *
 * Trách nhiệm sau refactor:
 *  1. Mở / lên lịch phiên (startAuction, openAuction).
 *  2. Nhận yêu cầu bid: lookup auction -> uỷ thác toàn bộ xử lý cho BidProcessor.
 *  3. Kết thúc phiên (finishAuction) – đã chuyển từ Auction sang đây.
 *  4. Huỷ phiên (cancelAuction).
 *  5. Lên lịch deadline thanh toán.
 *
 * Luồng bid đầy đủ:
 *  Bidder.bid()
 *    -> AuctionManager.placeBid()          [chỉ lookup auction]
 *        -> BidProcessor.process()         [validate bidder + auction + amount]
 *            -> applyBid()                 [apply trong lock, notify ngoài lock]
 *            -> bidder.recordBid()         [ghi lịch sử]
 *            -> AntiSnipeHandler           [extend + reschedule nếu snipe]
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
    private final Map<String, Auction> activeAuctions;
    private final ScheduledExecutorService scheduler;

    // Hạn để bidder thanh toán sau khi thắng
    private static final long PAYMENT_DEADLINE_SECONDS = 24 * 60 * 60; // 24 giờ


    // ===== PUBLIC API =====

    /** Lên lịch mở phiên đấu giá dựa theo startTime của Item. */
    public void startAuction(String auctionId, Seller seller, Item item) {
        LocalDateTime now      = LocalDateTime.now();
        long startDelay = ChronoUnit.SECONDS.between(now, item.getStartTime());
        long endDelay   = ChronoUnit.SECONDS.between(now, item.getEndTime());

        if (item.getEndTime().isBefore(item.getStartTime())) {
            System.err.println("[AuctionManager] End time must be after start time!");
            return;
        }
        if (endDelay <= 0) {
            System.err.println("[AuctionManager] Auction end time is in the past!");
            return;
        }

        Auction auction = new Auction(seller, item);

        if (startDelay <= 0) {
            openAuction(auction);
        } else {
            scheduler.schedule(() -> openAuction(auction), startDelay, TimeUnit.SECONDS);
            System.out.println("[AuctionManager] Auction " + auctionId + " scheduled to open in " + startDelay + "s");
        }
    }

    /**
     * Nhận yêu cầu đặt giá: lookup auction rồi uỷ thác toàn bộ xử lý
     * cho BidProcessor. Không chứa bất kỳ logic bid nào ở đây.
     */
    public boolean placeBid(String auctionId, Bidder bidder, double amount) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            System.err.println("[AuctionManager] Auction '" + auctionId+ "' does not exist or has already ended.");
            return false;
        }

        return BidProcessor.getInstance().process(auction, bidder, amount,scheduler,() -> endAuction(auction.getId()));
    }

    /** Huỷ phiên theo yêu cầu của Seller / Admin. */
    public void cancelAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

        auction.setStatus(AuctionStatus.CANCELED);

        AuctionResult result = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(result);

        activeAuctions.remove(auctionId);
        // Timer tự vô hiệu vì endAuction() sẽ tìm không thấy auction trong map
    }

    /** Xoá phiên khỏi map (dùng khi cần remove thủ công). */
    public void removeAuction(String auctionId) {
        activeAuctions.remove(auctionId);
    }

    /** Dừng scheduler khi application tắt để tránh thread leak. */
    public void shutdown() {
        scheduler.shutdownNow();
    }


    // ===== FINISH AUCTION  =====

    /**
     * Kết thúc phiên đấu giá: cập nhật status, lưu kết quả, xoá khỏi map.
     * Logic này trước đây nằm trong Auction.finishAuction(), nay được
     * chuyển vào AuctionManager để Auction chỉ giữ vai trò data holder.
     */
    public void finishAuction(String auctionId, AuctionStatus finalStatus) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

        ReentrantLock lock = auction.getLock();
        lock.lock();
        try {
            auction.applyFinalStatus(finalStatus);
        } finally {
            lock.unlock();
        }

        // Notify ngoài lock
        auction.notifyObservers();

        System.out.println("[AuctionManager] === AUCTION ENDED: " + finalStatus + " ===");
        System.out.println("[AuctionManager] Winner: "+ (auction.getHighestBidder() != null ? auction.getHighestBidder().getName() : "None"));

        AuctionResult result = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(result);

        activeAuctions.remove(auctionId);

        if (finalStatus == AuctionStatus.FINISHED) {
            schedulePaymentDeadline(auctionId);
        }
    }


    // ===== PRIVATE HELPERS =====

    private void openAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
        System.out.println("[AuctionManager] Auction '" + auction.getId() + "' is now OPEN.");
        auction.notifyObservers();

        long endDelay = ChronoUnit.SECONDS.between(
                LocalDateTime.now(), auction.getItem().getEndTime());
        scheduleEnd(auction, endDelay);
    }

    /** Lên lịch kết thúc phiên (không giữ lock). */
    private void scheduleEnd(Auction auction, long delaySeconds) {
        ScheduledFuture<?> timer = scheduler.schedule(() -> endAuction(auction.getId()), delaySeconds, TimeUnit.SECONDS);
        // setTimer tự acquire lock bên trong
        ReentrantLock lock = auction.getLock();
        lock.lock();
        try {
            auction.setTimer(timer);
        } finally {
            lock.unlock();
        }
    }

    /** Kết thúc phiên theo lịch (được gọi bởi scheduler). */
    private void endAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

        AuctionStatus finalStatus = (auction.getHighestBidder() != null) ? AuctionStatus.FINISHED : AuctionStatus.CANCELED;

        finishAuction(auctionId, finalStatus);
    }

    private void schedulePaymentDeadline(String auctionId) {
        AuctionHistoryManager ahm = AuctionHistoryManager.getInstance();
        AuctionResult result = ahm.getResult(auctionId);
        if (result == null) return;

        scheduler.schedule(() -> {
            AuctionResult r = ahm.getResult(auctionId);
            if (r != null && r.getStatus() == AuctionStatus.FINISHED) {
                ahm.updateStatus(auctionId, AuctionStatus.CANCELED);
            }
        }, PAYMENT_DEADLINE_SECONDS, TimeUnit.SECONDS);

        System.out.println("[AuctionManager][PaymentDeadline] Auction '" + auctionId + "' – winner has 24h to complete payment.");
    }


    // ===== GETTERS =====

    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    public Auction getAuction(String auctionId) {
        return activeAuctions.get(auctionId);
    }
}
