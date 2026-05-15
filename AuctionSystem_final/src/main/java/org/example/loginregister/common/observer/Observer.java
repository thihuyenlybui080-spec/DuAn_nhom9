package org.example.loginregister.common.observer;

public interface Observer {
    public abstract void update(String auctionId, double newPrice, String highestBidder);
}

