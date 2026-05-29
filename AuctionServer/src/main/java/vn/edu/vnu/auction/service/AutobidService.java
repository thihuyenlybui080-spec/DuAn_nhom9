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

public class AutobidService implements Observer {
    private static Logger logger = LoggerFactory.getLogger(AutobidService.class);
    private static volatile AutobidService instance;

    private AutobidService() {
    }

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

    private final Map<Integer, PriorityQueue<AutoBid>> queues = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicBoolean> processing = new ConcurrentHashMap<>();
    private static final ExecutorService autoBidExecutor = Executors.newCachedThreadPool();

    @Override
    public void update(int auctionId, double currentPrice, String highestBidder) {
        AtomicBoolean flag = processing.computeIfAbsent(auctionId, k -> new AtomicBoolean(false));
        autoBidExecutor.submit(() -> {
            try {
                processQueue(auctionId, highestBidder);
            } finally {
                flag.set(false);
            }
        });

    }

    public void processQueue(int auctionId, String highestBidder) {
        PriorityQueue<AutoBid> queue = queues.get(auctionId);
        if (queue == null || queue.isEmpty()) return;
        Auction auction = AuctionManager.getInstance().getActive(auctionId);
        if (auction == null) return;
        double currentPrice = auction.getCurrentPrice();
        Bidder currentHighest = auction.getHighestBidder();
        for (AutoBid autoBid : queue) {
            if (!autoBid.isActive()) continue;
            if(!autoBid.getBidder().isActive()){
                autoBid.deactivate();
                logger.info("AutoBid deactivated: bidder {} is locked",
                        autoBid.getBidder().getName());
                ClientRegistry.getInstance().notifyAll(auctionId, new NotificationMessage(
                        NotificationMessage.TYPE_USER_LOCKED,
                        auctionId,
                        autoBid.getBidder().getId()
                ));
                continue;
            }
            if (currentHighest != null && autoBid.getBidder().getId() == currentHighest.getId()) {
                logger.debug("Skipping auto-bid for bidder {} - already highest bidder", autoBid.getBidder().getName());
                continue;
            }

            double nextBid = currentPrice + autoBid.getConfig().getIncrement();
            if (nextBid > autoBid.getConfig().getMaxBid()) {
                autoBid.deactivate();
                logger.info("AutoBid maxBid reached: bidder={}, "
                                + "auctionId={}",
                        autoBid.getBidder().getName(), auctionId);
                continue;
            }
            try {
                boolean success = BidService.getInstance()
                        .processAutoBid(autoBid.getBidder(), auction, nextBid);
                if (success) {
                    logger.info("AutoBid placed: bidder={}, "
                                    + "amount={}, auctionId={}",
                            autoBid.getBidder().getName(),
                            nextBid, auctionId);
                    Auction updatedAuction = AuctionManager.getInstance().getActive(auctionId);
                    if (updatedAuction != null) {
                        ClientRegistry.getInstance().notifyAll(auctionId, new NotificationMessage(
                                NotificationMessage.TYPE_BID_UPDATED,
                                auctionId,
                                updatedAuction
                        ));
                    }
                    break;
                }
            } catch (Exception e) {
                logger.error("AutoBid failed: bidder={}, "
                                + "amount={}, auctionId={}",
                        autoBid.getBidder().getName(),
                        nextBid, auctionId, e);
            }
        }
    }

    public void enableAutoBid(Bidder bidder, Auction auction, AutoBidConfig config) {
        disableAutoBid(auction.getId(), bidder.getId());

        AutoBid autoBid = new AutoBid(bidder, auction.getId(), config);
        queues.computeIfAbsent(auction.getId(), k -> new PriorityQueue<>(
                        Comparator.comparing(AutoBid::getRegisteredAt)
                )
        ).offer(autoBid);
        AutoBidDAO.saveAutoBid(auction.getId(), bidder.getId(), config.getMaxBid(), config.getIncrement());
        logger.info("AutoBid enabled: bidder={}, auction={}",
                bidder.getName(), auction.getId());
        Bidder currentHighest = auction.getHighestBidder();
        boolean alreadyLeading = currentHighest != null && currentHighest.getId() == bidder.getId();
        if(alreadyLeading){
            logger.info("Autobid registered but bidder {} already leading - skipping initial bid", bidder.getName());
            return;
        }
        double currentPrice = auction.getCurrentPrice();
        double nextBid = currentPrice + config.getIncrement();
        if (nextBid <= config.getMaxBid()) {
            try {
                boolean success = BidService.getInstance().processAutoBid(bidder, auction, nextBid);
                if (success) {
                    logger.info("Initial auto-bid placed: bidder={}, amount={}, auctionId={}",
                            bidder.getName(), nextBid, auction.getId());
                    autoBidExecutor.submit(() -> {
                        Auction updatedAuction = AuctionManager.getInstance().getActive(auction.getId());
                        if (updatedAuction != null) {
                            ClientRegistry.getInstance().notifyAll(auction.getId(), new NotificationMessage(
                                    NotificationMessage.TYPE_BID_UPDATED,
                                    auction.getId(),
                                    updatedAuction
                            ));
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("Failed to place initial auto-bid: bidder={}, amount={}, auctionId={}",
                        bidder.getName(), nextBid, auction.getId(), e);
            }
        }
    }

    public void disableAutoBid(int auctionId, int bidderId) {
        PriorityQueue<AutoBid> queue = queues.get(auctionId);
        if (queue != null) {
            queue.stream()
                    .filter(ab -> ab.getBidder().getId() == bidderId)
                    .forEach(ab -> ab.deactivate());
            queue.removeIf(ab -> !ab.isActive());
        }
        AutoBidDAO.deleteAutoBid(auctionId, bidderId);

        logger.info("AutoBid disabled: bidderId={}, auctionId={}",
                bidderId, auctionId);
    }

    public void clearAuction(int auctionId){
        queues.remove(auctionId);
        processing.remove(auctionId);
        logger.info("AutoBid cleared: auctionId={}", auctionId);
    }

    public boolean isAutoBidActive(int auctionId, int bidderId) {
        PriorityQueue<AutoBid> queue = queues.get(auctionId);
        if (queue == null) return false;
        return queue.stream()
                .anyMatch(ab -> ab.getBidder().getId() == bidderId && ab.isActive());
    }
}
