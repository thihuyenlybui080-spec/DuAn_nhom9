package org.example.loginregister.server.util;

import org.example.loginregister.common.exception.AuctionClosedException;
import org.example.loginregister.common.exception.InvalidBidException;
import org.example.loginregister.server.dao.AuctionDAO;
import org.example.loginregister.server.dao.BidDAO;
import org.example.loginregister.server.dao.ItemDAO;
import org.example.loginregister.server.dao.UserDAO;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionResult;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;
import org.example.loginregister.server.model.entity.user.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * AuctionManager – Singleton quản lý phiên đấu giá.
 * Kết hợp in-memory (ConcurrentHashMap) để xử lý real-time
 * và AuctionDAO để đồng bộ với MySQL.
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
    private final Map<String, Auction> activeAuctions;
    private final ScheduledExecutorService scheduler;

    private static final long ANTI_SNIPE_THRESHOLD_SECONDS = 30;
    private static final long ANTI_SNIPE_EXTENSION_SECONDS = 60;
    private static final long PAYMENT_DEADLINE_SECONDS = 24 * 60 * 60;

    // ===== PUBLIC API =====

    /**
     * Tạo auction mới: lưu item + auction vào DB, rồi lên lịch mở.
     */
    public void startAuction(String auctionId, Seller seller, Item item) {
        LocalDateTime now = LocalDateTime.now();
        long startDelay = ChronoUnit.SECONDS.between(now, item.getStartTime());
        long endDelay   = ChronoUnit.SECONDS.between(now, item.getEndTime());

        if (item.getEndTime().isBefore(item.getStartTime())) {
            System.err.println("End time must be after start time!");
            return;
        }
        if (endDelay <= 0) {
            System.err.println("Auction end time is in the past!");
            return;
        }

        // Lấy sellerId từ DB id
        int sellerId = AuctionDAO.parseDbId(seller.getId());

        // Lưu item vào DB
        int itemDbId = ItemDAO.insertItem(item, sellerId);
        if (itemDbId > 0) {
            item.setId("item-" + itemDbId);
        }

        // Lưu auction vào DB
        long durationSeconds = ChronoUnit.SECONDS.between(item.getStartTime(), item.getEndTime());
        long endTimeMillis   = item.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        int auctionDbId = AuctionDAO.insertAuction(itemDbId, item.getStartingPrice(), durationSeconds, endTimeMillis);

        // Tạo Auction object
        Auction auction = new Auction(seller, item);
        if (auctionDbId > 0) {
            auction.setId("auction-" + auctionDbId);
        }

        if (startDelay <= 0) {
            openAuction(auction);
        } else {
            scheduler.schedule(() -> openAuction(auction), startDelay, TimeUnit.SECONDS);
            System.out.println("Auction " + auction.getId() + " scheduled to open in " + startDelay + "s");
        }
    }

    private void openAuction(Auction auction) {
        activeAuctions.put(auction.getId(), auction);
        System.out.println("Auction " + auction.getId() + " is now OPEN for bidding!");
        auction.notifyObservers();

        long endDelay = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getItem().getEndTime());
        scheduleEnd(auction, endDelay);
    }

    /**
     * Đặt giá: xử lý in-memory + lưu vào DB.
     */
    public boolean placeBid(String auctionId, Bidder bidder, double amount) throws InvalidBidException, AuctionClosedException {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) {
            System.err.println("Error: Auction session does not exist or has already ended!");
            return false;
        }

        boolean success = auction.processBid(bidder, amount);

        if (success) {
            bidder.recordBid(auction.getItem(), amount);
            tryAntiSnipe(auction);

            // Lưu bid vào DB
            int auctionDbId = AuctionDAO.parseDbId(auctionId);
            int bidderId    = AuctionDAO.parseDbId(bidder.getId());
            if (auctionDbId > 0 && bidderId > 0) {
                BidDAO.insertBid(auctionDbId, bidderId, amount);
                AuctionDAO.updateAuctionBid(auctionDbId, amount, bidderId);
            }
        }

        return success;
    }

    public void removeAuction(String auctionId) {
        activeAuctions.remove(auctionId);
        int dbId = AuctionDAO.parseDbId(auctionId);
        if (dbId > 0) {
            AuctionDAO.updateAuctionStatus(dbId, AuctionStatus.CANCELED);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    public void cancelAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

        auction.setStatus(AuctionStatus.CANCELED);

        // Cập nhật DB
        int dbId = AuctionDAO.parseDbId(auctionId);
        if (dbId > 0) {
            AuctionDAO.updateAuctionStatus(dbId, AuctionStatus.CANCELED);
        }

        AuctionResult result = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(result);

        activeAuctions.remove(auctionId);
    }

    /** Kết thúc phiên đấu giá, lưu kết quả vào DB. */
    public void endAuction(String auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction == null) return;

        AuctionStatus finalStatus = (auction.getHighestBidder() != null)
                ? AuctionStatus.FINISHED : AuctionStatus.CANCELED;

        auction.finishAuction(finalStatus);

        // Cập nhật status auction trong DB
        int auctionDbId = AuctionDAO.parseDbId(auctionId);
        if (auctionDbId > 0) {
            AuctionDAO.updateAuctionStatus(auctionDbId, finalStatus);

            // Ghi bid_transaction nếu có người thắng
            if (finalStatus == AuctionStatus.FINISHED && auction.getHighestBidder() != null) {
                int bidderId = AuctionDAO.parseDbId(auction.getHighestBidder().getId());
                int itemDbId = AuctionDAO.parseDbId(auction.getItem().getId());
                if (bidderId > 0 && itemDbId > 0) {
                    BidDAO.insertBidTransaction(auctionDbId, bidderId, itemDbId, auction.getCurrentPrice());
                }
            }
        }

        AuctionResult result = new AuctionResult(auction);
        AuctionHistoryManager.getInstance().saveResult(result);

        activeAuctions.remove(auctionId);

        if (finalStatus == AuctionStatus.FINISHED) {
            schedulePaymentDeadline(auctionId);
        }
    }

    // ===== GETTER =====

    /** Lấy danh sách auction đang chạy từ DB. */
    public List<Auction> getActiveAuctions() {
        List<User> allUsers = UserDAO.getAllUsers();
        return AuctionDAO.getActiveAuctions(allUsers);
    }

    public Auction getAuction(String auctionId) {
        Auction a = activeAuctions.get(auctionId);
        if (a != null) return a;
        List<User> allUsers = UserDAO.getAllUsers();
        return AuctionDAO.getAllAuctions(allUsers).stream()
                .filter(au -> au.getId().equals(auctionId))
                .findFirst().orElse(null);
    }

    /** Lấy tất cả auction từ DB (Admin dùng). */
    public List<Auction> getAllAuctions() {
        List<User> allUsers = UserDAO.getAllUsers();
        return AuctionDAO.getAllAuctions(allUsers);
    }

    /** Lấy auction theo seller từ DB. */
    public List<Auction> getAuctionsBySeller(String sellerId) {
        if (sellerId == null) return Collections.emptyList();
        int dbId = AuctionDAO.parseDbId(sellerId);
        if (dbId < 0) {
            // sellerId là số nguyên trực tiếp từ DB
            try { dbId = Integer.parseInt(sellerId); } catch (NumberFormatException e) { return Collections.emptyList(); }
        }
        List<User> allUsers = UserDAO.getAllUsers();
        return AuctionDAO.getAuctionsBySeller(dbId, allUsers);
    }

    /** Lấy item theo seller từ DB. */
    public List<Item> getItemsBySeller(String sellerId) {
        if (sellerId == null) return Collections.emptyList();
        int dbId = AuctionDAO.parseDbId(sellerId);
        if (dbId < 0) {
            try { dbId = Integer.parseInt(sellerId); } catch (NumberFormatException e) { return Collections.emptyList(); }
        }
        // Cần Seller object để map, tạo dummy seller với id
        Seller dummy = new Seller("", "", "", "");
        dummy.setId(sellerId);
        return ItemDAO.getItemsBySeller(dbId, dummy);
    }

    /** Lấy toàn bộ user từ DB (Admin dùng). */
    public List<User> getAllUsers() {
        return UserDAO.getAllUsers();
    }

    // ===== PRIVATE HELPERS =====

    private void tryAntiSnipe(Auction auction) {
        boolean extended = auction.tryExtendForAntiSnipe(ANTI_SNIPE_THRESHOLD_SECONDS, ANTI_SNIPE_EXTENSION_SECONDS);
        if (extended) {
            scheduleEnd(auction, ANTI_SNIPE_EXTENSION_SECONDS);
        }
    }

    private void scheduleEnd(Auction auction, long delaySeconds) {
        ScheduledFuture<?> timer = scheduler.schedule(
                () -> endAuction(auction.getId()), delaySeconds, TimeUnit.SECONDS);
        auction.setTimer(timer);
    }

    private void schedulePaymentDeadline(String auctionId) {
        AuctionHistoryManager ahm = AuctionHistoryManager.getInstance();
        AuctionResult result = ahm.getResult(auctionId);
        if (result == null) return;

        scheduler.schedule(() -> {
            AuctionResult r = ahm.getResult(auctionId);
            if (r != null && r.getStatus() == AuctionStatus.FINISHED) {
                ahm.updateStatus(auctionId, AuctionStatus.CANCELED);
                int dbId = AuctionDAO.parseDbId(auctionId);
                if (dbId > 0) AuctionDAO.updateAuctionStatus(dbId, AuctionStatus.CANCELED);
            }
        }, PAYMENT_DEADLINE_SECONDS, TimeUnit.SECONDS);

        System.out.println("[PaymentDeadline] Auction " + auctionId + " - winner has 1 day to complete payment.");
    }
    public static synchronized void resetForTesting() {
        if (instance != null) {
            if (instance.scheduler != null && !instance.scheduler.isShutdown()) {
                instance.scheduler.shutdownNow();
            }
            if (instance.activeAuctions != null) {
                instance.activeAuctions.clear();
            }

            instance = null;
        }
    }
}
