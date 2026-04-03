package service;
import model.*;
import java.util.HashMap;
import java.util.Map;

public class AuctionService {
    private Map<String, Auction> auctions = new HashMap<>();
    
    public void createAuction(String Id, String item, double starPrice) {
        auctions.put(Id, new Auction(Id, item, starPrice));
    }
    public Auction getAuction(String Id) {
        return auctions.get(Id);
    }
    public void placeBid(String auctionId, User bidder, double amount) {
        Auction auction = auctions.get(auctionId);
        if (auction != null) {
            auction.placeBid(new Bid(bidder, amount));
        } else {
            System.out.println("Auction khong ton tai!");
        }
    }
    
}
