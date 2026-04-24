package models.auction;

import java.time.LocalDateTime;

import models.user.Bidder;

public class Bid {
    private Bidder bidder;
    private double amount;
    private LocalDateTime time;
    public Bid(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = LocalDateTime.now();
    }
    public Bidder getBidder() {
        return bidder;
    }
    public double getAmount() {
        return amount;
    }
    public LocalDateTime getTime() {
        return time;
    }
    @Override
    public String toString() {
        return bidder.getName() + " bid " + amount + " at " + time;
    }
}
