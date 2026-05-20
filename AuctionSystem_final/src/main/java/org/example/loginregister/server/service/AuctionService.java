package org.example.loginregister.server.service;

import org.example.loginregister.server.dao.AuctionDAO;
import org.example.loginregister.server.dao.AutoBidDAO;
import org.example.loginregister.server.dao.ItemDAO;
import org.example.loginregister.server.dao.UserDAO;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionResult;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.auto_bidding.AutoBidConfig;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.example.loginregister.server.model.entity.user.User;
import org.example.loginregister.server.util.AuctionHistoryManager;
import org.example.loginregister.server.util.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
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

        int sellerId = AuctionDAO.parseDbId(item.getSellerId());
        int itemDbId = ItemDAO.insertItem(item, sellerId);
        if (itemDbId > 0) {
            item.setId("item-" + itemDbId);
        }

        long durationSeconds = ChronoUnit.SECONDS.between(item.getStartTime(), item.getEndTime());
        int auctionDbId = AuctionDAO.insertAuction(itemDbId, item.getStartingPrice(),
                durationSeconds, item.getEndTime());

        Auction auction = new Auction(item);
        if (auctionDbId > 0) {
            auction.setId("auction-" + auctionDbId);
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
        logger.info("Auction {} is now OPEN for bidding", auction.getId());
        auction.notifyObservers();

        long endDelay = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getItem().getEndTime());
        scheduleEnd(auction, endDelay);
    }

    /**
     * Kết thúc phiên đấu giá, đồng bộ DB và lưu kết quả.
     *
     * @param auctionId id phiên đấu giá
     */
    public Auction endAuction(String auctionId) {
        Auction auction = auctionManager.getActive(auctionId);
        if (auction == null) {
            logger.warn("endAuction: auction {} not found in memory", auctionId);
        }

        AuctionStatus finalStatus = (auction.getHighestBidder() != null)
                ? AuctionStatus.FINISHED : AuctionStatus.CANCELED;

        auction.finishAuction(finalStatus);

        int auctionDbId = AuctionDAO.parseDbId(auctionId);
        if (auctionDbId > 0) {
            AuctionDAO.updateAuctionStatus(auctionDbId, finalStatus);
            if (finalStatus == AuctionStatus.FINISHED && auction.getHighestBidder() != null) {
                int bidderId = AuctionDAO.parseDbId(auction.getHighestBidder().getId());
                int itemDbId = AuctionDAO.parseDbId(auction.getItem().getId());
                if (bidderId > 0 && itemDbId > 0) {
                    org.example.loginregister.server.dao.BidDAO.insertBidTransaction(
                            auctionDbId, bidderId, itemDbId, auction.getCurrentPrice());
                }
            }
        }

        AuctionResult result = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(result);
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
    public void cancelAuction(String auctionId) {
        Auction auction = auctionManager.getActive(auctionId);
        if (auction == null) {
            auction = AuctionDAO.getAuctionById(auctionId);
            
            if (auction == null) {
                logger.warn("cancelAuction: auction {} not found in DB or memory", auctionId);
                return;
            }
            int dbId = AuctionDAO.parseDbId(auctionId);
            if (dbId > 0) {
                AuctionDAO.updateAuctionStatus(dbId, AuctionStatus.CANCELED);
            }

            AuctionManager.getInstance().putActive(auction);
            AuctionResult result = new AuctionResult(auction);
            AuctionHistoryManager.getInstance().saveResult(result);
            logger.info("Auction {} canceled (from DB)", auctionId);
            return;
        }

        auction.setStatus(AuctionStatus.CANCELED);
        int dbId = AuctionDAO.parseDbId(auctionId);
        if (dbId > 0) {
            AuctionDAO.updateAuctionStatus(dbId, AuctionStatus.CANCELED);
        }

        AuctionResult result = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(result);
        auctionManager.removeActive(auctionId);
        logger.info("Auction {} canceled", auctionId);
    }

    /**
     * Gỡ phiên khỏi bộ nhớ (chưa chạy hoặc seller xóa item).
     */
    public void removeAuction(String auctionId) {
        auctionManager.removeActive(auctionId);
        int dbId = AuctionDAO.parseDbId(auctionId);
        if (dbId > 0) {
            AuctionDAO.updateAuctionStatus(dbId, AuctionStatus.CANCELED);
        }
        logger.debug("Auction {} removed from active map", auctionId);
    }

    /**
     * Lên lịch (hoặc gia hạn) thời điểm kết thúc phiên.
     */
    public void scheduleEnd(Auction auction, long delaySeconds) {
        ScheduledFuture<?> timer = auctionManager.getScheduler().schedule(
                () -> endAuction(auction.getId()), delaySeconds, TimeUnit.SECONDS);
        auction.setTimer(timer);
    }

    /** Lấy danh sách auction đang chạy từ DB và đăng ký vào AuctionManager. */
    public List<Auction> getActiveAuctions() {
        List<User> allUsers = UserDAO.getAllUsers();
        List<Auction> auctions = AuctionDAO.getActiveAuctions(allUsers);
        // Register auctions in AuctionManager for in-memory access
        for (Auction auction : auctions) {
            auctionManager.putActive(auction);
            // Restore auto-bid configurations from database
            int auctionDbId = AuctionDAO.parseDbId(auction.getId());
            if (auctionDbId > 0) {
                allUsers.stream()
                        .filter(u -> u instanceof Bidder)
                        .map(u -> (Bidder) u)
                        .forEach(bidder -> {
                            int bidderDbId = AuctionDAO.parseDbId(bidder.getId());
                            if (bidderDbId > 0) {
                                AutoBidConfig config =
                                AutoBidDAO.getAutoBidConfig(auctionDbId, bidderDbId);
                                if (config != null) {
                                    bidder.enableAutoBid(auction, config);
                                    logger.debug("Restored auto-bid for bidder {} on auction {} (maxBid={}, increment={})",
                                            bidder.getName(), auction.getId(), config.getMaxBid(), config.getIncrement());
                                }
                            }
                        });
            }
        }
        logger.info("Loaded and registered {} active auctions from database", auctions.size());
        return auctions;
    }

    /**
     * Lấy auction: ưu tiên in-memory, fallback DB.
     */
    public Auction getAuction(String auctionId) {
        Auction auction = AuctionDAO.getAuctionById(auctionId);
        return auction;
    }

    /** Lấy tất cả auction từ DB (admin). */
    public List<Auction> getAllAuctions() {
        List<User> allUsers = UserDAO.getAllUsers();
        return AuctionDAO.getAllAuctions(allUsers);
    }

    /** Lấy auction theo seller từ DB. */
    public List<Auction> getAuctionsBySeller(String sellerId) {
        if (sellerId == null) {
            return Collections.emptyList();
        }
        int dbId = AuctionDAO.parseDbId(sellerId);
        if (dbId < 0) {
            try {
                dbId = Integer.parseInt(sellerId);
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }
        }
        List<User> allUsers = UserDAO.getAllUsers();
        return AuctionDAO.getAuctionsBySeller(dbId, allUsers);
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
}
