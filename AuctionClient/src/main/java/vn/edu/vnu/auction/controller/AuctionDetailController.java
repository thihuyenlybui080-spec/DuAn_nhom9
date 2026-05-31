package vn.edu.vnu.auction.controller;

import static vn.edu.vnu.auction.controller.MainController.LOGIN_FXML;
import static vn.edu.vnu.auction.controller.MainController.LOGIN_TITLE;
import static vn.edu.vnu.auction.model.entity.AuctionStatus.FINISHED;
import static vn.edu.vnu.auction.model.entity.AuctionStatus.OPEN;
import static vn.edu.vnu.auction.model.entity.AuctionStatus.RUNNING;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.model.entity.user.Seller;
import vn.edu.vnu.auction.model.entity.user.User;
import vn.edu.vnu.auction.service.AuctionClientService;
import vn.edu.vnu.auction.service.SceneManager;

public class AuctionDetailController implements Initializable {

  private static final Logger logger = LoggerFactory.getLogger(AuctionDetailController.class);

  private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern(
      "dd/MM/yyyy HH:mm");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(
      "dd/MM/yyyy HH:mm:ss");
  private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(
      new Locale("vi", "VN"));

  @FXML
  private Label lblStatusBadge;
  ;
  @FXML
  Button btnSignOut;
  @FXML
  private Label lblCategory;
  @FXML
  private Label lblItemName;
  @FXML
  private Label lblDescription;
  @FXML
  private Label lblSeller;
  @FXML
  private Label lblStartPrice;
  @FXML
  private Label lblStartTime;
  @FXML
  private Label lblEndTime;
  @FXML
  private Label lblItemId;
  @FXML
  private Label lblUsername;

  @FXML
  private Label lblCurrentPrice;
  @FXML
  private Label lblBidCount;
  @FXML
  private Label lblCountdown;
  @FXML
  private Label lblCountdownLabel;
  @FXML
  private Button btnPlaceBid;
  @FXML
  private Button btnBack;
  @FXML
  private Label lblAuctionIdBar;
  @FXML
  private Label lblConnectionStatus;
  @FXML
  private BorderPane rootBorderPane;
  @FXML
  private ImageView imvProductImage;


