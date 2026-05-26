package vn.edu.vnu.auction.model.entity;

import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;

import java.io.Serializable;
import java.time.LocalDateTime;
public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private Bidder bidder;
    private Item item;
    private double amount;
    private LocalDateTime timestamp;
    private int auctionId;
    public BidTransaction(Bidder bidder, Item item, double amount){
        this.bidder = bidder;
        this.item = item;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }
    public Bidder getBidder(){
        return bidder;
    }
    public Item getItem(){
        return item;
    }
    public double getAmount(){
        return amount;
    }
    public LocalDateTime getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }
}
