package models.auction.auto_bidding;

import models.auction.Auction;
import models.manager.AuctionManager;
import models.user.Bidder;
import observer.Observer;

public class AutoBidAgent implements Observer {

    private final Bidder  bidder;
    private final Auction auction;
    private final AutoBidConfig config;
    private boolean       active = true;

    public AutoBidAgent(Bidder bidder, Auction auction, AutoBidConfig config) {
        this.bidder    = bidder;
        this.auction   = auction;
        this.config = config;
        auction.addObserver(this);   // bắt đầu lắng nghe
    }

    @Override
    public void update(String auctionId, double currentPrice, String highestBidder) {
        if (!active) return;

        //  đang dẫn đầu rồi, không cần làm gì
        if (bidder.getName().equals(highestBidder)) return;

        double nextBid = currentPrice + config.getIncrement();

        if (nextBid > config.getMaxBid()) {
            stop();   // hết ngân sách
            return;
        }
        try {
            AuctionManager mgr= AuctionManager.getInstance();
            mgr.placeBid(auctionId,bidder, nextBid);
        }catch(Exception e){
            System.err.println("[AutoBid] " + bidder.getName() + " đặt bid thất bại tại phiên " + auctionId + ": " + e.getMessage());
        }
    }

    public void stop() {
        active = false;
        auction.removeObserver(this);
    }
}