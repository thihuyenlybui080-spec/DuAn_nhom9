package vn.edu.vnu.auction.controller;

import static vn.edu.vnu.auction.controller.MainController.LOGIN_FXML;
import static vn.edu.vnu.auction.controller.MainController.LOGIN_TITLE;

import java.io.File;
import java.net.URL;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.common.network.NotificationMessage;
import vn.edu.vnu.auction.common.observer.Observer;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.BidTransaction;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.service.AuctionClientService;
import vn.edu.vnu.auction.service.NotificationListener;
import vn.edu.vnu.auction.service.SceneManager;

/**
 * Controller cho màn hình đấu giá (Bidding).
 * <p>
 * Lớp này quản lý giao diện người dùng cho màn hình đấu giá, bao gồm hiển thị thông tin phiên đấu
 * giá, xử lý đặt giá thầu, quản lý tính năng auto-bid, và cập nhật thời gian thực.
 * </p>
 */
public class BiddingController implements Initializable, Observer {

  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final NumberFormat VND_FORMAT =
      NumberFormat.getNumberInstance(new Locale("vi", "VN"));

  private String comingFromFile;
  private String comingFromTitle;


  @FXML
  private Label lblUsername;

  // ── FXML – Topbar ─────────────────────────────────────────────────────────

  @FXML
  private Label lblItemName;
  @FXML
  private Label lblCategory;
  @FXML
  private Label lblCountdown;
  @FXML
  private Label lblCountdownLabel;
  @FXML
  private Label lblStatusBadge;
  @FXML
  private Button btnBack;

  // ── FXML – Cột trái ───────────────────────────────────────────────────────

  @FXML
  private Label lblBidCount;
  @FXML
  private VBox bidHistoryContainer;
  @FXML
  private Button btnNavAuctions;
  @FXML
  private Button btnSignOut;
  @FXML
  private Button btnNavMyAuctions;

  // ── FXML – Cột phải ───────────────────────────────────────────────────────

  @FXML
  private Label lblCurrentPrice;
  @FXML
  private Label lblLeader;

  // Place Bid
  @FXML
  private TextField txtBidAmount;
  @FXML
  private Label lblMinBid;
  @FXML
  private Button btnPlaceBid;
  @FXML
  private Label lblBidError;

  // ── FXML – Auto-Bid ─────────────────────────────────────────────────────

  @FXML
  private CheckBox chkAutoBid;
  @FXML
  private VBox autoBidForm;
  @FXML
  private TextField txtMaxBid;
  @FXML
  private TextField txtIncrement;
  @FXML
  private Button btnEnableAutoBid;
  @FXML
  private Label lblAutoBidStatus;
  @FXML
  private Label lblAutoBidHint;
  @FXML
  private Label lblAutoBidActiveStatus;
  @FXML
  private Button btnDisableAutoBid;

  // ── FXML – Status bar ─────────────────────────────────────────────────────

  @FXML
  private Label lblConnectionStatus;
  @FXML
  private Label lblLastUpdate;
  @FXML
  private BorderPane rootBorderPane;
  @FXML
  private ImageView imvProductImage;

  @FXML
  private AreaChart<String, Number> priceChart;
  @FXML
  private CategoryAxis xAxis;
  @FXML
  private NumberAxis yAxis;
  private XYChart.Series<String, Number> priceSeries;

  private Auction auction;
  private Bidder bidder;
  private boolean autoBidEnable = false;
  private ScheduledExecutorService scheduler;
  private List<BidTransaction> localBids = new ArrayList<>();
  private boolean timeWarningShown = false;

  private final SceneManager sceneManager = new SceneManager(getClass());
  private final static Logger logger = LoggerFactory.getLogger(BiddingController.class);

