package vn.edu.vnu.auction.service;

import vn.edu.vnu.auction.dao.*;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.auto_bidding.AutoBidConfig;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.model.entity.user.User;
import vn.edu.vnu.auction.util.AuctionHistoryManager;
import vn.edu.vnu.auction.util.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Dịch vụ vòng đời phiên đấu giá: tạo, mở, kết thúc, hủy và truy vấn từ DB.
 * Thao tác in-memory thông qua {@link AuctionManager}.
 */
public class AuctionService {

    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    private static volatile AuctionService instance;

    private final AuctionManager auctionManager;
    private final PaymentService paymentService;

    private AuctionService() {
        this.auctionManager = AuctionManager.getInstance();
        this.paymentService = PaymentService.getInstance();
    }

    /**
     * @return singleton {@link AuctionService}
     */
    public static AuctionService getInstance() {
        if (instance == null) {
            synchronized (AuctionService.class) {
                if (instance == null) {
                    instance = new AuctionService();
                }
            }
        }
        return instance;
    }

    /**
     * Tạo auction mới: lưu item + auction vào DB, rồi lên lịch mở phiên.
     * @param item      sản phẩm đấu giá
     */

    public Auction startAuction(Item item) {
        LocalDateTime now = LocalDateTime.now();
        long startDelay = ChronoUnit.SECONDS.between(now, item.getStartTime());
        long endDelay = ChronoUnit.SECONDS.between(now, item.getEndTime());

        if (item.getEndTime().isBefore(item.getStartTime())) {
            logger.error("End time must be after start time for item {}", item.getItemName());
        }
        if (endDelay <= 0) {
            logger.error("Auction end time is in the past for item {}", item.getItemName());
        }

        int sellerId = item.getSellerId();
        int itemDbId = ItemDAO.insertItem(item, sellerId);
        if (itemDbId <= 0) {
            logger.error("Failed to insert item {} into database for seller {}", item.getItemName(), sellerId);
            throw new RuntimeException("Failed to create item in database");
        }
        item.setId(itemDbId);

        long durationSeconds = ChronoUnit.SECONDS.between(item.getStartTime(), item.getEndTime());
        int auctionDbId = AuctionDAO.insertAuction(itemDbId, item.getStartingPrice(),
                durationSeconds, item.getEndTime());

        if (auctionDbId <= 0) {
            logger.error("Failed to insert auction for item {} into database", item.getItemName());
            throw new RuntimeException("Failed to create auction in database");
        }

        Auction auction = new Auction(item);
        if (auctionDbId > 0) {
            auction.setId(auctionDbId);
        }

        if (startDelay <= 0) {
            openAuction(auction);
        } else {
            auctionManager.getScheduler().schedule(
                    () -> openAuction(auction), startDelay, TimeUnit.SECONDS);
            logger.info("Auction {} scheduled to open in {}s", auction.getId(), startDelay);
        }
        return auction;
    }

    /**
     * Đưa phiên vào bộ nhớ active và lên lịch kết thúc.
     */
    public void openAuction(Auction auction) {
        auctionManager.putActive(auction);
        auction.setStatus(AuctionStatus.RUNNING);
        int auctionId = auction.getId();
        if (auctionId > 0) {
            AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.RUNNING);
        }

        auction.addObserver(AutobidService.getInstance());
        logger.info("Registered AutobidService as observer for auction {}", auction.getId());

        logger.info("Auction {} is now RUNNING (live) for bidding", auction.getId());
        auction.notifyObservers();

