package org.example.loginregister.server.service;

import org.example.loginregister.common.exception.AuctionClosedException;
import org.example.loginregister.common.exception.InvalidBidException;
import org.example.loginregister.server.dao.AuctionDAO;
import org.example.loginregister.server.dao.BidDAO;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.util.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Dịch vụ đặt giá: manual bid, anti-snipe và auto-bid.
 */
public class BidService {

    private static final Logger logger = LoggerFactory.getLogger(BidService.class);
    private static volatile BidService instance;

    private static final long ANTI_SNIPE_THRESHOLD_SECONDS = 30;
    private static final long ANTI_SNIPE_EXTENSION_SECONDS = 60;

    private final AuctionManager auctionManager;
    private final AuctionService auctionService;

    private BidService() {
        this.auctionManager = AuctionManager.getInstance();
        this.auctionService = AuctionService.getInstance();
    }

    /**
     * @return singleton {@link BidService}
     */
    public static BidService getInstance() {
        if (instance == null) {
            synchronized (BidService.class) {
                if (instance == null) {
                    instance = new BidService();
                }
            }
        }
        return instance;
    }

    /**
     * Đặt giá thủ công: xử lý in-memory và lưu DB.
     *
     * @return true nếu đặt giá thành công
     */
    public boolean placeBid(String auctionId, Bidder bidder, double amount)
            throws InvalidBidException, AuctionClosedException {
        Auction auction = auctionManager.getActive(auctionId);
        if (auction == null) {
            logger.warn("placeBid: auction {} not active", auctionId);
            return false;
        }

        boolean success = auction.processBid(bidder, amount);
        if (success) {
            bidder.recordBid(auction.getItem(), amount);
            applyAntiSnipe(auction);
            persistBid(auctionId, bidder, amount);
            logger.info("Bid placed: auction={} bidder={} amount={}", auctionId, bidder.getName(), amount);
        }
        return success;
    }

    /**
     * Đặt giá tự động từ {@link org.example.loginregister.server.model.entity.auto_bidding.AutoBidAgent}.
     */
    public boolean processAutoBid(Bidder bidder, Auction auction, double amount) {
        try {
            return placeBid(auction.getId(), bidder, amount);
        } catch (InvalidBidException | AuctionClosedException e) {
            logger.warn("Auto-bid failed for {} on {}: {}", bidder.getName(), auction.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Gia hạn phiên nếu có bid sát giờ kết thúc (anti-snipe).
     */
    public void applyAntiSnipe(Auction auction) {
        boolean extended = auction.tryExtendForAntiSnipe(
                ANTI_SNIPE_THRESHOLD_SECONDS, ANTI_SNIPE_EXTENSION_SECONDS);
        if (extended) {
            auctionService.scheduleEnd(auction, ANTI_SNIPE_EXTENSION_SECONDS);
            logger.info("Anti-snipe extended auction {} by {}s", auction.getId(), ANTI_SNIPE_EXTENSION_SECONDS);
        }
    }

    public List<BidTransaction> getBidsByAuction(String auctionId){
        int auctionDbId = AuctionDAO.parseDbId(auctionId);
        List<BidTransaction> bidTransactionList = BidDAO.getBidsByAuction(auctionDbId);
        return  bidTransactionList;

    }
    private void persistBid(String auctionId, Bidder bidder, double amount) {
        int auctionDbId = AuctionDAO.parseDbId(auctionId);
        int bidderId = AuctionDAO.parseDbId(bidder.getId());
        if (auctionDbId > 0 && bidderId > 0) {
            BidDAO.insertBid(auctionDbId, bidderId, amount);
            AuctionDAO.updateAuctionBid(auctionDbId, amount, bidderId);
        }
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
}
