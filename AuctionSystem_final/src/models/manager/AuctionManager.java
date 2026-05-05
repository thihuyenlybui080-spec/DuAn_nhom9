package models.manager;

import base.Item;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import models.auction.Auction;
import models.auction.AuctionResult;
import models.auction.AuctionStatus;
import models.user.Bidder;

/**
 * AuctionManager – Singleton quản lý tất cả phiên đấu giá đang hoạt động.
 * - Anti-sniping (gia hạn + đặt lại timer) thực hiện bên trong lock của Auction
 * để tránh race condition giữa extend và endAuction.
 * - Singleton vẫn dùng double-checked locking với volatile (không đổi).
 * - endAuction() dùng ConcurrentHashMap.remove() – đủ thread-safe, không cần lock riêng.
 */
public class AuctionManager {

    // ===== SINGLETON =====
    private static volatile AuctionManager instance;

    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        scheduler      = Executors.newScheduledThreadPool(10);
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

    // ===== FIELDS =====
    /** ConcurrentHashMap: đọc/xoá không cần lock ngoài. */
    private final Map<String, Auction> activeAuctions;
    private final ScheduledExecutorService scheduler;

    /** Thời gian còn lại dưới ngưỡng này sẽ kích hoạt anti-sniping (giây). */
    private static final long ANTI_SNIPE_THRESHOLD_SECONDS = 30;

    /** Thời gian gia hạn khi anti-sniping kích hoạt (giây). */
    private static final long ANTI_SNIPE_EXTENSION_SECONDS = 60;

    // ===== PUBLIC API =====


    //tự lên lịch mở auction dựa vào startTime  
    public void startAuction(String auctionId, Item item) {
        LocalDateTime now = LocalDateTime.now();
        long startDelay = ChronoUnit.SECONDS.between(now, item.getStartTime());
        long endDelay   = ChronoUnit.SECONDS.between(now, item.getEndTime());

        //TH  end < strart
        if (item.getEndTime().isBefore(item.getStartTime())) {
            System.err.println("End time must be after start time!");
            return;
        }
        //TH endTime<now
        if (endDelay <= 0) {
            System.err.println("Auction end time is in the past!");
            return;
        }

        // Tạo auction với status OPEN, chưa đưa vào activeAuctions
        Auction auction = new Auction( item);

        if (startDelay <= 0) {
            // startTime đã qua → mở luôn
            openAuction(auction);
        } else {
            // Lên lịch mở auction khi đến startTime
            scheduler.schedule(() -> openAuction(auction), startDelay, TimeUnit.SECONDS);
            System.out.println("Auction " + auctionId + " scheduled to open in " + startDelay + "s");
        }
    }
    
    private void openAuction(Auction auction) {
        // Giữ nguyên status = OPEN (không ép RUNNING).
        // Status chỉ chuyển sang RUNNING khi có bid đầu tiên
        activeAuctions.put(auction.getId(), auction);
        System.out.println("Auction " + auction.getId() + " is now OPEN for bidding!");
        auction.notifyObservers(); // báo cho client biết phiên đã mở

        // Lên lịch kết thúc auction khi đến endTime
        // Tính lại delay TẠI THỜI ĐIỂM MỞ PHIÊN, không phải T=0
        long endDelay = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getItem().getEndTime());
        scheduleEnd(auction, endDelay);

    }

    /**
     * Đặt giá thầu.
     *
     * Thread-safety:
     * - Đọc auction từ ConcurrentHashMap (lock-free).
     * - Gọi auction.processBid() – bên trong có ReentrantLock riêng của Auction.
     * - Anti-sniping dùng auction.getLock() để extend + reschedule timer
     * trong cùng một critical section, tránh race với endAuction().
     */
    public boolean placeBid(String auctionId, Bidder bidder, double amount) {
        Auction auction = activeAuctions.get(auctionId);
        //auction không tồn tại
        if (auction == null ) {
            System.err.println("Error: Auction session does not exist or has already ended!");
            return false;
        }


        boolean success = auction.processBid(bidder, amount);

        if (success) {
            //lưu lịch sử dao dịch cho  uesr
            bidder.recordBid(auction.getItem(), amount);
            tryAntiSnipe(auction);
        }

        return success;
    }

    

    public void removeAuction(String auctionId) {
        activeAuctions.remove(auctionId);
    }


    /**
     * Dừng scheduler khi application tắt để tránh thread leak.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }


    //xử lí trường hợp seller
    public void cancelAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

        auction.setStatus(AuctionStatus.CANCELED);  // đóng cửa bid mới

        // Lưu kết quả dù là CANCELED (yêu cầu của bạn)
        AuctionResult result = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(result);

        activeAuctions.remove(auctionId);           // xóa khỏi map
        // timer tự hủy vì endAuction() sẽ check auction == null rồi return
    }



    // ===== PRIVATE HELPERS =====

    /**
     * Anti-sniping: nếu còn ít hơn THRESHOLD giây, gia hạn và lên lịch lại.
     * Toàn bộ thực hiện bên trong lock của Auction để đảm bảo:
     * - Không race với endAuction() đang chạy.
     * - extend và setTimer là 1 atomic operation.
     */
    private void tryAntiSnipe(Auction auction) {
        boolean extended = auction.tryExtendForAntiSnipe(ANTI_SNIPE_THRESHOLD_SECONDS, ANTI_SNIPE_EXTENSION_SECONDS);
        if (extended) {
            scheduleEnd(auction, ANTI_SNIPE_EXTENSION_SECONDS);
        }
    }


    /** Lên lịch kết thúc auction (không giữ lock). */
    private void scheduleEnd(Auction auction, long delaySeconds) {
        ScheduledFuture<?> timer = scheduler.schedule(
                () -> endAuction(auction.getId()), delaySeconds, TimeUnit.SECONDS);
        auction.setTimer(timer);
    }

    /** Kết thúc phiên đấu giá, lưu kết quả, xoá khỏi map. */
    private void endAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

        //nếu k có người thắng thì status auction==CANCELED
        AuctionStatus finalStatus = (auction.getHighestBidder() != null) ? AuctionStatus.FINISHED : AuctionStatus.CANCELED;

        auction.finishAuction(finalStatus); // thread-safe bên trong

        AuctionResult result = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(result);

        activeAuctions.remove(auctionId);
    }

    


    //====GETTER====
    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    public Auction getAuction(String auctionId) {
        return activeAuctions.get(auctionId);
    }
}
