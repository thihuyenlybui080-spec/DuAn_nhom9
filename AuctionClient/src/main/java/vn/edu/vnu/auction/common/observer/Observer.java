package vn.edu.vnu.auction.common.observer;

public interface Observer {
    public abstract void update(int auctionId, double newPrice, String highestBidder);
}

