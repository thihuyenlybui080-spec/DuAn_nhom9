package org.example.loginregister.server.model.entity.auto_bidding;

import org.example.loginregister.common.observer.Observer;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.util.AuctionManager;

public class AutoBidAgent implements Observer {

    private final Bidder  bidder;
    private final Auction auction;
    private final AutoBidConfig config;
    private boolean active = true;

    public AutoBidAgent(Bidder bidder, Auction auction, AutoBidConfig config) {
        this.bidder = bidder;
        this.auction= auction;
        this.config = config;
        auction.addObserver(this);   // bắt đầu lắng nghe
    }

    @Override
    public void update(String auctionId, double currentPrice, String highestBidder) {
        if (!active) return;

        //  đang dẫn đầu , không cần làm gì
        if (bidder.getName().equals(highestBidder)) return;

        double nextBid = currentPrice + config.getIncrement();

        if (nextBid > config.getMaxBid()) {
            stop();   // hết ngân sách
            return;
        }
        try {
            bidder.bid(auction, nextBid);

        }catch(Exception e){
            System.err.println("[AutoBid] " + bidder.getName() + " failed to place bid in auction " + auctionId + ": " + e.getMessage());
        }
    }

    public void stop() {
        active = false;
        auction.removeObserver(this);
    }
}
