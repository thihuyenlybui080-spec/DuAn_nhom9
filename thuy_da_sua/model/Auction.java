package model;

import observer.Subject;
import observer.Observer;
import model.exception.InvalidBidException;
import model.exception.AuctionClosedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;



public class Auction implements Subject {

    private String id;
    private Item item;
    private double currentPrice;
    private User highestBidder;

    private List<Observer> observers = new ArrayList<>();
    private List<Bid> bids = new ArrayList<>();

    // ===== CONSTANT =====
    private static final String OPEN = "OPEN";
    private static final String RUNNING = "RUNNING";
    private static final String FINISHED = "FINISHED";
    private static final String CANCELED = "CANCELED";

    private String status = OPEN;
    private long endTimeMillis;           // dùng để tính getSecondsRemaining()

    // ===== CONSTRUCTOR =====
    public Auction(String id, Item item, long durationInSeconds) {
        this.id = id;
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        this.endTimeMillis = System.currentTimeMillis() + durationInSeconds * 1000;
    }

    // ===== OBSERVER =====
    @Override
    public void addObserver(Observer observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer o : observers) {
            o.update(id,currentPrice,highestBidder != null ? highestBidder.getName() : "None");
        }
    }

    // ===== ĐẤU GIÁ (THREAD-SAFE) =====
    public boolean processBid(User user, double amount) {
        try {
            if (user == null) throw new IllegalArgumentException("User không hợp lệ!");

            Bid bid = new Bid(user, amount);           // Tạo Bid ở đây

            placeBid(bid);                             // Gọi method synchronized
            return true;
        } catch (Exception e) {
            System.err.println("Lỗi đặt giá: " + e.getMessage());
            return false;
        }
    }
    public synchronized void placeBid(Bid bid)throws InvalidBidException, AuctionClosedException {

        if (bid == null) {
            throw new IllegalArgumentException("Bid khong hop le!");
        }

        if (FINISHED.equals(status)|| CANCELED.equals(status)) {
            throw new AuctionClosedException("Auction da dong!");
        }

        if (bid.getAmount() <= currentPrice) {
            throw new InvalidBidException("Gia phai lon hon gia hien tai!");
        }

        currentPrice = bid.getAmount();
        highestBidder = bid.getBidder();
        bids.add(bid);

        status = RUNNING;
        item.setCurrentPrice(currentPrice);
        notifyObservers();
    }

    // =====GIA HẠN ĐẤU GIÁ  =====
    private ScheduledFuture<?> currentTimer;   // để cancel timer cũ

    public void extendEndTime(long additionalSeconds) {
        this.endTimeMillis += additionalSeconds * 1000;
        System.out.println("Gia hạn phiên " + id + " thêm " + additionalSeconds + " giây");
    }

    public void setTimer(ScheduledFuture<?> timer) {
        if (this.currentTimer != null) this.currentTimer.cancel(false);
        this.currentTimer = timer;
    }

    // ===== KẾT THÚC =====
    public void finishAuction() {
        if (!FINISHED.equals(status)&& !CANCELED.equals(status)) {
            status = FINISHED;

            notifyObservers();

            System.out.println("=== KET THUC ===");
            System.out.println("Winner: " +(highestBidder != null ? highestBidder.getName() : "None"));
        }
    }

    public String getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public User getHighestBidder() {
        return highestBidder;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status; 
    }
    public long getSecondsRemaining() {
        return Math.max(0, (endTimeMillis - System.currentTimeMillis()) / 1000);
    }
}