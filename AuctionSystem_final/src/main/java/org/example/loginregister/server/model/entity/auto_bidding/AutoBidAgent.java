package org.example.loginregister.server.model.entity.auto_bidding;

import org.example.loginregister.server.common.observer.Observer;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.service.BidService;

public class AutoBidAgent implements Observer {

    private final Bidder  bidder;
    private final Auction auction;
    private final AutoBidConfig config;
    private boolean active = true;

    public AutoBidAgent(Bidder bidder, Auction auction, AutoBidConfig config) {
        this.bidder = bidder;
        this.auction= auction;
        this.config = config;
        auction.addObserver(this);
    }

    @Override
    public void update(String auctionId, double currentPrice, String highestBidder) {
        if (!active) return;

        if (bidder.getName().equals(highestBidder)) return;

        double nextBid = currentPrice + config.getIncrement();

        if (nextBid > config.getMaxBid()) {
            stop();
            return;
        }
        BidService.getInstance().processAutoBid(bidder, auction, nextBid);
    }

    public void stop() {
        active = false;
        auction.removeObserver(this);
    }
}
