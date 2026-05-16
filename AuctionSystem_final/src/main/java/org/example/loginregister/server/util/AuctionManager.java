package org.example.loginregister.server.util;

import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.service.AuctionService;
import org.example.loginregister.server.service.BidService;
import org.example.loginregister.server.service.ItemService;
import org.example.loginregister.server.service.PaymentService;
import org.example.loginregister.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Singleton quản lý trạng thái in-memory của các phiên đấu giá đang chạy.
 * Logic nghiệp vụ nằm tại các service trong {@code org.example.loginregister.server.service}.
 */
public class AuctionManager {

    private static final Logger logger = LoggerFactory.getLogger(AuctionManager.class);
    private static volatile AuctionManager instance;

    private final Map<String, Auction> activeAuctions;
    private final ScheduledExecutorService scheduler;

    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
        scheduler = Executors.newScheduledThreadPool(10);
    }

    /**
     * @return singleton quản lý in-memory
     */
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

    /**
     * Đăng ký phiên đang active trong bộ nhớ.
     */
    public void putActive(Auction auction) {
        if (auction != null && auction.getId() != null) {
            activeAuctions.put(auction.getId(), auction);
            logger.debug("Active auction registered: {}", auction.getId());
        }
    }

    /**
     * Lấy phiên đang chạy từ bộ nhớ (không truy vấn DB).
     */
    public Auction getActive(String auctionId) {
        return activeAuctions.get(auctionId);
    }

    /**
     * Gỡ phiên khỏi bộ nhớ active.
     */
    public void removeActive(String auctionId) {
        activeAuctions.remove(auctionId);
        logger.debug("Active auction removed: {}", auctionId);
    }

    /** @return bản sao không đồng bộ của các phiên đang active */
    public Collection<Auction> getAllActive() {
        return activeAuctions.values();
    }

    /** Scheduler dùng cho mở/kết thúc phiên và payment deadline. */
    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    /** Dừng scheduler khi tắt server. */
    public void shutdown() {
        scheduler.shutdownNow();
        logger.info("AuctionManager scheduler shut down");
    }

    /**
     * Reset singleton cho unit test.
     */
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
        AuctionService.resetForTesting();
        BidService.resetForTesting();
        ItemService.resetForTesting();
        PaymentService.resetForTesting();
        UserService.resetForTesting();
    }
}
