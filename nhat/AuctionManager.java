import java.util.*;
import java.util.concurrent.*;


public class AuctionManager {
    
     private static volatile AuctionManager instance;
     private Map<String, Auction> activeAuctions;//kho lưu trữ tạm thời các auction đg hd
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
            
               Auction newAuction = new Auction(auctionId, item,durationInSeconds);
               newAuction.setStatus("RUNNING");
               activeAuctions.put(auctionId, newAuction);
               System.out.println("Mở phiên " + auctionId + " trong " + durationInSeconds + " giây.");

               //  Tự ture<?> newTimer động đóng phiên khi hết thời gian 
               ScheduledFuture<?> timer=scheduler.schedule(() -> endAuction(auctionId), durationInSeconds, TimeUnit.SECONDS);
               newAuction.setTimer(timer);
               
          }else{
               System.err.println("Phiên đấu giá " + auctionId + " đã tồn tại!");
          }
     }

     private void endAuction(String auctionId){
          Auction auction = activeAuctions.get(auctionId);
          if (auction != null ) {
               
               auction.finishAuction();
               AuctionResult result = new AuctionResult(//lấy kq auction
                    auction.getId(),
                    auction.getItem(),
                    auction.getHighestBidder(),
                    auction.getCurrentPrice(),
                    auction.getBids()
               );
               AuctionHistoryManager.getInstance().saveResult(result);//lưu kq vào kho 

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

          if (success && auction.getSecondsRemaining() < 30) {
               // Gia hạn phiên đấu giá (Anti-sniping Algorithm) 
               // Nếu có bid mới trong X giây cuối -> tự động gia hạn thêm Y giây 
                
               System.out.println("Anti-sniping kích hoạt: Gia hạn thêm 60 giây cho phiên " + auctionId);
               // Gia hạn thực tế bằng cách tạo lại timer
               auction.extendEndTime(60);
               ScheduledFuture<?> newTimer=scheduler.schedule(() -> endAuction(auctionId), 60, TimeUnit.SECONDS);
               auction.setTimer(newTimer);
          }
          
          return success;
     }
}