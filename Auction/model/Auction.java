package model;

import observer.Subject;
import observer.Observer;
import model.exception.InvalidBidException;
import model.exception.AuctionClosedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction implements Subject {

    private String id;
    private String itemName;
    private double currentPrice;
    private User highestBidder;

    private final List<Observer> observers = new ArrayList<>();
    private final List<Bid> bids = new ArrayList<>();

    // ===== CONSTANT =====
    private static final String OPEN = "OPEN";
    private static final String RUNNING = "RUNNING";
    private static final String FINISHED = "FINISHED";

    private String status = OPEN;

    // Fair lock: thread chờ trước vào trước
    private final ReentrantLock lock = new ReentrantLock(true);

    // ===== CONSTRUCTOR =====
    public Auction(String id, String itemName, double startPrice) {
        this.id = id;
        this.itemName = itemName;
        this.currentPrice = startPrice;
    }

    // ===== OBSERVER =====
    @Override
    public void addObserver(Observer observer) {
        if (observer != null) {
            lock.lock();
            try {
                observers.add(observer);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void removeObserver(Observer observer) {
        lock.lock();
        try {
            observers.remove(observer);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void notifyObservers() {
        List<Observer> observerSnapshot;

        lock.lock();
        try {
            observerSnapshot = new ArrayList<>(observers);
        } finally {
            lock.unlock();
        }

        for (Observer o : observerSnapshot) {
            o.update(
                id,
                currentPrice,
                highestBidder != null ? highestBidder.getName() : "None"
            );
        }
    }

    // ===== ĐẤU GIÁ (THREAD-SAFE) =====
    public void placeBid(Bid bid)
            throws InvalidBidException, AuctionClosedException {

        if (bid == null) {
            throw new IllegalArgumentException("Bid khong hop le!");
        }

        lock.lock();

        try {
            if (FINISHED.equals(status)) {
                throw new AuctionClosedException("Auction da dong!");
            }

            if (bid.getAmount() <= currentPrice) {
                throw new InvalidBidException("Gia phai lon hon gia hien tai!");
            }

            currentPrice = bid.getAmount();
            highestBidder = bid.getBidder();
            bids.add(bid);

            status = RUNNING;

        } finally {
            lock.unlock();
        }

        notifyObservers();
    }

    // ===== KẾT THÚC =====
    public void finishAuction() {

        boolean changed = false;

        lock.lock();
        try {
            if (!FINISHED.equals(status)) {
                status = FINISHED;
                changed = true;
            }
        } finally {
            lock.unlock();
        }

        if (changed) {
            notifyObservers();

            System.out.println("=== KET THUC ===");
            System.out.println("Winner: " +
                    (highestBidder != null ? highestBidder.getName() : "None"));
        }
    }

    public String getId() {
        return id;
    }

    public String getItemName() {
        return itemName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public User getHighestBidder() {
        return highestBidder;
    }

    public List<Bid> getBids() {
        lock.lock();
        try {
            return new ArrayList<>(bids);
        } finally {
            lock.unlock();
        }
    }

    public String getStatus() {
        return status;
    }
}