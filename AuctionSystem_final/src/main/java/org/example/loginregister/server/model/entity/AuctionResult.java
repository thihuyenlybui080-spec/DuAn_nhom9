package org.example.loginregister.server.model.entity;

import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//lưu kết quả
public class AuctionResult {

    private final String auctionId;
    private final Item item;
    private final User winner;
    private final double finalPrice;
    private final LocalDateTime endTime;
    private final List<BidTransaction> bidHistory;
    private  AuctionStatus status;//có thể thay đổi từ FINISHED->PAID

    public AuctionResult(Auction auction) {
        this.auctionId = auction.getId();
        this.item = auction.getItem();
        this.winner = auction.getHighestBidder();
        this.finalPrice = auction.getCurrentPrice();
        this.status=auction.getStatus();
        this.endTime = LocalDateTime.now();
        this.bidHistory = new ArrayList<>(auction.getBids()); // copy để tránh thay đổi sau này
    }

    public void setStatusPaid(){
        status=AuctionStatus.PAID;
    }

    //getter
    public String getAuctionId() { return auctionId; }
    public Item getItem() { return item; }
    public User getWinner() { return winner; }
    public double getFinalPrice() { return finalPrice; }
    public LocalDateTime getEndTime() { return endTime; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public AuctionStatus getStatus() {return status;}

    @Override
    public String toString() {
        String winnerName = (winner != null) ? winner.getName() : "None";

        return "Session " + auctionId + " | Item: " + item.getItemName() + " | Winner: " + winnerName + " | Final Price: " + (long) finalPrice;
    }
}
