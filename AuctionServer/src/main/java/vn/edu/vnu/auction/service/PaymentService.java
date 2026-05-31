package vn.edu.vnu.auction.service;

import static vn.edu.vnu.auction.model.entity.AuctionStatus.CANCELED;
import static vn.edu.vnu.auction.model.entity.AuctionStatus.FINISHED;
import static vn.edu.vnu.auction.model.entity.AuctionStatus.PAID;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.dao.AuctionDAO;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.util.AuctionHistoryManager;
import vn.edu.vnu.auction.util.AuctionManager;

/**
 * Dịch vụ thanh toán tích hợp Stripe Checkout (sandbox). Luồng hoạt động: 1. Client bấm Pay → gọi
 * createPaymentLink() 2. Server tạo Stripe Checkout Session → trả link về client 3. Client mở trình
 * duyệt → user thanh toán bằng thẻ test 4. Client gọi confirmPayment() để xác nhận sau khi thanh
 * toán 5. Server kiểm tra trạng thái session Stripe → cập nhật PAID
 */
public class PaymentService {

  private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
  private static volatile PaymentService instance;

  private static final long PAYMENT_DEADLINE_SECONDS = 24 * 60 * 60;
  private static final long STRIPE_MINIUM_CENTS = 50;

  private final AuctionManager auctionManager;
  private final AuctionHistoryManager historyManager;

  private static final String STRIPE_SECRET_KEY = loadStripeKey();

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

  private PaymentService() {
    this.auctionManager = AuctionManager.getInstance();
    this.historyManager = AuctionHistoryManager.getInstance();
    Stripe.apiKey = STRIPE_SECRET_KEY;
    logger.info("PaymentService initialized with Stripe sandbox");
  }

  /**
   * Tạo Stripe Checkout link cho người thắng đấu giá.
   *
   * @return URL thanh toán, hoặc null nếu không hợp lệ / lỗi Stripe
   */
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
      long amountInCents = Math.max(STRIPE_MINIUM_CENTS, Math.round(result.getFinalPrice()));

      SessionCreateParams params = buildSessionCreateParams(bidder, auctionId, result,
          amountInCents);
      Session session = Session.create(params);
      logger.info("Stripe session created for auction {}: {}", auctionId, session.getId());
      return session.getUrl();

    } catch (StripeException e) {
      logger.error("Stripe error creating payment link: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Lên lịch hủy phiên nếu người thắng không thanh toán trong thời hạn.
   */
  public void schedulePaymentDeadline(int auctionId) {
      if (historyManager.getResult(auctionId) == null) {
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
    AuctionResult result = getAuctionResult(auctionId);
    if (result == null) {
      logger.info("Auction {} not found in memory, loading from database", auctionId);
      Auction auction = AuctionDAO.getAuctionById(auctionId);
      if (auction == null) {
        logger.warn("processPayment: Auction {} not found in database", auctionId);
        return false;
      }
      result = new AuctionResult(auction);
      historyManager.saveResult(result);
    }

    if (result.getWinner() == null || result.getWinner().getId() != bidder.getId()) {
      logger.warn("processPayment: {} is not winner of {}", bidder.getName(), auctionId);
      return false;
    }

    if (result.getStatus() == PAID) {
      logger.info("Auction {} already paid by {}", auctionId, bidder.getName());
      return true;
    }
    if (result.getStatus() != FINISHED) {
      logger.warn("Auction {} cannot be paid (status={})", auctionId, result.getStatus());
      return false;
    }

    logger.info("{} paying {} for item {}", bidder.getName(), result.getFinalPrice(),
        result.getItem().getItemName());
    updateAuctionStatus(auctionId);
    return true;
  }

  public static synchronized void resetForTesting() {
    synchronized (PaymentService.class) {
      instance = null;
    }
  }

  /**
   * Load từ DB nếu không có trong memory.
   */
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

  private void updateAuctionStatus(int auctionId) {
    historyManager.updateStatus(auctionId, PAID);
    AuctionDAO.updateAuctionStatus(auctionId, PAID);
  }

  private SessionCreateParams buildSessionCreateParams(Bidder bidder, int auctionId,
      AuctionResult result, long amountInCents) {
    return SessionCreateParams.builder()
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
                                .setName("Auction #" + auctionId + " - " + result.getItem()
                                    .getItemName())
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
  }

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
}
