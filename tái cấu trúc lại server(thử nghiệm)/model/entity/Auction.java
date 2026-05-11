package org.example.loginregister.server.model.entity;

import org.example.loginregister.common.observer.Observer;
import org.example.loginregister.common.observer.Subject;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Auction – Thực thể dữ liệu của một phiên đấu giá.
 *
 * Sau refactor, class này chỉ chịu trách nhiệm:
 *  1. Lưu trữ trạng thái phiên (currentPrice, highestBidder, bids, status...).
 *  2. Quản lý danh sách Observer và kích hoạt notifyObservers().
 *  3. Cung cấp lock (ReentrantLock) cho các class xử lý nghiệp vụ bên ngoài
 *     (BidProcessor, AntiSnipeHandler, AuctionManager) dùng để đảm bảo
 *     thread-safety khi thay đổi trạng thái.
 *  4. Cung cấp mutator nội bộ để các class trong server.util gọi sau khi giữ lock.
 *
 * KHÔNG chứa:
 *  - Logic validate / apply bid  → BidProcessor
 *  - Logic anti-sniping          → AntiSnipeHandler
 *  - Logic kết thúc phiên        → AuctionManager
 */
public class Auction implements Subject {

    // ===== FIELDS =====
    private final Seller seller;
    private final String id;
    private final Item   item;
    private volatile double        currentPrice;
    private volatile Bidder        highestBidder;
    /** Mặc định OPEN khi khởi tạo. */
    private volatile AuctionStatus status = AuctionStatus.OPEN;

    /**
     * Fair ReentrantLock – dùng chung cho BidProcessor, AntiSnipeHandler,
     * AuctionManager để đảm bảo extend + setTimer là atomic.
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /** CopyOnWriteArrayList: iterate observer list không cần lock riêng. */
    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    /** Chỉ ghi bên trong lock nên ArrayList là đủ. */
    private final List<BidTransaction> bids = new ArrayList<>();

    private volatile ScheduledFuture<?> currentTimer;


    // ===== CONSTRUCTOR =====
    public Auction(Seller seller, Item item) {
        this.seller       = seller;
        this.id           = "auction-" + item.getId().substring(5);
        this.item         = item;
        this.currentPrice = item.getStartingPrice();
    }


    // ===== OBSERVER (thread-safe via CopyOnWriteArrayList) =====
    @Override
    public void addObserver(Observer observer) {
        if (observer != null) observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        String bidderName = (highestBidder != null) ? highestBidder.getName() : "None";
        for (Observer o : observers) {
            o.update(id, currentPrice, bidderName);
        }
    }


    // ===== INTERNAL MUTATORS (called by BidProcessor / AntiSnipeHandler / AuctionManager) =====

    /**
     * Áp dụng một BidTransaction đã được BidProcessor validate.
     * Caller (BidProcessor) phải đang giữ lock trước khi gọi.
     */
    public void applyValidatedBid(BidTransaction bid) {
        currentPrice  = bid.getAmount();
        highestBidder = bid.getBidder();
        bids.add(bid);
        status = AuctionStatus.RUNNING;
    }

    /**
     * Gia hạn endTime – chỉ dùng bởi AntiSnipeHandler (trong lock).
     */
    public void extendEndTimeInternal(long additionalSeconds) {
        item.setEndTime(item.getEndTime().plusSeconds(additionalSeconds));
    }

    /**
     * Đặt trạng thái kết thúc – dùng bởi AuctionManager.finishAuction().
     * Caller phải đang giữ lock.
     */
    public void applyFinalStatus(AuctionStatus finalStatus) {
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED) return;
        status = finalStatus;
    }

    /**
     * Đặt trạng thái trực tiếp – dùng bởi Admin.cancelAuction().
     * Tự acquire lock bên trong.
     */
    public void setStatus(AuctionStatus newStatus) {
        lock.lock();
        try {
            this.status = newStatus;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Quản lý timer: huỷ timer cũ, lưu timer mới.
     * Caller (AntiSnipeHandler hoặc AuctionManager) phải đang giữ lock.
     */
    public void setTimer(ScheduledFuture<?> timer) {
        if (this.currentTimer != null) this.currentTimer.cancel(false);
        this.currentTimer = timer;
    }

    /**
     * Xoá tất cả bid của một bidder bị ban/xóa và cập nhật lại giá cao nhất.
     */
    public void cancelBidsFrom(Bidder bidder) {
        lock.lock();
        try {
            bids.removeIf(b -> b.getBidder().equals(bidder));
            if (highestBidder != null && highestBidder.equals(bidder)) {
                bids.stream()
                        .max(Comparator.comparingDouble(BidTransaction::getAmount))
                        .ifPresentOrElse(
                                top -> { highestBidder = top.getBidder(); currentPrice = top.getAmount(); },
                                ()  -> { highestBidder = null; currentPrice = item.getStartingPrice(); }
                        );
            }
        } finally {
            lock.unlock();
        }
        notifyObservers();
    }


    // ===== GETTERS =====

    /** Trả về lock để BidProcessor / AntiSnipeHandler có thể giữ cùng lock. */
    public ReentrantLock getLock()               { return lock; }

    public Seller getSeller()                    { return seller; }
    public String getId()                        { return id; }
    public Item   getItem()                      { return item; }
    public double getCurrentPrice()              { return currentPrice; }
    public Bidder getHighestBidder()             { return highestBidder; }
    public AuctionStatus getStatus()             { return status; }
    public List<BidTransaction> getBids()        { return Collections.unmodifiableList(bids); }


    /**
     * @return Số giây còn lại trước khi phiên kết thúc, trả về 0 nếu đã hết thời gian.
     */
    public long getSecondsRemaining() {
        return Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), item.getEndTime()));
    }
}
