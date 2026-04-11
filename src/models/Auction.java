package models;

import base.Item;
import observer.Subject;
import observer.Observer;
import exceptions.InvalidBidException;
import exceptions.AuctionClosedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

public class Auction implements Subject {

    private String id;
    private Item item;
    private double currentPrice;
    private Bidder highestBidder;

    private List<Observer> observers = new ArrayList<>();
    private List<Bid> bids = new ArrayList<>();

    // ===== CONSTANT =====
    private static final String OPEN = "OPEN";
    private static final String RUNNING = "RUNNING";
    private static final String FINISHED = "FINISHED";
    private static final String CANCELED = "CANCELED";

    private String status = OPEN;
    private long endTimeMillis;           // used to calculate getSecondsRemaining()

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
            o.update(id, currentPrice, highestBidder != null ? highestBidder.getName() : "None");
        }
    }

    // ===== BIDDING (THREAD-SAFE) =====
    public boolean processBid(Bidder user, double amount) {
        try {
            if (user == null) throw new IllegalArgumentException("Invalid user!");

            Bid bid = new Bid(user, amount);           // Create Bid here

            placeBid(bid);                             // Call synchronized method
            return true;
        } catch (Exception e) {
            System.err.println("Bidding error: " + e.getMessage());
            return false;
        }
    }

    public synchronized void placeBid(Bid bid) throws InvalidBidException, AuctionClosedException {

        if (bid == null) {
            throw new IllegalArgumentException("Invalid bid!");
        }

        if (FINISHED.equals(status) || CANCELED.equals(status)) {
            throw new AuctionClosedException("Auction is already closed!");
        }

        if (bid.getAmount() <= currentPrice) {
            throw new InvalidBidException("Bid amount must be greater than current price!");
        }

        currentPrice = bid.getAmount();
        highestBidder = bid.getBidder();
        bids.add(bid);

        status = RUNNING;
        item.setCurrentPrice(currentPrice);
        notifyObservers();
    }

    // ===== AUCTION EXTENSION =====
    private ScheduledFuture<?> currentTimer;   // to cancel old timer

    public void extendEndTime(long additionalSeconds) {
        this.endTimeMillis += additionalSeconds * 1000;
        System.out.println("Extended auction " + id + " by " + additionalSeconds + " seconds");
    }

    public void setTimer(ScheduledFuture<?> timer) {
        if (this.currentTimer != null) this.currentTimer.cancel(false);
        this.currentTimer = timer;
    }

    // ===== FINISH =====
    public void finishAuction() {
        if (!FINISHED.equals(status) && !CANCELED.equals(status)) {
            status = FINISHED;

            notifyObservers();

            System.out.println("=== AUCTION FINISHED ===");
            System.out.println("Winner: " + (highestBidder != null ? highestBidder.getName() : "None"));
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

    public Bidder getHighestBidder() {
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