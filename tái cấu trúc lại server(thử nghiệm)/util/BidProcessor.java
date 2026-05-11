package org.example.loginregister.server.util;

import org.example.loginregister.common.exception.AuctionClosedException;
import org.example.loginregister.common.exception.InvalidBidException;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.util.anti_sniping.AntiSnipeHandler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

/**
 * BidProcessor – Chịu trách nhiệm toàn bộ luồng xử lý một lượt đặt giá,
 * từ lúc nhận yêu cầu đến khi hoàn tất (kể cả kiểm tra anti-snipe).
 *
 * Trách nhiệm:
 *  1. Validate bidder  (null? bị khóa?)
 *  2. Validate auction (null? đã đóng?)
 *  3. Validate amount  (> currentPrice?)
 *  4. Áp dụng bid vào auction (trong lock).
 *  5. Notify observers (ngoài lock).
 *  6. Ghi lịch sử giao dịch vào Bidder.
 *  7. Chuyển sang AntiSnipeHandler để kiểm tra và gia hạn nếu cần.
 *
 * AuctionManager.placeBid() chỉ làm đúng một việc:
 *   lookup auction từ map → gọi BidProcessor.process().
 * Mọi logic xử lý sau đó đều nằm ở đây.
 */
public class BidProcessor {

    // ===== SINGLETON =====
    private static volatile BidProcessor instance;

    private BidProcessor() {}

    public static BidProcessor getInstance() {
        if (instance == null) {
            synchronized (BidProcessor.class) {
                if (instance == null) {
                    instance = new BidProcessor();
                }
            }
        }
        return instance;
    }


    // ===== PUBLIC API =====

    /**
     * Xử lý toàn bộ một lượt đặt giá.
     *
     * Được gọi bởi AuctionManager.placeBid() sau khi auction đã được lookup.
     * Nhận thêm scheduler và endTask để uỷ thác cho AntiSnipeHandler,
     * tránh BidProcessor phải giữ tham chiếu đến AuctionManager (tránh
     * circular dependency).
     *
     * @param auction   Phiên đấu giá (đã được AuctionManager lookup, không null check lần nữa ở đây nếu chắc chắn, nhưng vẫn guard để an toàn).
     * @param bidder    Người đặt giá.
     * @param amount    Số tiền đặt giá.
     * @param scheduler Scheduler của AuctionManager, truyền sang AntiSnipeHandler.
     * @param endTask   Runnable kết thúc phiên, truyền sang AntiSnipeHandler.
     * @return true nếu bid được chấp nhận, false nếu bị từ chối.
     */
    public boolean process(Auction auction, Bidder bidder, double amount,ScheduledExecutorService scheduler, Runnable endTask) {

        // ── Bước 1: Validate bidder ──────────────────────────────────────────
        if (bidder == null) {
            System.err.println("[BidProcessor] Bid rejected: bidder is null.");
            return false;
        }
        if (!bidder.isActive()) {
            System.err.println("[BidProcessor] Bid rejected: account of '"
                    + bidder.getName() + "' is locked.");
            return false;
        }

        // ── Bước 2: Validate auction ─────────────────────────────────────────
        if (auction == null) {
            System.err.println("[BidProcessor] Bid rejected: auction is null.");
            return false;
        }

        // ── Bước 3 & 4: Validate amount + apply bid (trong lock) ─────────────
        BidTransaction bid = new BidTransaction(bidder, auction.getItem(), amount);
        boolean success = applyBid(auction, bid);
        if (!success) return false;

        // ── Bước 5: Notify observers (ngoài lock, đã gọi bên trong applyBid) ─

        // ── Bước 6: Ghi lịch sử giao dịch vào Bidder ────────────────────────
        bidder.recordBid(auction.getItem(), amount);

        // ── Bước 7: Kiểm tra & xử lý anti-snipe ───────────────────────────
        AntiSnipeHandler.getInstance().handleAfterBid(auction, scheduler, endTask);

        return true;
    }


    // ===== PRIVATE HELPERS =====

    /**
     * kiểm tra tính hợp lệ của bid
     * nếu hợp lệ sẽ thay đổi các trạng thái của auction
     * đồng thời add bid vào bids
     * notifyObservers() được gọi ngay sau khi unlock.
     */
    private boolean applyBid(Auction auction, BidTransaction bid) {
        ReentrantLock lock = auction.getLock();
        lock.lock();
        try {
            // Kiểm tra trạng thái phiên
            AuctionStatus status = auction.getStatus();
            if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED) {
                throw new AuctionClosedException("Auction '" + auction.getId()+ "' is already closed.");
            }

            // Kiểm tra giá hợp lệ
            if (bid.getAmount() <= auction.getCurrentPrice()) {
                throw new InvalidBidException("Bid amount " + bid.getAmount()+ " must be greater than current price "+ auction.getCurrentPrice() + ".");
            }

            // Áp dụng vào trạng thái auction
            auction.applyValidatedBid(bid);

        } catch (AuctionClosedException | InvalidBidException e) {
            System.err.println("[BidProcessor] Bid rejected: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("[BidProcessor] Unexpected error: " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }

        // Notify ngoài lock để tránh deadlock nếu observer cũng acquire lock
        auction.notifyObservers();
        return true;
    }
}