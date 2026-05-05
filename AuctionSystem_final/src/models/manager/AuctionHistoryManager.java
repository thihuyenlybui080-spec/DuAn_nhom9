package models.manager;

import base.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import models.auction.AuctionResult;

public class AuctionHistoryManager {

     private static final AuctionHistoryManager instance = new AuctionHistoryManager();
     private final Map<String, AuctionResult> completedAuctions = new ConcurrentHashMap<>();

     private AuctionHistoryManager() {}   

     public static AuctionHistoryManager getInstance() {
          return instance;
     }

      
     // Lưu kết quả phiên đấu giá khi kết thúc
     public void saveResult(AuctionResult result) {
          if (result != null) {
               completedAuctions.put(result.getAuctionId(), result);
               System.out.println("AUCTION RESULT SAVED: " + result);
          }
     }

     
     //Lấy kết quả của một phiên theo ID
     public AuctionResult getResult(String auctionId) {
          return completedAuctions.get(auctionId);
     }


     // Lấy toàn bộ lịch sử đấu giá
     public List<AuctionResult> getAllResults() {
          return new ArrayList<>(completedAuctions.values());
     }

  
     //Lấy lịch sử theo người thắng (tùy chọn)
    
     public List<AuctionResult> getResultsByWinner(User winner) {
          List<AuctionResult> list = new ArrayList<>();
          for (AuctionResult r : completedAuctions.values()) {
               if (r.getWinner() != null && r.getWinner().equals(winner)) {
                    list.add(r);
               }
          }
          return list;
     }

     
     //Xóa lịch sử (dùng khi test hoặc reset hệ thống)
     public void clearHistory() {
          completedAuctions.clear();
     }

     public void updateStatusPaid(String auctionId){
          completedAuctions.get(auctionId).setStatusPaid();
     }
}