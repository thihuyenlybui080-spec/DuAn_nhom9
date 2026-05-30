package vn.edu.vnu.auction.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.common.observer.Observer;
import vn.edu.vnu.auction.common.network.NotificationMessage;
import vn.edu.vnu.auction.dao.AutoBidDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.auto_bidding.AutoBid;
import vn.edu.vnu.auction.model.entity.auto_bidding.AutoBidConfig;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.util.AuctionManager;
import vn.edu.vnu.auction.ClientRegistry;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dịch vụ quản lý tính năng tự động đặt giá (Auto-Bid).
 * <p>
 * Lớp này chịu trách nhiệm quản lý và xử lý các yêu cầu auto-bid từ người tham gia đấu giá.
 * Nó duy trì hàng đợi ưu tiên cho mỗi phiên đấu giá và tự động đặt giá khi có người khác
 * đặt giá cao hơn, miễn là giá không vượt quá mức giá tối đa đã thiết lập.
 * </p>
 */
public class AutobidService implements Observer {
    private static final Logger logger = LoggerFactory.getLogger(AutobidService.class);
    private static volatile AutobidService instance;
    private final Map<Integer, PriorityQueue<AutoBid>> queues = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicBoolean> processing = new ConcurrentHashMap<>();
    private static final ExecutorService autoBidExecutor = Executors.newCachedThreadPool();

    private AutobidService() {}

    /**
     * Lấy thể hiện duy nhất của AutobidService (Singleton pattern).
     *
     * @return thể hiện duy nhất của AutobidService
     */
    public static AutobidService getInstance() {
        if (instance == null) {
            synchronized (AutobidService.class){
                if (instance == null){
                    instance = new AutobidService();
                }
            }
        }
        return instance;
    }

    /**
     * Phương thức callback từ Observer interface, được gọi khi có cập nhật giá đấu giá.
     *
     * @param auctionId ID của phiên đấu giá
     * @param currentPrice giá hiện tại của phiên đấu giá
     * @param highestBidder tên của người đang có giá cao nhất
     */
    @Override
    public void update(int auctionId, double currentPrice, String highestBidder) {
        AtomicBoolean flag = processing.computeIfAbsent(auctionId, k -> new AtomicBoolean(false));
        if(!flag.compareAndSet(false, true)){
            logger.debug("AutoBid queue for auction {} is already being processed", auctionId);
            return;
        }
        autoBidExecutor.submit(() -> {
            try {
                processQueue(auctionId);
            } finally {
                flag.set(false);
            }
        });

    }

    /**
     * Xử lý hàng đợi auto-bid cho một phiên đấu giá cụ thể.
     *
     * @param auctionId ID của phiên đấu giá
     *tên của người đang có giá cao nhất
     */
    public void processQueue(int auctionId) {
        PriorityQueue<AutoBid> queue = queues.get(auctionId);
        if (queue == null || queue.isEmpty()) return;

        Auction auction = AuctionManager.getInstance().getActive(auctionId);
        if (auction == null) return;

        double currentPrice = auction.getCurrentPrice();
        Bidder currentHighest = auction.getHighestBidder();

        for (AutoBid autoBid : queue) {
            if (!autoBid.isActive()) continue;
            if(isBidderLocked(autoBid, auctionId)) continue;
            if(isAlreadyLeading(autoBid, currentHighest)) continue;

            double nextBid = currentPrice + autoBid.getConfig().increment();
            if (nextBid > autoBid.getConfig().maxBid()) {
                autoBid.deactivate();
                logger.info("AutoBid maxBid reached: bidder={}, "
                                + "auctionId={}",
                        autoBid.getBidder().getName(), auctionId);
                continue;
            }
            if(tryPlaceBid(autoBid.getBidder(), auction, nextBid)){
                notifyBidUpdated(auctionId);
                break;
            }
        }
    }


