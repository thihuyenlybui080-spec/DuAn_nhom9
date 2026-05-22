package org.example.loginregister.server.service;

import org.example.loginregister.server.common.exception.AuctionClosedException;
import org.example.loginregister.server.common.exception.InvalidBidException;
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
     * Đặt giá thủ công: xử lý in-memory và lưu DB với transaction.
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


        double previousPrice = auction.getCurrentPrice();
        var previousBidder = auction.getHighestBidder();
        int previousBidCount = auction.getBids().size();

        BidTransaction bidTx = new BidTransaction(bidder, auction.getItem(), amount);
        auction.placeBid(bidTx, false);

        bidder.recordBid(auction.getItem(), amount);
        applyAntiSnipe(auction);
        boolean dbSuccess = persistBid(auctionId, bidder, amount);
        if (!dbSuccess) {
            logger.error("placeBid: Database persist failed for auctionId={}, bidder={}, amount={}", auctionId, bidder.getName(), amount);
            auction.setCurrentPrice(previousPrice);
            auction.setHighestBidder(previousBidder);
            // Remove the last bid that was added
            if (auction.getBids().size() > previousBidCount) {
                auction.getBids().remove(auction.getBids().size() - 1);
            }
            throw new InvalidBidException("Failed to persist bid to database. Please try again.");
        }

        auction.notifyObservers();
        logger.info("Bid placed successfully: auction={} bidder={} amount={}", auctionId, bidder.getName(), amount);
        return true;
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
            long newEndDelay = auction.getSecondsRemaining();
            auctionService.scheduleEnd(auction, newEndDelay);
            logger.info("Anti-snipe extended auction {} by {}s, rescheduled end in {}s", auction.getId(), ANTI_SNIPE_EXTENSION_SECONDS, newEndDelay);
        }
    }

    public List<BidTransaction> getBidsByAuction(String auctionId){
        int auctionDbId = AuctionDAO.parseDbId(auctionId);
        List<BidTransaction> bidTransactionList = BidDAO.getBidsByAuction(auctionDbId);
        return  bidTransactionList;

    }
    private boolean persistBid(String auctionId, Bidder bidder, double amount) {
        int auctionDbId = AuctionDAO.parseDbId(auctionId);
        int bidderId = AuctionDAO.parseDbId(bidder.getId());
        logger.info("[BidService] persistBid START: auctionId={} -> auctionDbId={}, bidderId={} -> bidderDbId={}, amount={}",
                auctionId, auctionDbId, bidder.getId(), bidderId, amount);
        if (auctionDbId > 0 && bidderId > 0) {
            boolean result = BidDAO.insertBid(auctionDbId, bidderId, amount);
            logger.info("[BidService] persistBid END: auctionDbId={}, result={}", auctionDbId, result);
            return result;
        } else {
            logger.error("[BidService] persistBid FAILED: Invalid IDs - auctionDbId= {}, bidderDbId={}", auctionDbId, bidderId);
            return false;
        }
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
}
