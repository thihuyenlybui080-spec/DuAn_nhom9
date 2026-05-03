package models.auction.auto_bidding;

import models.auction.Auction;
import models.auction.Bid;
import models.user.Bidder;
import observer.Observer;

public class AutoBidAgent implements Observer {

    private final Bidder  bidder;
    private final Auction auction;
    private final double  maxBid;
    private final double  increment;
    private boolean       active = true;

    public AutoBidAgent(Bidder bidder, Auction auction, double maxBid, double increment) {
        this.bidder    = bidder;
        this.auction   = auction;
        this.maxBid    = maxBid;
        this.increment = increment;
        auction.addObserver(this);   // bắt đầu lắng nghe
    }

    @Override
    public void update(String auctionId, double currentPrice, String highestBidder) {
        if (!active) return;

        //  đang dẫn đầu rồi, không cần làm gì
        if (bidder.getName().equals(highestBidder)) return;

        double nextBid = currentPrice + increment;

        if (nextBid > maxBid) {
            stop();   // hết ngân sách
            return;
        }
        try {
            auction.placeBid(new Bid(bidder, nextBid));
        }catch(Exception e){
            System.err.println("[AutoBid] " + bidder.getName() + " đặt bid thất bại tại phiên " + auctionId + ": " + e.getMessage());
        }
    }

    public void stop() {
        active = false;
        auction.removeObserver(this);
    }
}