    /**
     * Kích hoạt tính năng auto-bid cho một người tham gia đấu giá.
     *
     * @param bidder người tham gia đấu giá muốn kích hoạt auto-bid
     * @param auction phiên đấu giá muốn kích hoạt auto-bid
     * @param config cấu hình auto-bid bao gồm giá tối đa và bước tăng giá
     */
    public void enableAutoBid(Bidder bidder, Auction auction, AutoBidConfig config) {
        disableAutoBid(auction.getId(), bidder.getId());
        registerAutoBid(bidder, auction.getId(), config);

        if(isAlreadyLeading(bidder, auction)){
            logger.info("Autobid registered but bidder {} already leading - skipping initial bid", bidder.getName());
            return;
        }

        double nextBid = auction.getCurrentPrice() + config.increment();
        if (nextBid <= config.maxBid() && tryPlaceBid(bidder, auction, nextBid)) {
            logger.info("Initial auto-bid placed: bidder={}, amount={}, auctionId={}",
                    bidder.getName(), nextBid, auction.getId());
            boolean success = BidService.getInstance().processAutoBid(bidder, auction, nextBid);
            autoBidExecutor.submit(() -> notifyBidUpdated(auction.getId()));
        }
    }
    /**
     * Vô hiệu hóa tính năng auto-bid cho một người tham gia đấu giá.
     *
     * @param auctionId ID của phiên đấu giá
     * @param bidderId ID của người tham gia đấu giá
     */
    public void disableAutoBid(int auctionId, int bidderId) {
        PriorityQueue<AutoBid> queue = queues.get(auctionId);
        if (queue != null) {
            queue.stream()
                    .filter(ab -> ab.getBidder().getId() == bidderId)
                    .forEach(AutoBid::deactivate);
            queue.removeIf(ab -> !ab.isActive());
        }
        AutoBidDAO.deleteAutoBid(auctionId, bidderId);
        logger.info("AutoBid disabled: bidderId={}, auctionId={}",
                bidderId, auctionId);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────
    private void registerAutoBid(Bidder bidder, int auctionId, AutoBidConfig config){
        AutoBid autoBid = new AutoBid(bidder, auctionId, config);
        queues.computeIfAbsent(auctionId ,k -> new PriorityQueue<>(
                        Comparator.comparing(AutoBid::getRegisteredAt)
                )
        ).offer(autoBid);
        AutoBidDAO.saveAutoBid(auctionId, bidder.getId(), config.maxBid(), config.increment());
        logger.info("AutoBid enabled: bidder={}, auction={}",
                bidder.getName(), auctionId);
    }

    private boolean isBidderLocked(AutoBid autoBid, int auctionId){
        if(autoBid.getBidder().isActive()) return false;
        autoBid.deactivate();
        logger.info("AutoBid deactivated: bidder {} is locked",
                autoBid.getBidder().getName());
        ClientRegistry.getInstance().notifyAll(auctionId, new NotificationMessage(
                NotificationMessage.TYPE_USER_LOCKED,
                auctionId,
                autoBid.getBidder().getId()
        ));
        return true;
    }

    private boolean isAlreadyLeading(AutoBid autoBid, Bidder currentHighest){
        return currentHighest != null && autoBid.getBidder().getId() == currentHighest.getId();
    }

    private boolean isAlreadyLeading(Bidder bidder, Auction auction){
        Bidder highest = auction.getHighestBidder();
        return highest != null && highest.getId() == bidder.getId();
    }

    private boolean tryPlaceBid(Bidder bidder, Auction auction, double amount){
        try {
            boolean success = BidService.getInstance()
                    .processAutoBid(bidder, auction, amount);
            if (success) {
                logger.info("AutoBid placed: bidder={}, amount={}, auctionId={}",
                        bidder.getName(), amount, auction.getId());
            }
            return success;
        } catch (Exception e) {
            logger.error("AutoBid failed: bidder={}, amount={}, auctionId={}",
                    bidder.getName(), amount, auction.getId(), e);
            return false;
        }
    }
    private void notifyBidUpdated(int auctionId){
        Auction updatedAuction = AuctionManager.getInstance().getActive(auctionId);
        if (updatedAuction != null) {
            ClientRegistry.getInstance().notifyAll(auctionId, new NotificationMessage(
                    NotificationMessage.TYPE_BID_UPDATED,
                    auctionId,
                    updatedAuction
            ));
        }
    }

    /**
     * Xóa tất cả auto-bid liên quan đến một phiên đấu giá.
     *
     * @param auctionId ID của phiên đấu giá cần xóa
     */
    public void clearAuction(int auctionId){
        queues.remove(auctionId);
        processing.remove(auctionId);
        logger.info("AutoBid cleared: auctionId={}", auctionId);
    }

    /**
     * Kiểm tra xem auto-bid có đang hoạt động cho một người tham gia đấu giá cụ thể không.
     *
     * @param auctionId ID của phiên đấu giá
     * @param bidderId ID của người tham gia đấu giá
     * @return true nếu auto-bid đang hoạt động, false nếu không
     */
    public boolean isAutoBidActive(int auctionId, int bidderId) {
        PriorityQueue<AutoBid> queue = queues.get(auctionId);
        if (queue == null) return false;
        return queue.stream()
                .anyMatch(ab -> ab.getBidder().getId() == bidderId && ab.isActive());
    }
}
