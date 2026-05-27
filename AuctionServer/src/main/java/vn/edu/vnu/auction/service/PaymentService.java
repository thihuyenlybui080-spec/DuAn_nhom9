package vn.edu.vnu.auction.service;

import vn.edu.vnu.auction.dao.AuctionDAO;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.util.AuctionHistoryManager;
import vn.edu.vnu.auction.util.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Dịch vụ thanh toán: deadline sau khi thắng đấu giá và xác nhận thanh toán.
 */
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static volatile PaymentService instance;

    private static final long PAYMENT_DEADLINE_SECONDS = 24 * 60 * 60;

    private final AuctionManager auctionManager;

    private PaymentService() {
        this.auctionManager = AuctionManager.getInstance();
    }

    /**
     * @return singleton {@link PaymentService}
     */
    public static PaymentService getInstance() {
        if (instance == null) {
            synchronized (PaymentService.class) {
                if (instance == null) {
                    instance = new PaymentService();
                }
            }
        }
        return instance;
    }

    /**
     * Lên lịch hủy phiên nếu người thắng không thanh toán trong thời hạn.
     */
    public void schedulePaymentDeadline(int auctionId) {
        AuctionHistoryManager historyManager = AuctionHistoryManager.getInstance();
        AuctionResult result = historyManager.getResult(auctionId);
        if (result == null) {
            return;
        }

        auctionManager.getScheduler().schedule(() -> {
            AuctionResult current = historyManager.getResult(auctionId);
            if (current != null && current.getStatus() == AuctionStatus.FINISHED) {
                historyManager.updateStatus(auctionId, AuctionStatus.CANCELED);
                if (auctionId > 0) {
                    AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.CANCELED);
                }
                logger.warn("Payment deadline expired for auction {}", auctionId);
            }
        }, PAYMENT_DEADLINE_SECONDS, TimeUnit.SECONDS);

        logger.info("Payment deadline scheduled for auction {} ({} hours)", auctionId,
                PAYMENT_DEADLINE_SECONDS / 3600);
    }

    /**
     * Xử lý thanh toán của bidder cho phiên đã thắng.
     *
     * @return true nếu thanh toán thành công hoặc đã thanh toán trước đó
     */
    public boolean processPayment(Bidder bidder, int auctionId) {
        AuctionResult result = AuctionHistoryManager.getInstance().getResult(auctionId);
        
        // If not in memory, try to load from database
        if (result == null) {
            logger.info("Auction {} not found in memory, loading from database", auctionId);
            vn.edu.vnu.auction.model.entity.Auction auction = AuctionDAO.getAuctionById(auctionId);
            if (auction == null) {
                logger.warn("processPayment: Auction {} not found in database", auctionId);
                return false;
            }
            result = new AuctionResult(auction);
            AuctionHistoryManager.getInstance().saveResult(result);
        }

        if (result.getWinner() == null || result.getWinner().getId() != bidder.getId()) {
            logger.warn("processPayment: {} is not winner of {}", bidder.getName(), auctionId);
            return false;
        }

        if (result.getStatus() == AuctionStatus.PAID) {
            logger.info("Auction {} already paid by {}", auctionId, bidder.getName());
            return true;
        }
        if (result.getStatus() != AuctionStatus.FINISHED) {
            logger.warn("Auction {} cannot be paid (status={})", auctionId, result.getStatus());
            return false;
        }

        logger.info("{} paying {} for item {}", bidder.getName(), result.getFinalPrice(),
                result.getItem().getItemName());
        AuctionHistoryManager.getInstance().updateStatus(auctionId, AuctionStatus.PAID);
        if (auctionId > 0) {
            AuctionDAO.updateAuctionStatus(auctionId, AuctionStatus.PAID);
        }

        return true;
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
}
