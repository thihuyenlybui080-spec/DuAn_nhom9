package org.example.loginregister.server.common.observer;

public interface Observer {
    public abstract void update(String auctionId, double newPrice, String highestBidder);
}

