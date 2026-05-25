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
import vn.edu.vnu.auction.util.Utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

        int sellerId = Utils.parseDbId(item.getSellerId());
        int itemDbId = ItemDAO.insertItem(item, sellerId);
        if (itemDbId <= 0) {
            logger.error("Failed to insert item {} into database for seller {}", item.getItemName(), sellerId);
            throw new RuntimeException("Failed to create item in database");
        }
        item.setId("item-" + itemDbId);

        long durationSeconds = ChronoUnit.SECONDS.between(item.getStartTime(), item.getEndTime());
        int auctionDbId = AuctionDAO.insertAuction(itemDbId, item.getStartingPrice(),
                durationSeconds, item.getEndTime());

        if (auctionDbId <= 0) {
            logger.error("Failed to insert auction for item {} into database", item.getItemName());
            throw new RuntimeException("Failed to create auction in database");
        }

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
        auction.setStatus(AuctionStatus.RUNNING);
        
        // Update database status when auction transitions to RUNNING
        int auctionDbId = Utils.parseDbId(auction.getId());
        if (auctionDbId > 0) {
            AuctionDAO.updateAuctionStatus(auctionDbId, AuctionStatus.RUNNING);
        }
        
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
    public Auction endAuction(String auctionId, boolean forced) {
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

        int auctionDbId = Utils.parseDbId(auctionId);
        if (auctionDbId > 0) {
            AuctionDAO.updateAuctionStatus(auctionDbId, finalStatus);
            if (finalStatus == AuctionStatus.FINISHED && auction.getHighestBidder() != null) {
                int bidderId = Utils.parseDbId(auction.getHighestBidder().getId());
                int itemDbId = Utils.parseDbId(auction.getItem().getId());
                if (bidderId > 0 && itemDbId > 0) {
                    BidDAO.insertBidTransaction(auctionDbId, bidderId, itemDbId, auction.getCurrentPrice());
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
            int dbId = Utils.parseDbId(auctionId);
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
        int dbId = Utils.parseDbId(auctionId);
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
        int dbId = Utils.parseDbId(auctionId);
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
                () -> endAuction(auction.getId(), false), delaySeconds, TimeUnit.SECONDS);
        auction.setTimer(timer);
    }

    /** Lấy danh sách auction đang chạy từ DB và đăng ký vào AuctionManager. */
    public List<Auction> getActiveAuctions() {
        List<User> allUsers = UserDAO.getAllUsers();
        List<Auction> auctions = AuctionDAO.getActiveAuctions(allUsers);
        Map<String, AutoBidConfig> autoBidConfigConcurrentHashMap = AutoBidDAO.getAllAutoBidConfig();

        // Register auctions in AuctionManager for in-memory access
        for (Auction auction : auctions) {
            auctionManager.putActive(auction);

            // Check if auction has already ended and reschedule or end it
            LocalDateTime now = LocalDateTime.now();
            long endDelay = ChronoUnit.SECONDS.between(now, auction.getItem().getEndTime());
            long startDelay = ChronoUnit.SECONDS.between(now, auction.getItem().getStartTime());

            if (endDelay <= 0) {
                // Auction has already ended, update status
                logger.info("Auction {} has expired, ending it now", auction.getId());
                endAuction(auction.getId(), false);
            } else if (startDelay <= 0) {
                auction.setStatus(AuctionStatus.RUNNING);
                
                // Update database status when auction transitions to RUNNING
                int auctionDbId = Utils.parseDbId(auction.getId());
                if (auctionDbId > 0) {
                    AuctionDAO.updateAuctionStatus(auctionDbId, AuctionStatus.RUNNING);
                }
                
                logger.info("Auction {} is now RUNNING (live) for bidding", auction.getId());
                auction.notifyObservers();
                scheduleEnd(auction, endDelay);
                logger.info("Rescheduled end timer for auction {} in {}s", auction.getId(), endDelay);
            } else {
                auctionManager.getScheduler().schedule(
                        () -> openAuction(auction), startDelay, TimeUnit.SECONDS);
                logger.info("Auction {} scheduled to open in {}s", auction.getId(), startDelay);
            }
        }

        // Restore auto-bid configurations from database AFTER all auctions are registered
        // This ensures we get the auction instance from AuctionManager (in-memory)
        if (auctions == null || auctions.size() == 0) return auctions;
        int auctionDbId = Utils.parseDbId(auctions.get(0).getId());
        if (auctionDbId > 0) {
            for (User user : allUsers) {
                if (user instanceof Bidder) {
                    Bidder bidder = (Bidder) user;
                    int bidderDbId = Utils.parseDbId(bidder.getId());
                    if (bidderDbId > 0) {
                        for (Auction auction : auctions) {
                            auctionDbId = Utils.parseDbId(auction.getId());
                            AutoBidConfig config = autoBidConfigConcurrentHashMap.get(auctionDbId + "-" + bidderDbId);
                            if (config != null) {
                                // Get the auction from AuctionManager to ensure we use the in-memory instance
                                Auction inMemoryAuction = auctionManager.getActive(auction.getId());
                                if (inMemoryAuction != null) {
                                    //bidder.enableAutoBid(inMemoryAuction, config);
                                    logger.debug("Restored auto-bid for bidder {} on auction {} (maxBid={}, increment={})",
                                            bidder.getName(), auction.getId(), config.getMaxBid(), config.getIncrement());
                                }
                            }
                        }
                    }
                }
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
        if (auction != null) {
            // Check if there's an in-memory instance in AuctionManager
            Auction inMemoryAuction = auctionManager.getActive(auctionId);
            if (inMemoryAuction != null) {
                // Use the in-memory instance to preserve observer registrations
                auction = inMemoryAuction;
            } else {
                // If no in-memory instance, put this one in the manager
                auctionManager.putActive(auction);
                // Re-register auto-bid agents for this auction
                List<User> allUsers = UserDAO.getAllUsers();
                Map<String, AutoBidConfig> autoBidConfigConcurrentHashMap = AutoBidDAO.getAllAutoBidConfig();
                int auctionDbId = Utils.parseDbId(auctionId);
                if (auctionDbId > 0) {
                    for (User user : allUsers) {
                        if (user instanceof Bidder) {
                            Bidder bidder = (Bidder) user;
                            int bidderDbId = Utils.parseDbId(bidder.getId());
                            if (bidderDbId > 0) {
                                AutoBidConfig config = autoBidConfigConcurrentHashMap.get(auctionDbId + "-" + bidderDbId);
                                if (config != null) {
                                    //bidder.enableAutoBid(auction, config);
                                    logger.debug("Re-registered auto-bid for bidder {} on auction {} (maxBid={}, increment={})",
                                            bidder.getName(), auction.getId(), config.getMaxBid(), config.getIncrement());
                                }
                            }
                        }
                    }
                }
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
    public List<Auction> getAuctionsBySeller(String sellerId) {
        if (sellerId == null) {
            return Collections.emptyList();
        }
        int dbId = Utils.parseDbId(sellerId);
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
