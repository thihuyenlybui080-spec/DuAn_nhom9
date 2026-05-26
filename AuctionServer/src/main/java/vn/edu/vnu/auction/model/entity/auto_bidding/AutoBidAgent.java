package vn.edu.vnu.auction.model.entity.auto_bidding;

import vn.edu.vnu.auction.common.observer.Observer;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.service.BidService;
import vn.edu.vnu.auction.util.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoBidAgent implements Observer, Serializable {

    private static final Logger logger = LoggerFactory.getLogger(AutoBidAgent.class);
    private static final ExecutorService autoBidExecutor = Executors.newCachedThreadPool();
    private static final long serialVersionUID = 1L;

    private final Bidder bidder;
    private final int auctionId;
    private final AutoBidConfig config;
    private boolean active = true;
    private final AtomicBoolean processing = new AtomicBoolean(false); // ← thêm

    public AutoBidAgent(Bidder bidder, Auction auction, AutoBidConfig config) {
        this.bidder = bidder;
        this.auctionId = auction.getId();
        this.config = config;
        auction.addObserver(this);
    }

    @Override
    public void update(int auctionId, double currentPrice, String highestBidder) {
        if (!active) return;
        if (bidder.getName().equals(highestBidder)) return; // mình đang lead → không bid
        if (!processing.compareAndSet(false, true)) return; // đang xử lý → bỏ qua

        double nextBid = currentPrice + config.getIncrement();
        if (nextBid > config.getMaxBid()) {
            processing.set(false);
            stop();
            return;
        }

        autoBidExecutor.submit(() -> {
            try {
                Auction currentAuction = AuctionManager.getInstance().getActive(this.auctionId);
                if (currentAuction == null || !active) return;

                String currentLeader = currentAuction.getHighestBidder() != null
                        ? currentAuction.getHighestBidder().getName() : "";
                if (bidder.getName().equals(currentLeader)) return; // mình đang lead → không bid

                double nextBidNow = currentAuction.getCurrentPrice() + config.getIncrement();
                if (nextBidNow > config.getMaxBid()) {
                    stop();
                    return;
                }

                BidService.getInstance().processAutoBid(bidder, currentAuction, nextBidNow);
                logger.info("Auto-bid placed: {} bid {} on auction {}",
                        bidder.getName(), nextBidNow, auctionId);

            } catch (Exception e) {
                logger.error("Auto-bid failed for {} on auction {}: {}",
                        bidder.getName(), auctionId, e.getMessage());
            } finally {
                processing.set(false);
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