package model;
import observer.Observer;
public class User implements Observer {
    private String id;
    private String name;
    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }
    @Override
    public void update(String auctionId, double newPrice, String highestBidder) {
        System.out.println("User " + name + " received update: Auction " + auctionId + " | New price: " + newPrice + "| Highest bidder: " + highestBidder);
    }
    public String getName() {
        return name;
    }
}
    