  private Auction auction;
  private User currentUser;
  private ScheduledExecutorService scheduler;
  private final SceneManager sceneManager = new SceneManager(getClass());

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    Platform.runLater(() -> {
      if (rootBorderPane != null) {
        Stage stage = (Stage) rootBorderPane.getScene().getWindow();
        stage.setFullScreen(true);
      }
    });
  }

  private String comingFromFxml;
  private String comingFromTitle;

  public void setData(Auction auction, User currentUser,
      String comingFromFxml, String comingFromTitle) {
    logger.info("received auction data: {}", auction != null ? auction.getId() : "NULL");
    this.auction = auction;
    this.currentUser = currentUser;
    this.comingFromFxml = comingFromFxml;
    this.comingFromTitle = comingFromTitle;
    populateView();
    startAutoRefresh();
  }

  private void populateView() {
    if (currentUser != null) {
      lblUsername.setText(currentUser.getName());
    } else {
      lblUsername.setText("Guest");
      logger.warn("Warning: currentUser is null");
    }

    updateStatusBadge();

    lblCategory.setText(auction.getItem().getCategory());

    lblItemId.setText(String.valueOf(auction.getItem().getId()));
    lblItemName.setText(auction.getItem().getItemName());
    lblDescription.setText(auction.getItem().getDescription());

    String sellerName = (auction.getSeller() != null) ? auction.getSeller().getName() : "Unknown";
    lblSeller.setText(sellerName);
    lblStartPrice.setText(formatPrice(auction.getItem().getStartingPrice()) + " ₫");
    lblStartTime.setText(auction.getItem().getStartTime() != null
        ? auction.getItem().getStartTime().format(DT_FORMAT) : "—");
    lblEndTime.setText(auction.getItem().getEndTime() != null
        ? auction.getItem().getEndTime().format(DT_FORMAT) : "—");
    lblAuctionIdBar.setText("Auction ID: #" + auction.getId());

    // Display product image if available
    String imagePath = auction.getItem().getImagePath();
    logger.debug("[AuctionDetail] Image path: {}", imagePath);
    if (imagePath != null && !imagePath.isEmpty()) {
      try {
        File imageFile = new File(imagePath);
        logger.debug("[AuctionDetail] File exists: {}, Absolute path: {}", imageFile.exists(),
            imageFile.getAbsolutePath());
        if (imageFile.exists()) {
          Image image = new Image(imageFile.toURI().toString());
          imvProductImage.setImage(image);
          imvProductImage.setPreserveRatio(true);
          imvProductImage.setFitHeight(200);
        } else {
          logger.error("[AuctionDetail] Image file not found: {}", imagePath);
        }
      } catch (Exception e) {
        logger.error("[AuctionDetail] Error loading product image: {}", e.getMessage());
        e.printStackTrace();
      }
    } else {
      logger.debug("[AuctionDetail] Image path is null or empty");
    }

    updatePriceArea();
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
    lblBidCount.setText(auction.getBids().size() + " bids placed");
  }

  @FXML
  private void onPlaceBid(ActionEvent event) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/vn/edu/vnu/auctionclient/bidding.fxml"));
      Scene scene = new Scene(loader.load());

      BiddingController ctrl = loader.getController();
      ctrl.setData(
          auction,
          (Bidder) currentUser,
          "auction_detail.fxml",  // ← Back từ Bidding → quay lại AuctionDetail
          "Auction Detail"
      );

      Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
      stage.setScene(scene);
      stage.setTitle("Bidding");
      if (stage.getWidth() < 800) {
        stage.setWidth(1280);
        stage.setHeight(720);
      }
      stage.show();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void updateBidButton() {
    boolean canBid = auction.getStatus() == RUNNING;
    btnPlaceBid.setDisable(!canBid);
    if (!canBid) {
      btnPlaceBid.setStyle(
          "-fx-background-color: #ccc; -fx-text-fill: white;"
              + "-fx-font-size: 18px; -fx-font-weight: bold;"
              + "-fx-background-radius: 8;");
      btnPlaceBid.setText(
          auction.getStatus() == OPEN ? "⏳ Not Started" : "🔒 Auction Ended");
    }
  }

  private void startAutoRefresh() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(
        () -> Platform.runLater(this::tick),
        0, 1, TimeUnit.SECONDS
    );
  }

  private void tick() {
    updateCountdown();
    long elapsed = Duration.between(auction.getItem().getStartTime(), LocalDateTime.now())
        .getSeconds();
    if (elapsed % 5 == 0) {
      Auction updated = AuctionClientService.getInstance().getAuctionById(auction.getId());
      if (updated != null) {
        this.auction = updated;
        updatePriceArea();
        updateStatusBadge();
        updateBidButton();
        lblConnectionStatus.setText(
            "● Live — updated " + LocalDateTime.now().format(TIME_FORMAT)
        );
      }
    }
  }

  private void updateCountdown() {
    if (auction.getStatus() == FINISHED) {
      lblCountdown.setText("ENDED");
      lblCountdownLabel.setText("TIME REMAINING");
      lblCountdown.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #888;");
      stopAutoRefresh();
      return;
    }

    long totalSecs;
    if (auction.getStatus() == OPEN && auction.getItem().getStartTime() != null) {
      totalSecs = Duration.between(LocalDateTime.now(), auction.getItem().getStartTime())
          .getSeconds();
      lblCountdownLabel.setText("STARTS IN");
      if (totalSecs <= 0) {
        return;
      }
    } else if (auction.getItem().getEndTime() != null) {
      totalSecs = Duration.between(LocalDateTime.now(), auction.getItem().getEndTime())
          .getSeconds();
      lblCountdownLabel.setText("TIME REMAINING");
      if (totalSecs <= 0) {
        lblCountdown.setText("ENDED");
        stopAutoRefresh();
        return;
      }
    } else {
      return;
    }

    long h = totalSecs / 3600;
    long m = (totalSecs % 3600) / 60;
    long s = totalSecs % 60;
    lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));

    if (auction.getStatus() == OPEN) {
      lblCountdown.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1a56db;");
    } else if (totalSecs <= 300) {
      lblCountdown.setStyle(
          "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e53935;"
      );
    } else if (totalSecs <= 1800) {
      lblCountdown.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f57c00;");
    } else {
      lblCountdown.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");
    }
  }

  private void stopAutoRefresh() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
  }

  @FXML
  public void onBack(ActionEvent event) {
    try {
      logger.debug("onBack called, going to: {}", comingFromFxml);
      stopAutoRefresh();
      Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

      if (comingFromFxml.equals("seller_dashboard.fxml")) {
        SellerDashboardController ctrl = sceneManager.switchSceneAndGetController(stage,
            comingFromFxml, comingFromTitle);
        if (ctrl != null && currentUser instanceof Seller) {
          ctrl.setCurrentUser((Seller) currentUser);
        }
      } else if (comingFromFxml.equals("bidder_dashboard.fxml")) {
        BidderDashboardController ctrl = sceneManager.switchSceneAndGetController(stage,
            comingFromFxml, comingFromTitle);
        if (ctrl != null && currentUser instanceof Bidder) {
          ctrl.setCurrent((Bidder) currentUser);
        }
      } else {
        sceneManager.switchScene(stage, comingFromFxml, comingFromTitle);
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Error in onBack: {}", e.getMessage());
    }
  }

  public void onSignOut(ActionEvent event) {
    stopAutoRefresh();
    sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);

  }

  private String formatPrice(double price) {
    return VND_FORMAT.format((long) price);
  }
}