        long endDelay = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getItem().getEndTime());
        scheduleEnd(auction, endDelay);
    }

    /**
     * Kết thúc phiên đấu giá, đồng bộ DB và lưu kết quả.
     *
     * @param auctionId id phiên đấu giá
     * @param forced true nếu admin force end, false nếu kết thúc tự nhiên
     */
    public Auction endAuction(int auctionId, boolean forced) {
        Auction auction = auctionManager.getActive(auctionId);
        if (auction == null) {
            logger.warn("endAuction: auction {} not found in memory", auctionId);
        }

        // Luôn dùng FINISHED cho cả force end và kết thúc tự nhiên
        AuctionStatus finalStatus = AuctionStatus.FINISHED;
        if (forced) {
            logger.info("Force ending auction {}", auctionId);
        } else {
            logger.info("Naturally ending auction {}", auctionId);
        }

        auction.finishAuction(finalStatus);

        if (auctionId > 0) {
            AuctionDAO.updateAuctionStatus(auctionId, finalStatus);
            if (finalStatus == AuctionStatus.FINISHED && auction.getHighestBidder() != null) {
                int bidderId = auction.getHighestBidder().getId();
                int itemDbId = auction.getItem().getId();
                if (bidderId > 0 && itemDbId > 0) {
                    BidDAO.insertBidTransaction(auctionId, bidderId, itemDbId, auction.getCurrentPrice());
                }
            }
        }

        AuctionResult result = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(result);

        auction.removeObserver(AutobidService.getInstance());
        AutobidService.getInstance().clearAuction(auctionId);
        logger.info("Removed AutobidService observer and cleared auto-bids for auction {}", auctionId);

        auctionManager.removeActive(auctionId);

        if (finalStatus == AuctionStatus.FINISHED) {
            paymentService.schedulePaymentDeadline(auctionId);
        }
        logger.info("Auction {} ended with status {}", auctionId, finalStatus);
        return auction;
    }

    /**
     * Hủy phiên đang chạy (admin / seller / ban seller).
     * Tìm phiên nếu trên Ram không có, lấy từ database
     */
    public void cancelAuction(int auctionId) {
        Auction auction = auctionManager.getActive(auctionId);
        if (auction == null) {
            auction = AuctionDAO.getAuctionById(auctionId);

            if (auction == null) {
                logger.warn("cancelAuction: auction {} not found in DB or memory", auctionId);
                return;
            }
            if (auctionId > 0) {
                AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.CANCELED);
            }

            AuctionManager.getInstance().putActive(auction);
            AuctionResult result = new AuctionResult(auction);
            AuctionHistoryManager.getInstance().saveResult(result);
            logger.info("Auction {} canceled (from DB)", auctionId);
            return;
        }

        auction.setStatus(AuctionStatus.CANCELED);
        if (auctionId > 0) {
            AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.CANCELED);
        }

        AuctionResult result = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(result);
        auctionManager.removeActive(auctionId);
        logger.info("Auction {} canceled", auctionId);
    }

    /**
     * Gỡ phiên khỏi bộ nhớ (chưa chạy hoặc seller xóa item).
     */
    public void removeAuction(int auctionId) {
        auctionManager.removeActive(auctionId);
        if (auctionId > 0) {
            AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.CANCELED);
        }
        logger.debug("Auction {} removed from active map", auctionId);
    }

    /**
     * Lên lịch (hoặc gia hạn) thời điểm kết thúc phiên.
     */
    public void scheduleEnd(Auction auction, long delaySeconds) {
        ScheduledFuture<?> timer = auctionManager.getScheduler().schedule(
                () -> endAuction(auction.getId(), false), delaySeconds, TimeUnit.SECONDS);
        auction.setTimer(timer);
    }

    /** Lấy danh sách auction đang chạy từ DB và đăng ký vào AuctionManager. */
    public List<Auction> getActiveAuctions() {
        List<User> allUsers = UserDAO.getAllUsers();
        List<Auction> auctions = AuctionDAO.getActiveAuctions(allUsers);
        for (Auction auction : auctions) {
            auctionManager.putActive(auction);

            LocalDateTime now = LocalDateTime.now();
            long endDelay = ChronoUnit.SECONDS.between(now, auction.getItem().getEndTime());
            long startDelay = ChronoUnit.SECONDS.between(now, auction.getItem().getStartTime());

            if (endDelay <= 0) {
                logger.info("Auction {} has expired, ending it now", auction.getId());
                endAuction(auction.getId(), false);
            } else if (startDelay <= 0) {
                auction.setStatus(AuctionStatus.RUNNING);
                int auctionId = auction.getId();
                if (auctionId > 0) {
                    AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.RUNNING);
                }
                
                logger.info("Auction {} is now RUNNING (live) for bidding", auction.getId());
                auction.notifyObservers();
                scheduleEnd(auction, endDelay);
                logger.info("Rescheduled end timer for auction {} in {}s", auction.getId(), endDelay);
                restoreAutoBidAgents(auction, allUsers);
            } else {
                auctionManager.getScheduler().schedule(
                        () -> openAuction(auction), startDelay, TimeUnit.SECONDS);
                logger.info("Auction {} scheduled to open in {}s", auction.getId(), startDelay);
            }
        }
        logger.info("Loaded and registered {} active auctions from database", auctions.size());
        return auctions;
    }

    private void restoreAutoBidAgents(Auction auction, List<User> allUsers){
        try{
            Map<Integer, AutoBidConfig> configs = AutoBidDAO.getAutoBidsByAuction(auction.getId());
            for(Map.Entry<Integer, AutoBidConfig> entry : configs.entrySet()){
                int bidderId = entry.getKey();
                AutoBidConfig config = entry.getValue();
                User user = UserDAO.getUserById(bidderId);
                if(user instanceof Bidder){
                    AutobidService.getInstance().enableAutoBid((Bidder) user,auction, config);
                    logger.info("Restored auto-bid: bidder={}, auction={}",
                            user.getName(), auction.getId());
                }
            }
            auction.addObserver(AutobidService.getInstance());
            logger.info("Registered AutobidService as observer for auction {}", auction.getId());
        }catch (Exception e) {
            logger.error("Failed to restore auto-bid agents for auction {}: {}",
                    auction.getId(), e.getMessage());
        }
    }
    public List<AuctionResult> getWonAuctions(int bidderId) {
        if (bidderId <= 0) {
            return Collections.emptyList();
        }
        List<User> allUsers = UserDAO.getAllUsers();
        List<Auction> wonAuctionsList = AuctionDAO.getWonAuctionsByBidder(bidderId, allUsers);
        List<AuctionResult> results = new ArrayList<>();
        for(Auction auction : wonAuctionsList){
            results.add(new AuctionResult(auction));
        }
        return results;
    }


    /**
     * Lấy auction: ưu tiên in-memory, fallback DB.
     */
    public Auction getAuction(int auctionId) {
        Auction auction = AuctionDAO.getAuctionById(auctionId);
        if (auction != null) {
            Auction inMemoryAuction = auctionManager.getActive(auctionId);
            if (inMemoryAuction != null) {
                auction = inMemoryAuction;
            } else {
                auctionManager.putActive(auction);
            }
        }
        return auction;
    }

    /** Lấy tất cả auction từ DB (admin). */
    public List<Auction> getAllAuctions() {
        List<User> allUsers = UserDAO.getAllUsers();
        return AuctionDAO.getAllAuctions(allUsers);
    }

    /** Lấy auction theo seller từ DB. */
    public List<Auction> getAuctionsBySeller(int sellerId) {
        if (sellerId < 0) {
            return Collections.emptyList();
        }
        List<User> allUsers = UserDAO.getAllUsers();
        return AuctionDAO.getAuctionsBySeller(sellerId, allUsers);
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
}
