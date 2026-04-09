import java.util.*;
import java.util.concurrent.*;


public class AuctionManager {
    
     private static volatile AuctionManager instance;
     private Map<String, Auction> activeAuctions;
     //Scheduler để đếm ngược thời gian 
     private ScheduledExecutorService scheduler;

     
     private AuctionManager() {
          activeAuctions = new ConcurrentHashMap<>();
          // Khởi tạo Thread Pool để xử lý nhiều phiên đấu giá đếm ngược cùng lúc
          scheduler = Executors.newScheduledThreadPool(10); 
     }

     public static AuctionManager getInstance() {
          if (instance == null) {
               synchronized (AuctionManager.class) {
               if (instance == null) {
               instance = new AuctionManager();
                    }
               }
          }
          return instance;
     }

     
     public void startAuction(String auctionId, Item item,long durationInSeconds) {
          if (!activeAuctions.containsKey(auctionId)) {
            
               Auction newAuction = new Auction(auctionId, item);
               newAuction.setStatus("RUNNING");
               activeAuctions.put(auctionId, newAuction);
               System.out.println("Mở phiên " + auctionId + " trong " + durationInSeconds + " giây.");

               //  Tự động đóng phiên khi hết thời gian 
               scheduler.schedule(() -> endAuction(auctionId), durationInSeconds, TimeUnit.SECONDS);
          }
     }

     private synchronized void endAuction(String auctionId){
          Auction auction = activeAuctions.get(auctionId);
          if (auction != null ) {
               auction.setStatus("FINISHED");
               
               //Xác định người thắng cuộc 
               User winner = auction.getCurrentWinner();
               if (winner != null) {
                    System.out.println("Phiên " + auctionId + " kết thúc! Người thắng: " + winner.getUsername() + " với giá " + auction.getCurrentHighestBid());
               } else {
                    System.out.println("Phiên " + auctionId + " kết thúc! Không có ai đặt giá.");
               }
               
               // Xóa auction khi đã xong
               activeAuctions.remove(auctionId);
          }
     }
     
          // Xử lý đấu giá đồng thời 
         
     public synchronized boolean placeBid(String auctionId, User user, double amount) {
          Auction auction = activeAuctions.get(auctionId);
          
          if (auction == null || !auction.getStatus().equals("RUNNING")) {
               System.err.println("Lỗi: Phiên đấu giá không tồn tại hoặc đã kết thúc!");
               return false; 
          }
          // Logic xử lý giá bên trong lớp Auction
          boolean success = auction.processBid(user, amount);

          if (success) {
               // Gia hạn phiên đấu giá (Anti-sniping Algorithm) [cite: 95]
               // Nếu có bid mới trong X giây cuối -> tự động gia hạn thêm Y giây 
               if (auction.getSecondsRemaining() < 30) { 
                    System.out.println("Anti-sniping kích hoạt: Gia hạn thêm 60 giây cho phiên " + auctionId);
               }
          }
          
          return success;
     }
}
