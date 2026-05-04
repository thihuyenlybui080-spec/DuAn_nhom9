package models.auction;

import base.Item;
import base.User;

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

     public AuctionResult(String auctionId, Item item, User winner,double finalPrice, List<BidTransaction> bidHistory) {
          this.auctionId = auctionId;
          this.item = item;
          this.winner = winner;
          this.finalPrice = finalPrice;
          this.endTime = LocalDateTime.now();
          this.bidHistory = new ArrayList<>(bidHistory); // copy để tránh thay đổi sau này
     }

     //getter
     public String getAuctionId() { return auctionId; }
     public Item getItem() { return item; }
     public User getWinner() { return winner; }
     public double getFinalPrice() { return finalPrice; }
     public LocalDateTime getEndTime() { return endTime; }
     public List<BidTransaction> getBidHistory() { return bidHistory; }

     @Override
     public String toString() {
          String winnerName = (winner != null) ? winner.getName() : "None";

          return "Session " + auctionId + " | Item: " + item.getItemName() + " | Winner: " + winnerName + " | Final Price: " + (long) finalPrice;
     }
}