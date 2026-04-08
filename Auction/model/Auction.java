package model;

import observer.Subject;
import observer.Observer;
import model.exception.InvalidBidException;
import model.exception.AuctionClosedException;

import java.util.ArrayList;
import java.util.List;

public class Auction implements Subject {

    private String id;
    private String itemName;
    private double currentPrice;
    private User highestBidder;

    private List<Observer> observers = new ArrayList<>();
    private List<Bid> bids = new ArrayList<>();

    // ===== CONSTANT =====
    private static final String OPEN = "OPEN";
    private static final String RUNNING = "RUNNING";
    private static final String FINISHED = "FINISHED";

    private String status = OPEN;

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
            o.update(
                id,
                currentPrice,
                highestBidder != null ? highestBidder.getName() : "None"
            );
        }
    }

    // ===== ĐẤU GIÁ (THREAD-SAFE) =====
    public synchronized void placeBid(Bid bid)
            throws InvalidBidException, AuctionClosedException {

        if (bid == null) {
            throw new IllegalArgumentException("Bid khong hop le!");
        }

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

        notifyObservers();
    }

    // ===== KẾT THÚC =====
    public void finishAuction() {
        if (!FINISHED.equals(status)) {
            status = FINISHED;

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
        return bids;
    }

    public String getStatus() {
        return status;
    }
}