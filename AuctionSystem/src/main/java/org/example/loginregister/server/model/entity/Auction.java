package org.example.loginregister.server.model.entity;



import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Bidder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

public class Auction implements Subject {

    // ===== CONSTANTS =====
    public static final String OPEN     = "OPEN";
    public static final String RUNNING  = "RUNNING";
    public static final String FINISHED = "FINISHED";
    public static final String CANCELED = "CANCELED";

    // ===== FIELDS =====
    private final String id;
    private final Item item;

    private volatile double  currentPrice;
    private volatile Bidder highestBidder;
    private volatile String  status = OPEN;
    private volatile long    endTimeMillis;

    /**
     * ReentrantLock thay thế cho synchronized:
     *  - Cho phép tryLock() với timeout khi cần thiết.
     *  - Hỗ trợ fairness (true) để tránh thread starvation.
     *  - Dễ test/mock hơn synchronized.
     */
    private final ReentrantLock lock = new ReentrantLock(true); // fair lock

    /** CopyOnWriteArrayList để observer list không cần lock riêng khi iterate. */
    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    /** Bid list chỉ được ghi bên trong lock nên dùng ArrayList bình thường. */
    private final List<Bid> bids = new ArrayList<>();

    private volatile ScheduledFuture<?> currentTimer;

    // ===== CONSTRUCTOR =====
    public Auction(String id, Item item, long durationInSeconds) {
        this.id           = id;
        this.item         = item;
        this.currentPrice = item.getStartingPrice();
        this.endTimeMillis = System.currentTimeMillis() + durationInSeconds * 1_000L;
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

    // ===== BIDDING =====

    /**
     * Public entry point: wraps placeBid trong try/catch.
     * Trả về true nếu đấu thầu thành công, false nếu thất bại.
     */
    public boolean processBid(Bidder user, double amount) {
        if (user == null) {
            System.err.println("Bidding error: Invalid user!");
            return false;
        }
        try {
            placeBid(new Bid(user, amount));
            return true;
        } catch (Exception e) {
            System.err.println("Bidding error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Thread-safe với ReentrantLock.
     * Mọi thay đổi trạng thái auction đều nằm trong lock.
     */
    public void placeBid(Bid bid) throws InvalidBidException, AuctionClosedException {
        if (bid == null) throw new IllegalArgumentException("Invalid bid!");

        lock.lock();
        try {
            if (FINISHED.equals(status) || CANCELED.equals(status)) {
                throw new AuctionClosedException("Auction is already closed!");
            }
            if (bid.getAmount() <= currentPrice) {
                throw new InvalidBidException("Bid amount must be greater than current price!");
            }

            currentPrice  = bid.getAmount();
            highestBidder = bid.getBidder();
            bids.add(bid);
            status = RUNNING;
            item.setCurrentPrice(currentPrice);
        } finally {
            lock.unlock();
        }

        // Notify ngoài lock để tránh deadlock nếu observer cũng cần acquire lock
        notifyObservers();
    }

    // ===== FINISH =====
    public void finishAuction() {
        lock.lock();
        try {
            if (FINISHED.equals(status) || CANCELED.equals(status)) return;
            status = FINISHED;
        } finally {
            lock.unlock();
        }

        notifyObservers();
        System.out.println("=== AUCTION FINISHED ===");
        System.out.println("Winner: " + (highestBidder != null ? highestBidder.getName() : "None"));
    }

    // ===== TIMER / EXTENSION =====
    public void extendEndTime(long additionalSeconds) {
        lock.lock();
        try {
            endTimeMillis += additionalSeconds * 1_000L;
        } finally {
            lock.unlock();
        }
        System.out.println("Extended auction " + id + " by " + additionalSeconds + " seconds");
    }

    public void setTimer(ScheduledFuture<?> timer) {
        lock.lock();
        try {
            if (this.currentTimer != null) this.currentTimer.cancel(false);
            this.currentTimer = timer;
        } finally {
            lock.unlock();
        }
    }

    // ===== GETTERS =====
    public String getId() {
        return id;
    }
    public Item   getItem() {
        return item;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public Bidder getHighestBidder() {
        return highestBidder;
    }
    public String getStatus() {
        return status;
    }
    public List<Bid> getBids() {
        return Collections.unmodifiableList(bids);
    }
    public long getSecondsRemaining() {
        return Math.max(0, (endTimeMillis - System.currentTimeMillis()) / 1_000L);
    }

    /**
     * Chỉ dùng nội bộ (Admin.cancelAuction).
     * Dùng lock để đảm bảo an toàn khi set status.
     */
    public void setStatus(String newStatus) {
        lock.lock();
        try {
            this.status = newStatus;
        } finally {
            lock.unlock();
        }
    }

    /** Expose lock để AuctionManager có thể gia hạn + đặt timer trong cùng 1 transaction. */
    public ReentrantLock getLock() {
        return lock;
    }
}
