package org.example.loginregister.common.observer;

import org.example.loginregister.server.model.entity.user.Bidder;

public interface Observer {
    public abstract void update(String auctionId, double newPrice, String highestBidder);
}