  /**
   * Khởi tạo controller sau khi FXML được tải.
   *
   * @param url            vị trí của FXML
   * @param resourceBundle tài nguyên bundle
   */
  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

  }

  /**
   * Thiết lập dữ liệu cho màn hình đấu giá.
   *
   * @param auction   phiên đấu giá đang xem
   * @param bidder    người tham gia đấu giá hiện tại
   * @param fromFXML  file FXML để quay lại khi nhấn nút back
   * @param fromTitle tiêu đề của màn hình quay lại
   */
  public void setData(Auction auction, Bidder bidder, String fromFXML, String fromTitle) {
    this.auction = auction;
    this.bidder = bidder;
    this.comingFromFile = fromFXML;
    this.comingFromTitle = fromTitle;
    Platform.runLater(() -> {
      Stage stage = (Stage) rootBorderPane.getScene().getWindow();
      if (stage != null) {
        stage.setFullScreen(true);
      }
    });
    if (this.auction != null) {
      this.auction.addObserver(this);
    }
    populateView();
    AuctionClientService.getInstance().watchAuction(auction.getId());
    checkAutoBidStatus();
    NotificationListener.getInstance().register(auction.getId(), notification -> {
          switch (notification.getType()) {
            case NotificationMessage.TYPE_BID_UPDATED:
              Auction updated = (Auction) notification.getData();
              Platform.runLater(() -> {
                this.auction.setCurrentPrice(updated.getCurrentPrice());
                if (updated.getHighestBidder() != null) {
                  this.auction.setHighestBidder(updated.getHighestBidder());
                  this.auction.setHighestBidderName(updated.getHighestBidder().getName());
                } else if (updated.getBids() != null && !updated.getBids().isEmpty()) {
                  BidTransaction highestBid = updated.getBids().stream()
                      .max((b1, b2) -> Double.compare(b1.getAmount(), b2.getAmount()))
                      .orElse(null);
                  if (highestBid != null && highestBid.getBidder() != null) {
                    this.auction.setHighestBidder(highestBid.getBidder());
                    this.auction.setHighestBidderName(highestBid.getBidder().getName());
                  }
                }
                this.auction.setStatus(updated.getStatus());
                if (updated.getItem() != null && updated.getItem().getEndTime() != null) {
                  this.auction.getItem().setEndTime(updated.getItem().getEndTime());
                }
                if (updated.getBids() != null && !updated.getBids().isEmpty()) {
                  localBids.clear();
                  localBids.addAll(updated.getBids());
                  localBids.sort((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()));
                } else {
                  try {
                    List<BidTransaction> bids = AuctionClientService.getInstance()
                        .getBidsByAuction(auction.getId());
                    localBids.clear();
                    localBids.addAll(bids);
                    localBids.sort((b1, b2) -> b2.getTimestamp().compareTo(b1.getTimestamp()));
                  } catch (Exception e) {
                    System.err.println("Failed to reload bids: " + e.getMessage());
                  }
                }
                updatePriceArea();
                refreshBidHistory();
                updateStatusBadge();
                updateBidButton();
              });
              break;
            case NotificationMessage.TYPE_AUCTION_ENDED:
              Platform.runLater(() -> {
                Auction ended = (Auction) notification.getData();
                this.auction.setStatus(ended.getStatus());
                updateBidButton();
                updateStatusBadge();
                lblCountdown.setText("ENDED");

                Stage stage = (Stage) rootBorderPane.getScene().getWindow();
                // Check if current bidder is the winner
                if (ended.getHighestBidder() != null
                    && ended.getHighestBidder().getId() == bidder.getId()) {
                  ToastNotification.show(stage, "Congratulations!",
                      "You won the auction for " + auction.getItem().getItemName() + "!",
                      ToastNotification.Type.SUCCESS);
                } else {
                  ToastNotification.show(stage, "Auction Ended", "The auction has ended. Winner: " +
                      (ended.getHighestBidder() != null ? ended.getHighestBidder().getName()
                          : "No winner"), ToastNotification.Type.INFO);
                }
              });
              break;
            case NotificationMessage.TYPE_TIME_EXTENDED:
              Platform.runLater(() -> {
                Auction extended = (Auction) notification.getData();
                if (extended.getItem() != null && extended.getItem().getEndTime() != null) {
                  this.auction.getItem().setEndTime(extended.getItem().getEndTime());
                }
                if (extended.getStatus() != null && extended.getStatus() == AuctionStatus.RUNNING) {
                  this.auction.setStatus(AuctionStatus.RUNNING);
                  updateStatusBadge();
                  updateBidButton();
                }
                lblCountdown.setStyle(
                    "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ff9800;");
                lblBidError.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 11px;");
                lblBidError.setText("⏱ Anti-snipe: +60s added!");
                lblBidError.setVisible(true);
                lblBidError.setManaged(true);

                // Auto-hide after 3 seconds
                new java.util.Timer().schedule(new java.util.TimerTask() {
                  @Override
                  public void run() {
                    Platform.runLater(() -> {
                      lblBidError.setVisible(false);
                      lblBidError.setManaged(false);
                    });
                  }
                }, 3000);
              });
              break;
            case NotificationMessage.TYPE_AUTO_BID_AUCTION_ENDED:
              Platform.runLater(() -> {
                String message = (String) notification.getData();
                lblBidError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
                lblBidError.setText("🤖 " + message);
                lblBidError.setVisible(true);
                lblBidError.setManaged(true);
                updateBidButton();
                updateStatusBadge();
                lblCountdown.setText("ENDED");

                // Auto-hide after 3 seconds
                new java.util.Timer().schedule(new java.util.TimerTask() {
                  @Override
                  public void run() {
                    Platform.runLater(() -> {
                      lblBidError.setVisible(false);
                      lblBidError.setManaged(false);
                    });
                  }
                }, 3000);

                Stage stage = (Stage) rootBorderPane.getScene().getWindow();
                ToastNotification.show(stage, "Auto-Bid Failed", message, ToastNotification.Type.ERROR);
              });
              break;
            case NotificationMessage.TYPE_USER_LOCKED:
              Platform.runLater(() -> {
                Integer lockedBidderId = (Integer) notification.getData();
                if (lockedBidderId != null && lockedBidderId.equals(bidder.getId())) {
                  bidder.setActive(false);
                  if (autoBidEnable) {
                    onDisableAutoBid();

                    Stage stage = (Stage) rootBorderPane.getScene().getWindow();
                    ToastNotification.show(stage, "Account Locked",
                        "Your account has been locked. Auto-bid has been disabled.",
                        ToastNotification.Type.ERROR);
                    lblBidError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
                    lblBidError.setText("🔒 Your account has been locked. Auto-bid disabled.");
                  } else {
                    Stage stage = (Stage) rootBorderPane.getScene().getWindow();
                    ToastNotification.show(stage, "Account Locked",
                        "Your account has been locked. You cannot place bids.",
                        ToastNotification.Type.ERROR);
                    lblBidError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
                    lblBidError.setText("🔒 Your account has been locked. You cannot place bids.");
                  }
                  lblBidError.setVisible(true);
                  lblBidError.setManaged(true);
                }
              });
              break;
            case NotificationMessage.TYPE_USER_UNLOCKED:
              Platform.runLater(() -> {
                Integer unlockedBidderId = (Integer) notification.getData();
                if (unlockedBidderId != null && unlockedBidderId.equals(bidder.getId())) {
                  bidder.setActive(true);

                  Stage stage = (Stage) rootBorderPane.getScene().getWindow();
                  ToastNotification.show(stage, "Account Unlocked",
                      "Your account has been unlocked. You can now bid and use auto-bid.",
                      ToastNotification.Type.SUCCESS);
                  lblBidError.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 11px;");
                  lblBidError.setText("✅ Your account has been unlocked.");
                  lblBidError.setVisible(true);
                  lblBidError.setManaged(true);
                  updateBidButton();

                  // Hide error message after 3 seconds
                  new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                      Platform.runLater(() -> {
                        lblBidError.setVisible(false);
                        lblBidError.setManaged(false);
                      });
                    }
                  }, 3000);
                }
              });
              break;
          }
        }
    );
    startAutoRefresh();
  }

  private void populateView() {
    lblUsername.setText(bidder.getName());
    lblItemName.setText(auction.getItem().getItemName());
    lblCategory.setText(auction.getItem().getCategory());
    updateStatusBadge();
    String imagePath = auction.getItem().getImagePath();
    System.out.println("[BiddingController] Image path: " + imagePath);
    if (imagePath != null && !imagePath.isEmpty()) {
      try {
        File imageFile = new File(imagePath);
        System.out.println(
            "[BiddingController] File exists: " + imageFile.exists() + ", Absolute path: "
                + imageFile.getAbsolutePath());
        if (imageFile.exists()) {
          Image image = new Image(imageFile.toURI().toString());
          imvProductImage.setImage(image);
          imvProductImage.setPreserveRatio(true);
          imvProductImage.setFitHeight(200);
        } else {
          System.err.println("[BiddingController] Image file not found: " + imagePath);
        }
      } catch (Exception e) {
        System.err.println("[BiddingController] Error loading product image: " + e.getMessage());
        e.printStackTrace();
      }
    } else {
      System.out.println("[BiddingController] Image path is null or empty");
    }
    logger.debug("[DEBUG] populateView - Initial localBids count: {}", localBids.size());
    if (localBids.isEmpty()) {
      try {
        List<BidTransaction> bids = AuctionClientService.getInstance()
            .getBidsByAuction(auction.getId());
        logger.debug("[DEBUG] populateView - Loaded {} bids from database", bids.size());
        localBids.addAll(bids);
        localBids.sort((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()));
        logger.debug("[DEBUG] populateView - After loading and sorting, localBids count: {}",
            localBids.size());
        if (!bids.isEmpty()) {
          for (BidTransaction bid : bids) {
            logger.debug("[DEBUG] Bid - Amount: {}, Bidder: {}", bid.getAmount(),
                bid.getBidder() != null ? bid.getBidder().getName() : "null");
          }
        }
        if (!localBids.isEmpty()) {
          BidTransaction highestBid = localBids.get(0);
          this.auction.setCurrentPrice(highestBid.getAmount());
          this.auction.setHighestBidder(highestBid.getBidder());
          this.auction.setHighestBidderName(highestBid.getBidder().getName());
        }
      } catch (Exception e) {
        System.err.println("Failed to load bids: " + e.getMessage());
        e.printStackTrace();
      }
    }
    updatePriceArea();
    refreshBidHistory();
    priceSeries = new XYChart.Series<>();
    priceSeries.setName("Bid Price");
    priceChart.getData().add(priceSeries);

    List<BidTransaction> chartBids = new ArrayList<>(localBids);
    Collections.reverse(chartBids);
    for (BidTransaction bid : chartBids) {
      String time = bid.getTimestamp() != null
          ? bid.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
          : "--";
      priceSeries.getData().add(
          new XYChart.Data<>(time, bid.getAmount()));
    }
    updateBidButton();
  }

  private void updateStatusBadge() {
    switch (auction.getStatus()) {
      case RUNNING:
        lblStatusBadge.setText("● Live");
        lblStatusBadge.setStyle(
            "-fx-background-color: #e6f4ea; -fx-text-fill: #2d8a4e;"
                + "-fx-background-radius: 10; -fx-padding: 4 12;"
                + "-fx-font-size: 11px; -fx-font-weight: bold;");
        break;
      case OPEN:
        lblStatusBadge.setText("● Upcoming");
        lblStatusBadge.setStyle(
            "-fx-background-color: #e8f0fe; -fx-text-fill: #1a56db;"
                + "-fx-background-radius: 10; -fx-padding: 4 12;"
                + "-fx-font-size: 11px; -fx-font-weight: bold;");
        break;
      case FINISHED:
        lblStatusBadge.setText("● Finished");
        lblStatusBadge.setStyle(
            "-fx-background-color: #f0f0f0; -fx-text-fill: #888;"
                + "-fx-background-radius: 10; -fx-padding: 4 12;"
                + "-fx-font-size: 11px; -fx-font-weight: bold;");
        break;
      default:
        lblStatusBadge.setText(auction.getStatus().toString());
    }
  }

  private void updatePriceArea() {
    lblCurrentPrice.setText(formatPrice(auction.getCurrentPrice()));

    String leaderName = null;
    if (auction.getHighestBidder() != null) {
      leaderName = auction.getHighestBidder().getName();
      logger.debug("[DEBUG] Leader from highestBidder: {}", leaderName);
    }
    if (leaderName == null && !localBids.isEmpty()) {
      BidTransaction highestBid = localBids.stream()
          .max((b1, b2) -> Double.compare(b1.getAmount(), b2.getAmount()))
          .orElse(null);
      if (highestBid != null && highestBid.getBidder() != null) {
        leaderName = highestBid.getBidder().getName();
        logger.debug("[DEBUG] Leader from localBids: {}", leaderName);
      }
    }

    if (leaderName != null && !leaderName.isEmpty()) {
      lblLeader.setText("Leader: 👑 " + leaderName);
    } else {
      lblLeader.setText("Leader: __");
      logger.debug("[DEBUG] No leader found. localBids count: {}", localBids.size());
    }

    double minBid = auction.getCurrentPrice() + 1;
    lblMinBid.setText("Min bid: " + formatPrice(minBid) + " ₫");
    lblBidCount.setText(localBids.size() + " bids");
  }

  private void updateBidButton() {
    boolean canBid = auction.getStatus() == AuctionStatus.RUNNING;
    btnPlaceBid.setDisable(!canBid);
    txtBidAmount.setDisable(!canBid);
    chkAutoBid.setDisable(!canBid);

    if (!canBid) {
      btnPlaceBid.setText(
          auction.getStatus() == AuctionStatus.OPEN
              ? "⏳  Not Started Yet"
              : "🔒  Auction Ended");
      btnPlaceBid.setStyle(
          "-fx-background-color: #ccc; -fx-text-fill: white;"
              + "-fx-font-size: 14px; -fx-font-weight: bold;"
              + "-fx-background-radius: 6;");
    } else {
      btnPlaceBid.setText("🔨  Place Bid");
      btnPlaceBid.setStyle(
          "-fx-background-color: #c0c43f; -fx-text-fill: #722f37;"
              + "-fx-font-size: 14px; -fx-font-weight: bold;"
              + "-fx-background-radius: 8; -fx-cursor: hand;");
    }
  }

  /**
   * Xử lý sự kiện khi người dùng nhấn nút đặt giá thầu.
   */
  @FXML
  private void onPlaceBid() {
    hideBidError();

    // Check if bidder is locked
    if (!bidder.isActive()) {
      Stage stage = (Stage) rootBorderPane.getScene().getWindow();
      ToastNotification.show(stage, "Account Locked",
          "Your account has been locked. You cannot place bids.", ToastNotification.Type.ERROR);
      showBidError("🔒 Your account has been locked. You cannot place bids.");
      updateBidButton();
      return;
    }

    String raw = txtBidAmount.getText().trim().replaceAll("[^0-9]", "");

    if (raw.isEmpty()) {
      Stage stage = (Stage) rootBorderPane.getScene().getWindow();
      ToastNotification.show(stage, "Error", "Please enter a bid amount",
          ToastNotification.Type.ERROR);
      showBidError("Please enter a bid amount");
      return;
    }

    double amount;
    try {
      amount = Double.parseDouble(raw);
    } catch (NumberFormatException e) {
      Stage stage = (Stage) rootBorderPane.getScene().getWindow();
      ToastNotification.show(stage, "Error", "Invalid amount.", ToastNotification.Type.ERROR);
      showBidError("Invalid amount.");
      return;
    }

    if (amount <= auction.getCurrentPrice()) {
      Stage stage = (Stage) rootBorderPane.getScene().getWindow();
      ToastNotification.show(stage, "Warning",
          "Bid must be greater than current price (" + formatPrice(auction.getCurrentPrice())
              + " ₫)", ToastNotification.Type.WARNING);
      showBidError(
          "Bid must be greater than current price (" + formatPrice(auction.getCurrentPrice())
              + " ₫)");
      return;
    }

    try {
      Auction updatedAuction = AuctionClientService.getInstance()
          .placeBid(auction.getId(), bidder.getId(), amount);
      txtBidAmount.clear();
      if (updatedAuction != null) {
        this.auction.setCurrentPrice(updatedAuction.getCurrentPrice());
        if (updatedAuction.getHighestBidder() != null) {
          this.auction.setHighestBidder(updatedAuction.getHighestBidder());
          this.auction.setHighestBidderName(updatedAuction.getHighestBidder().getName());
        }
        if (updatedAuction.getBids() != null && !updatedAuction.getBids().isEmpty()) {
          localBids.clear();
          localBids.addAll(updatedAuction.getBids());
          localBids.sort((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()));
        }
      }
      updatePriceArea();
      refreshBidHistory();
      showBidError("✅ Bid placed successfully: " + formatPrice(amount) + " ₫");
      lblBidError.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 11px;");

      Stage stage = (Stage) rootBorderPane.getScene().getWindow();
      ToastNotification.show(stage, "Success",
          "Bid placed successfully: " + formatPrice(amount) + " ₫", ToastNotification.Type.SUCCESS);
    } catch (RuntimeException e) {
      Stage stage = (Stage) rootBorderPane.getScene().getWindow();
      ToastNotification.show(stage, "Error", e.getMessage(), ToastNotification.Type.ERROR);
      showBidError(e.getMessage());
    }
  }

  /**
   * Xử lý sự kiện khi người dùng bật/tắt checkbox auto-bid.
   */
  @FXML
  private void onToggleAutoBid() {
    boolean on = chkAutoBid.isSelected();
    autoBidForm.setVisible(on);
    autoBidForm.setManaged(on);
    lblAutoBidHint.setVisible(!on);
    lblAutoBidHint.setManaged(!on);

    if (!on) {
      autoBidEnable = false;
      AuctionClientService.getInstance().disableAutoBid(auction.getId(), bidder.getId());
      lblAutoBidStatus.setText("");
      btnEnableAutoBid.setText("⚡ Enable Auto-Bid");
      btnEnableAutoBid.setStyle(
          "-fx-background-color: #1e3a5f; -fx-text-fill: #60a5fa;"
              + "-fx-font-size: 12px; -fx-font-weight: bold;"
              + "-fx-background-radius: 6; -fx-cursor: hand;"
      );
      txtMaxBid.clear();
      txtIncrement.clear();
    }
  }

  /**
   * Xử lý sự kiện khi người dùng nhấn nút kích hoạt auto-bid.
   */
  @FXML
  private void onEnableAutoBid() {
    // Check if bidder is locked
    if (!bidder.isActive()) {
      lblAutoBidStatus.setText("Your account is locked and cannot enable auto-bid.");
      lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
      Stage stage = (Stage) rootBorderPane.getScene().getWindow();
      ToastNotification.show(stage, "Account Locked",
          "Your account has been locked. Auto-bid cannot be enabled.",
          ToastNotification.Type.ERROR);
      return;
    }

    if (auction.getStatus() == AuctionStatus.FINISHED) {
      lblAutoBidStatus.setText("This auction has ended and cannot enable auto-bid.");
      lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
      Stage stage = (Stage) rootBorderPane.getScene().getWindow();
      ToastNotification.show(stage, "Warning", "This auction has ended and cannot enable auto-bid.",
          ToastNotification.Type.WARNING);
      return;
    }
    if (auction.getItem().getEndTime() != null && auction.getItem().getEndTime()
        .isBefore(LocalDateTime.now())) {
      lblAutoBidStatus.setText("This auction has expired and cannot enable auto-bid.");
      lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
      Stage stage = (Stage) rootBorderPane.getScene().getWindow();
      ToastNotification.show(stage, "Warning",
          "This auction has expired and cannot enable auto-bid.", ToastNotification.Type.WARNING);
      return;
    }

    if (!bidder.isActive()) {
      lblAutoBidStatus.setText("Your account is locked and cannot enable auto-bid.");
      lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
      return;
    }

    String maxBidStr = txtMaxBid.getText().trim().replaceAll("[^0-9]", "");
    String incrStr = txtIncrement.getText().trim().replaceAll("[^0-9]", "");
    if (maxBidStr.isEmpty() || incrStr.isEmpty()) {
      lblAutoBidStatus.setText("Please fill in both fields");
      lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
      return;
    }

    double maxBid;
    double increment;
    try {
      maxBid = Double.parseDouble(maxBidStr);
      increment = Double.parseDouble(incrStr);

    } catch (NumberFormatException e) {
      lblAutoBidStatus.setText("Invalid numbers.");
      lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
      return;
    }

    if (maxBid <= auction.getCurrentPrice()) {
      lblAutoBidStatus.setText("Max bid must be higher than current price.");
      lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
      return;
    }

    lblAutoBidStatus.setText("⏳ Enabling...");
    btnEnableAutoBid.setDisable(true);

    new Thread(() -> {
      try {
        AuctionClientService.getInstance()
            .enableAutoBid(auction.getId(), bidder.getId(), maxBid, increment);

        Platform.runLater(() -> {
          autoBidEnable = true;
          lblAutoBidStatus.setText("✅ Active — Max: " + formatPrice(maxBid) + " ₫"
              + "  Inc: " + formatPrice(increment) + " ₫");
          lblAutoBidStatus.setStyle(
              "-fx-text-fill: #4ade80; -fx-font-size: 12px; -fx-font-weight: bold;");
          btnEnableAutoBid.setText("✅ Auto-Bid Active");
          btnEnableAutoBid.setStyle("-fx-background-color: #052e16; -fx-text-fill: #34d399;"
              + "-fx-font-size: 12px; -fx-font-weight: bold;"
              + "-fx-background-radius: 6;");
          btnEnableAutoBid.setDisable(false);

          txtMaxBid.clear();
          txtIncrement.clear();

          autoBidForm.setVisible(false);
          autoBidForm.setManaged(false);
          lblAutoBidHint.setVisible(false);
          lblAutoBidHint.setManaged(false);

          if (btnDisableAutoBid != null) {
            btnDisableAutoBid.setVisible(true);
            btnDisableAutoBid.setManaged(true);
          }
        });
      } catch (Exception e) {
        logger.error("Failed to enable auto-bid: {}", e.getMessage());
        Platform.runLater(() -> {
          String errorMsg = e.getMessage();
          lblAutoBidStatus.setText("❌ Failed: " + errorMsg);
          lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
          btnEnableAutoBid.setDisable(false);
          if (errorMsg != null && errorMsg.toLowerCase().contains("lock")) {
            Stage stage = (Stage) rootBorderPane.getScene().getWindow();
            ToastNotification.show(stage, "Account Locked",
                "Your account has been locked. Auto-bid cannot be enabled.",
                ToastNotification.Type.ERROR);
          }
        });
      }
    }).start();

  }

  @Override
  public void update(int auctionId, double newPrice, String highestBidder) {
    Platform.runLater(() -> {
      this.auction.setCurrentPrice(newPrice);
      this.auction.setHighestBidderName(highestBidder);
      updatePriceArea();
      refreshBidHistory();
      updateStatusBadge();
      updateBidButton();
      lblLastUpdate.setText(
          "Updated " + LocalDateTime.now().format(TIME_FORMAT));

    });

  }

  private void checkAutoBidStatus() {
    Map<String, Object> status = AuctionClientService.getInstance()
        .checkAutoBid(auction.getId(), bidder.getId());
    if (status != null) {
      boolean isActive = (Boolean) status.get("active");
      Platform.runLater(() -> {
        if (isActive) {
          double maxBid = (Double) status.get("maxBid");
          double increment = (Double) status.get("increment");
          lblAutoBidActiveStatus.setText(
              "✓ Auto-Bid Active (Max: " + VND_FORMAT.format(maxBid) + ")");
          lblAutoBidActiveStatus.setStyle(
              "-fx-text-fill: #4ade80; -fx-font-size: 12px; -fx-font-weight: bold;");
          if (btnDisableAutoBid != null) {
            btnDisableAutoBid.setVisible(true);
            btnDisableAutoBid.setManaged(true);
          }
          autoBidEnable = true;
          chkAutoBid.setSelected(true);
          txtMaxBid.clear();
          txtIncrement.clear();
        } else {
          lblAutoBidActiveStatus.setText("");
          if (btnDisableAutoBid != null) {
            btnDisableAutoBid.setVisible(false);
            btnDisableAutoBid.setManaged(false);
          }
          autoBidEnable = false;
          chkAutoBid.setSelected(false);
          txtMaxBid.clear();
          txtIncrement.clear();
        }
      });
    }
  }

  /**
   * Xử lý sự kiện khi người dùng nhấn nút vô hiệu hóa auto-bid.
   */
  @FXML
  private void onDisableAutoBid() {
    AuctionClientService.getInstance().disableAutoBid(auction.getId(), bidder.getId());
    lblAutoBidActiveStatus.setText("");
    if (btnDisableAutoBid != null) {
      btnDisableAutoBid.setVisible(false);
      btnDisableAutoBid.setManaged(false);
    }
    autoBidEnable = false;
    chkAutoBid.setSelected(false);

    // Reset form and button to initial state
    lblAutoBidStatus.setText("");
    btnEnableAutoBid.setText("⚡ Enable Auto-Bid");
    btnEnableAutoBid.setStyle(
        "-fx-background-color: #1e3a5f; -fx-text-fill: #60a5fa;"
            + "-fx-font-size: 12px; -fx-font-weight: bold;"
            + "-fx-background-radius: 6; -fx-cursor: hand;"
    );
    autoBidForm.setVisible(false);
    autoBidForm.setManaged(false);
    lblAutoBidHint.setVisible(true);
    lblAutoBidHint.setManaged(true);
  }

  private void refreshBidHistory() {
    if (priceSeries != null && !localBids.isEmpty()) {
      priceSeries.getData().clear();
      List<BidTransaction> chartBids = new ArrayList<>(localBids);
      Collections.reverse(chartBids);
      for (BidTransaction bid : chartBids) {
        String time = bid.getTimestamp() != null
            ? bid.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            : "--";
        priceSeries.getData().add(
            new XYChart.Data<>(time, bid.getAmount()));
      }
    }
    bidHistoryContainer.getChildren().clear();

    if (localBids.isEmpty()) {
      Label empty = new Label("No bids yet. Be the first!");
      empty.setStyle("-fx-text-fill: #c0c43f; -fx-font-size: 12px;");
      empty.setPadding(new Insets(8, 0, 0, 0));
      bidHistoryContainer.getChildren().add(empty);
      return;
    }

    for (int i = 0; i < localBids.size(); i++) {
      bidHistoryContainer.getChildren().add(buildBidRow(localBids.get(i), i == 0));
    }

    lblBidCount.setText(localBids.size() + " bids");
  }

  private HBox buildBidRow(BidTransaction tx, boolean isTop) {
    HBox row = new HBox(8);
    row.setAlignment(Pos.CENTER_LEFT);
    row.setPadding(new Insets(6, 0, 6, 0));
    row.setStyle(
        "-fx-border-color: transparent transparent #c0c43f transparent;"
            + "-fx-border-width: 0 0 1 0;");

    String nameText = isTop
        ? "👑 " + tx.getBidder().getName()
        : tx.getBidder().getName();

    Label lblName = new Label(nameText);
    lblName.setFont(Font.font("System", FontWeight.BOLD, 11));
    lblName.setStyle("-fx-text-fill: #fff;");

    Region spacer1 = new Region();
    HBox.setHgrow(spacer1, Priority.ALWAYS);

    Label lblPrice = new Label(formatPrice(tx.getAmount()) + " ₫");
    lblPrice.setStyle(
        "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #c0c43f;");

    Region spacer2 = new Region();
    HBox.setHgrow(spacer2, Priority.ALWAYS);

    Label lblTime = new Label(tx.getTimestamp() != null
        ? tx.getTimestamp().format(TIME_FORMAT) : "—");
    lblTime.setStyle("-fx-font-size: 10px; -fx-text-fill: #c0c43f; -fx-opacity: 0.7");
    lblTime.setPrefWidth(150);

    row.getChildren().addAll(lblName, spacer1, lblPrice, spacer2, lblTime);
    return row;

  }

  private void startAutoRefresh() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        () -> Platform.runLater(this::tick),
        0, 1, TimeUnit.SECONDS);
  }

  private void tick() {
    updateCountdown();
    // Always refresh from server to ensure UI is synced
    try {
      Auction updatedAuction = AuctionClientService.getInstance().getAuctionById(auction.getId());
      if (updatedAuction != null) {
        boolean needsUpdate = false;
        if (updatedAuction.getStatus() != this.auction.getStatus()) {
          logger.info("Server status: {}, Local status: {}", updatedAuction.getStatus(),
              this.auction.getStatus());
          this.auction.setStatus(updatedAuction.getStatus());
          updateStatusBadge();
          updateBidButton();
          needsUpdate = true;
        }
        // Also sync end time if it's different
        if (updatedAuction.getItem() != null && updatedAuction.getItem().getEndTime() != null
            && !updatedAuction.getItem().getEndTime().equals(this.auction.getItem().getEndTime())) {
          this.auction.getItem().setEndTime(updatedAuction.getItem().getEndTime());
          logger.info("Synced auction end time to {}", updatedAuction.getItem().getEndTime());
          needsUpdate = true;
        }
        if (needsUpdate) {
          updateCountdown();
        }
      }
    } catch (Exception e) {
      // Silent fail - don't spam logs on every tick
    }
  }

  private void updateCountdown() {
    if (auction.getStatus() == AuctionStatus.FINISHED) {
      lblCountdown.setText("ENDED");
      lblCountdownLabel.setText("Time Left");
      lblCountdown.setStyle(
          "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #722f37;");
      timeWarningShown = false;
      stopScheduler();
      return;
    }

    long totalSecs;
    if (auction.getStatus() == AuctionStatus.OPEN && auction.getItem().getStartTime() != null) {
      totalSecs = Duration.between(LocalDateTime.now(), auction.getItem().getStartTime())
          .getSeconds();
      lblCountdownLabel.setText("Starts in");
      if (totalSecs <= 0) {
        // Refresh auction data from server when start time is reached
        try {
          Auction updatedAuction = AuctionClientService.getInstance()
              .getAuctionById(auction.getId());
          if (updatedAuction != null) {
            this.auction.setStatus(updatedAuction.getStatus());
            updateStatusBadge();
            updateBidButton();
          }
        } catch (Exception e) {
          logger.error("Failed to refresh auction status: {}", e.getMessage());
        }
        return;
      }
    } else if (auction.getItem().getEndTime() != null) {
      totalSecs = Duration.between(LocalDateTime.now(), auction.getItem().getEndTime())
          .getSeconds();
      lblCountdownLabel.setText("Time Left");

      if (totalSecs <= 60 && totalSecs > 0 && !timeWarningShown) {
        timeWarningShown = true;
        Stage stage = (Stage) rootBorderPane.getScene().getWindow();
        ToastNotification.show(stage, "Warning", "Auction ending in less than 1 minute!",
            ToastNotification.Type.WARNING);
      }

      if (totalSecs <= 0) {
        try {
          logger.info("End time reached, refreshing auction {} status from server (current: {})",
              auction.getId(), auction.getStatus());
          Auction updatedAuction = AuctionClientService.getInstance()
              .getAuctionById(auction.getId());
          if (updatedAuction != null) {
            logger.info("Server returned auction with status: {}", updatedAuction.getStatus());
            this.auction.setStatus(updatedAuction.getStatus());
            updateStatusBadge();
            updateBidButton();
            if (updatedAuction.getStatus() == AuctionStatus.FINISHED) {
              lblCountdown.setText("ENDED");

              Stage stage = (Stage) rootBorderPane.getScene().getWindow();
              if (updatedAuction.getHighestBidder() != null
                  && updatedAuction.getHighestBidder().getId() == bidder.getId()) {
                ToastNotification.show(stage, "Congratulations!",
                    "You won the auction for " + auction.getItem().getItemName() + "!",
                    ToastNotification.Type.SUCCESS);
              } else {
                ToastNotification.show(stage, "Auction Ended", "The auction has ended. Winner: " +
                    (updatedAuction.getHighestBidder() != null ? updatedAuction.getHighestBidder()
                        .getName() : "No winner"), ToastNotification.Type.INFO);
              }

              stopScheduler();
              return;
            }
          } else {
            logger.warn("Server returned null auction for ID {}", auction.getId());
          }
        } catch (Exception e) {
          logger.error("Failed to refresh auction status: {}", e.getMessage());
        }
        lblCountdown.setText("ENDED");
        return;
      }
    } else {
      return;
    }

    long h = totalSecs / 3600;
    long m = (totalSecs % 3600) / 60;
    long s = totalSecs % 60;
    lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));

    if (auction.getStatus() == AuctionStatus.OPEN) {
      lblCountdown.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #60a5fa;");
    } else if (totalSecs <= 300) {
      lblCountdown.setStyle(
          "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #fff;");
    } else if (totalSecs <= 1800) {
      lblCountdown.setStyle(
          "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ffff9e;");
    } else {
      lblCountdown.setStyle(
          "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #c0c43f;");
    }
  }

  private void stopScheduler() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
    NotificationListener.getInstance().unregister(auction.getId());
    AuctionClientService.getInstance().leaveAuction(auction.getId());
  }

  // ── Navigation ────────────────────────────────────────────────────────────

  /**
   * Xử lý sự kiện khi người dùng nhấn nút quay lại.
   *
   * @param event sự kiện action
   */
  @FXML
  private void onBack(ActionEvent event) {
    stopScheduler();
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    if (comingFromFile.equals("bidder_dashboard.fxml")) {
      BidderDashboardController ctrl = sceneManager.switchSceneAndGetController(stage,
          comingFromFile, comingFromTitle);
      if (ctrl != null) {
        ctrl.setCurrent(bidder);
      }
    } else if (comingFromFile.equals("auction_detail.fxml")) {
      AuctionDetailController ctrl = sceneManager.switchSceneAndGetController(stage, comingFromFile,
          comingFromTitle);
      if (ctrl != null) {
        ctrl.setData(auction, bidder, "bidder_dashboard.fxml", "Bidder Dashboard");
      }
    } else {
      sceneManager.switchScene(stage, comingFromFile, comingFromTitle);
    }
  }

  /**
   * Xử lý sự kiện khi người dùng điều hướng đến màn hình danh sách đấu giá.
   *
   * @param event sự kiện action
   */
  @FXML
  private void onNavAuctions(ActionEvent event) {
    stopScheduler();
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    BidderDashboardController ctrl = sceneManager.switchSceneAndGetController(stage,
        "bidder_dashboard.fxml", "Bidder Dashboard");
    if (ctrl != null) {
      ctrl.setCurrent(bidder);
    }
  }

  /**
   * Xử lý sự kiện khi người dùng đăng xuất.
   *
   * @param event sự kiện action
   */
  @FXML
  private void onSignOut(ActionEvent event) {
    stopScheduler();
    sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);
  }

  /**
   * Xử lý sự kiện khi người dùng điều hướng đến màn hình đấu giá của mình.
   *
   * @param event sự kiện action
   */
  @FXML
  private void onNavMyAuctions(ActionEvent event) {
    sceneManager.switchScene(event, "bidding.fxml", "Bidding");
  }

  // ── UI helpers ────────────────────────────────────────────────────────────

  private void showBidError(String message) {
    lblBidError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
    lblBidError.setText(message);
    lblBidError.setVisible(true);
    lblBidError.setManaged(true);

    // Auto-hide after 3 seconds, but only if bidder is not locked
    if (bidder.isActive()) {
      new java.util.Timer().schedule(new java.util.TimerTask() {
        @Override
        public void run() {
          Platform.runLater(() -> {
            lblBidError.setVisible(false);
            lblBidError.setManaged(false);
          });
        }
      }, 3000);
    }
  }

  private void hideBidError() {
    // Don't hide error message if bidder is locked
    if (!bidder.isActive()) {
      return;
    }
    lblBidError.setVisible(false);
    lblBidError.setManaged(false);
  }

  // ── Format helpers ────────────────────────────────────────────────────────

  private String formatPrice(double price) {
    return VND_FORMAT.format((long) price);
  }


}
