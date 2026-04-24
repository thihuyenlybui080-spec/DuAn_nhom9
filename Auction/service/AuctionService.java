package service;
import model.*;
import model.exception.InvalidBidException;
import model.exception.AuctionClosedException;
import java.util.HashMap;
import java.util.Map;

public class AuctionService {
    private Map<String, Auction> auctions = new HashMap<>();
    
    public void createAuction(String Id, String item, double startPrice) {
        auctions.put(Id, new Auction(Id, item, startPrice));
    }
    public Auction getAuction(String Id) {
        return auctions.get(Id);
    }
    public void placeBid(String auctionId, User bidder, double amount)
            throws InvalidBidException, AuctionClosedException {

        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            throw new IllegalArgumentException("Auction khong ton tai!");
        }

        Bid bid = new Bid(bidder, amount);

        auction.placeBid(bid);
    }
}
