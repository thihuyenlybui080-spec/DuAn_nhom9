package vn.edu.vnu.auction.model.entity;

import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.User;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int auctionId;
    private final Item item;
    private final User winner;
    private final double finalPrice;
    private final LocalDateTime endTime;
    private static List<BidTransaction> bidHistory;
    private  AuctionStatus status;

    public AuctionResult(Auction auction) {
        this.auctionId = auction.getId();
        this.item = auction.getItem();
        this.winner = auction.getHighestBidder();
        this.finalPrice = auction.getCurrentPrice();
        this.status=auction.getStatus();
        this.endTime = auction.getItem().getEndTime();
        this.bidHistory = new ArrayList<>(auction.getBids());
    }

    public void setStatus(AuctionStatus newStatus){
        status=newStatus;
    }

    //getter
    public int getAuctionId() { return auctionId; }
    public Item getItem() { return item; }
    public User getWinner() { return winner; }
    public double getFinalPrice() { return finalPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public static List<BidTransaction> getBidHistory() { return bidHistory; }
    public AuctionStatus getStatus() {return status;}

    @Override
    public String toString() {
        String winnerName = (winner != null) ? winner.getName() : "None";

        return "Session " + auctionId + " | Item: " + item.getItemName() + " | Winner: " + winnerName + " | Final Price: " + (long) finalPrice;
    }
}
