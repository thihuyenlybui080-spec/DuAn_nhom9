package org.example.loginregister.server.model.entity.auto_bidding;

import org.example.loginregister.server.common.observer.Observer;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.service.BidService;
import org.example.loginregister.server.util.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoBidAgent implements Observer, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(AutoBidAgent.class);
    private static final ExecutorService autoBidExecutor = Executors.newCachedThreadPool();

    private static final long serialVersionUID = 1L;
    private final Bidder  bidder;
    private final String auctionId;
    private final AutoBidConfig config;
    private boolean active = true;

    public AutoBidAgent(Bidder bidder, Auction auction, AutoBidConfig config) {
        this.bidder = bidder;
        this.auctionId = auction.getId();
        this.config = config;
        auction.addObserver(this);
    }

    @Override
    public void update(String auctionId, double currentPrice, String highestBidder) {
        if (!active) return;

        if (bidder.getName().equals(highestBidder)) return;

        double nextBid = currentPrice + config.getIncrement();

        if (nextBid > config.getMaxBid()) {
            stop();
            return;
        }
        // Execute auto-bid asynchronously to avoid blocking the main bid response
        autoBidExecutor.submit(() -> {
            try {
                Auction currentAuction = AuctionManager.getInstance().getActive(this.auctionId);
                if (currentAuction != null) {
                    // Re-check conditions in case state changed
                    if (active && !bidder.getName().equals(currentAuction.getHighestBidder() != null ? currentAuction.getHighestBidder().getName() : "")) {
                        double currentPriceNow = currentAuction.getCurrentPrice();
                        double nextBidNow = currentPriceNow + config.getIncrement();
                        if (nextBidNow <= config.getMaxBid()) {
                            BidService.getInstance().processAutoBid(bidder, currentAuction, nextBidNow);
                            logger.info("Auto-bid placed: {} bid {} on auction {}", bidder.getName(), nextBidNow, auctionId);
                        } else {
                            logger.info("Auto-bid stopped for {} on auction {}: max bid reached", bidder.getName(), auctionId);
                            stop();
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Auto-bid failed for {} on auction {}: {}", bidder.getName(), auctionId, e.getMessage());
            }
        });
    }

    public void stop() {
        active = false;
        Auction currentAuction = AuctionManager.getInstance().getActive(auctionId);
        if (currentAuction != null) {
            currentAuction.removeObserver(this);
        }
    }
}
