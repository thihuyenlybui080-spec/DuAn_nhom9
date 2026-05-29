package vn.edu.vnu.auction.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.model.checkout.Session;
import vn.edu.vnu.auction.dao.AuctionDAO;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.util.AuctionHistoryManager;
import vn.edu.vnu.auction.util.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

import static vn.edu.vnu.auction.model.entity.AuctionStatus.*;

/**
 * Dịch vụ thanh toán: deadline sau khi thắng đấu giá và xác nhận thanh toán.
 */
/**
 * Dịch vụ thanh toán tích hợp Stripe Checkout (sandbox).
 *
 * Luồng hoạt động:
 *   1. Client bấm Pay → gọi createPaymentLink()
 *   2. Server tạo Stripe Checkout Session → trả link về client
 *   3. Client mở trình duyệt → user thanh toán bằng thẻ test
 *   4. Client gọi confirmPayment() để xác nhận sau khi thanh toán
 *   5. Server kiểm tra trạng thái session Stripe → cập nhật PAID
 */
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static volatile PaymentService instance;

    private static final String STRIPE_SECRET_KEY = loadStripeKey();

    private static String loadStripeKey() {
        try (java.io.InputStream in = PaymentService.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            java.util.Properties props = new java.util.Properties();
            props.load(in);
            return props.getProperty("stripe.secret.key", "");
        } catch (Exception e) {
            return "";
        }
    }


    private static final long PAYMENT_DEADLINE_SECONDS = 24 * 60 * 60;

    private final AuctionManager auctionManager;

    private PaymentService() {
        this.auctionManager = AuctionManager.getInstance();
        Stripe.apiKey = STRIPE_SECRET_KEY;
        logger.info("PaymentService initialized with Stripe sandbox");
    }
    public String createPaymentLink(Bidder bidder, int auctionId) {
        AuctionResult result = getAuctionResult(auctionId);
        if (result == null) {
            logger.warn("createPaymentLink: Auction {} not found", auctionId);
            return null;
        }

        if (result.getWinner() == null || result.getWinner().getId() != bidder.getId()) {
            logger.warn("createPaymentLink: {} is not winner of {}", bidder.getName(), auctionId);
            return null;
        }

        if (result.getStatus() == AuctionStatus.PAID) {
            logger.info("Auction {} already paid", auctionId);
            return null;
        }

        try {
            // Stripe yêu cầu số tiền tính bằng đơn vị nhỏ nhất (VND không có xu → x100 để dùng USD cent)
            long amountInCents = Math.max(50, Math.round(result.getFinalPrice()));

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://example.com/success?auction=" + auctionId)
                    .setCancelUrl("https://example.com/cancel?auction=" + auctionId)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Auction #" + auctionId + " - " + result.getItem().getItemName())
                                                                    .setDescription("Winner: " + bidder.getName())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    // Lưu auctionId vào metadata để xác nhận sau
                    .putMetadata("auction_id", String.valueOf(auctionId))
                    .putMetadata("bidder_id", String.valueOf(bidder.getId()))
                    .build();

            Session session = Session.create(params);
            logger.info("Stripe session created for auction {}: {}", auctionId, session.getId());
            return session.getUrl();

        } catch (StripeException e) {
            logger.error("Stripe error creating payment link: {}", e.getMessage());
            return null;
        }
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
            if (current != null && current.getStatus() == FINISHED) {
                historyManager.updateStatus(auctionId, CANCELED);
                if (auctionId > 0) {
                    AuctionDAO.updateAuctionStatus(auctionId, CANCELED);
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
        AuctionHistoryManager.getInstance().updateStatus(auctionId, PAID);
        if (auctionId > 0) {
            AuctionDAO.updateAuctionStatus(auctionId, PAID);
        }

        return true;
    }
    private AuctionResult getAuctionResult(int auctionId) {
        AuctionResult result = AuctionHistoryManager.getInstance().getResult(auctionId);
        if (result == null) {
            vn.edu.vnu.auction.model.entity.Auction auction = AuctionDAO.getAuctionById(auctionId);
            if (auction != null) {
                result = new AuctionResult(auction);
                AuctionHistoryManager.getInstance().saveResult(result);
            }
        }
        return result;
    }

    public static synchronized void resetForTesting() {
        instance = null;
    }
}
