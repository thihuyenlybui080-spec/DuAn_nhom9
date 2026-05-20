package org.example.loginregister.server.model.entity.auto_bidding;

import org.example.loginregister.server.common.observer.Observer;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.service.BidService;
import org.example.loginregister.server.util.AuctionManager;

import java.io.Serializable;

public class AutoBidAgent implements Observer, Serializable {

    private static final long serialVersionUID = 1L;
    private final Bidder  bidder;
    private final String auctionId;
    private final AutoBidConfig config;
    private boolean active = true;

    public AutoBidAgent(Bidder bidder, Auction auction, AutoBidConfig config) {
        this.bidder = bidder;
        this.auctionId = auction.getId();
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
        // Get current auction from AuctionManager to ensure we have the latest object
        Auction currentAuction = AuctionManager.getInstance().getActive(this.auctionId);
        if (currentAuction != null) {
            BidService.getInstance().processAutoBid(bidder, currentAuction, nextBid);
        }
    }

    public void stop() {
        active = false;
        Auction currentAuction = AuctionManager.getInstance().getActive(auctionId);
        if (currentAuction != null) {
            currentAuction.removeObserver(this);
        }
    }
}
