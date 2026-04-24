package me.user;

public interface Observer {
    void update(String auctionId, double newPrice, String highestBidder);

}