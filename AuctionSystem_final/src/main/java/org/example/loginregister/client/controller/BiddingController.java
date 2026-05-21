package org.example.loginregister.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.loginregister.client.service.AuctionClientService;
import org.example.loginregister.client.service.NotificationListener;
import org.example.loginregister.client.service.SceneManager;
import org.example.loginregister.server.common.network.NotificationMessage;
import org.example.loginregister.server.common.observer.Observer;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.auto_bidding.AutoBidConfig;
import org.example.loginregister.server.model.entity.user.Bidder;

import java.io.File;
import java.net.URL;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.example.loginregister.client.controller.MainController.LOGIN_FXML;
import static org.example.loginregister.client.controller.MainController.LOGIN_TITLE;

public class BiddingController implements Initializable, Observer {
    private static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final NumberFormat VND_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private String comingFromFile;
    private String comingFromTitle;


    @FXML private Label lblUsername;

    // ── FXML – Topbar ─────────────────────────────────────────────────────────

    @FXML private Label lblItemName;
    @FXML private Label lblCategory;
    @FXML private Label lblCountdown;
    @FXML private Label lblStatusBadge;
    @FXML private Button btnBack;

    // ── FXML – Cột trái ───────────────────────────────────────────────────────

    @FXML private Label lblBidCount;
    @FXML private VBox bidHistoryContainer;
    @FXML private Button btnNavAuctions;
    @FXML private Button btnSignOut;
    @FXML private Button btnNavMyAuctions;

    // ── FXML – Cột phải ───────────────────────────────────────────────────────

    @FXML
    private Label lblCurrentPrice;
    @FXML private Label lblLeader;

    // Place Bid
    @FXML private TextField txtBidAmount;
    @FXML private Label lblMinBid;
    @FXML private Button btnPlaceBid;
    @FXML private Label lblBidError;

    // ── FXML – Auto-Bid ─────────────────────────────────────────────────────

    @FXML private CheckBox chkAutoBid;
    @FXML private VBox autoBidForm;
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Button btnEnableAutoBid;
    @FXML private Label lblAutoBidStatus;
    @FXML private Label lblAutoBidHint;
    @FXML private Label lblAutoBidActiveStatus;
    @FXML private Button btnDisableAutoBid;

    // ── FXML – Status bar ─────────────────────────────────────────────────────

    @FXML private Label lblConnectionStatus;
    @FXML private Label lblLastUpdate;
    @FXML private BorderPane rootBorderPane;
    @FXML private ImageView imvProductImage;

    private Auction auction;
    private Bidder bidder;
    private boolean autoBidEnable = false;
    private ScheduledExecutorService scheduler;
    private List<BidTransaction> localBids = new ArrayList<>();

    private final SceneManager sceneManager = new SceneManager(getClass());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){

    }

    public void setData(Auction auction, Bidder bidder, String fromFXML, String fromTitle){
        this.auction = auction;
        this.bidder = bidder;
        this.comingFromFile = fromFXML;
        this.comingFromTitle = fromTitle;
        Platform.runLater(() -> {
            Stage stage = (Stage) rootBorderPane.getScene().getWindow();
            if (stage != null) stage.setFullScreen(true);
        });
        if(this.auction != null){
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
                        // Only update changed fields, don't replace entire object to preserve bids
                        this.auction.setCurrentPrice(updated.getCurrentPrice());
                        if (updated.getHighestBidder() != null) {
                            this.auction.setHighestBidder(updated.getHighestBidder());
                            this.auction.setHighestBidderName(updated.getHighestBidder().getName());
                        } else if (updated.getBids() != null && !updated.getBids().isEmpty()) {
                            // If highestBidder is null, try to get it from bids list
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
                        // Always sync bids from server when notification is received
                        // This ensures auto-bids and other users' bids are shown immediately
                        if (updated.getBids() != null && !updated.getBids().isEmpty()) {
                            localBids.clear();
                            localBids.addAll(updated.getBids());
                            // Sort bids by amount descending (highest first)
                            localBids.sort((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()));
                        } else {
                            // If server doesn't send bids, reload them explicitly
                            try {
                                List<BidTransaction> bids = AuctionClientService.getInstance().getBidsByAuction(auction.getId());
                                localBids.clear();
                                localBids.addAll(bids);
                                // Sort bids by timestamp descending (newest first)
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
                    });
                    break;
                case NotificationMessage.TYPE_TIME_EXTENDED:
                    Platform.runLater(() -> {
                        Auction extended = (Auction) notification.getData();
                        if (extended.getItem() != null && extended.getItem().getEndTime() != null) {
                            this.auction.getItem().setEndTime(extended.getItem().getEndTime());
                        }
                        lblCountdown.setStyle(
                                "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ff9800;");
                        lblBidError.setText("⏱ Anti-snipe: +60s added!");
                        lblBidError.setStyle("-fx-text-fill: #ff9800; -fx-font-size: 11px;");
                        lblBidError.setVisible(true);
                        lblBidError.setManaged(true);
                    });
                    break;
                case NotificationMessage.TYPE_AUTO_BID_AUCTION_ENDED:
                    Platform.runLater(() -> {
                        String message = (String) notification.getData();
                        lblBidError.setText("🤖 " + message);
                        lblBidError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px; -fx-font-weight: bold;");
                        lblBidError.setVisible(true);
                        lblBidError.setManaged(true);
                        updateBidButton();
                        updateStatusBadge();
                        lblCountdown.setText("ENDED");
                    });
                    break;
            }
            }
         );
        startAutoRefresh();
    }
    private void populateView() {
        lblUsername.setText(bidder.getFullName());
        lblItemName.setText(auction.getItem().getItemName());
        lblCategory.setText(auction.getItem().getCategory());
        updateStatusBadge();
        String imagePath = auction.getItem().getImagePath();
        System.out.println("[BiddingController] Image path: " + imagePath);
        if (imagePath != null && !imagePath.isEmpty()) {
            try {
                File imageFile = new File(imagePath);
                System.out.println("[BiddingController] File exists: " + imageFile.exists() + ", Absolute path: " + imageFile.getAbsolutePath());
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
        // Load bids from database if not already loaded
        System.out.println("[DEBUG] populateView - Initial localBids count: " + localBids.size());
        if (localBids.isEmpty()) {
            try {
                List<BidTransaction> bids = AuctionClientService.getInstance().getBidsByAuction(auction.getId());
                System.out.println("[DEBUG] populateView - Loaded " + bids.size() + " bids from database");
                localBids.addAll(bids);
                // Sort bids by amount descending (highest first)
                localBids.sort((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()));
                System.out.println("[DEBUG] populateView - After loading and sorting, localBids count: " + localBids.size());
                if (!bids.isEmpty()) {
                    for (BidTransaction bid : bids) {
                        System.out.println("[DEBUG] Bid - Amount: " + bid.getAmount() + ", Bidder: " + (bid.getBidder() != null ? bid.getBidder().getName() : "null"));
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

    private void updatePriceArea(){
        lblCurrentPrice.setText(formatPrice(auction.getCurrentPrice()));

        // Try to get leader name from various sources
        String leaderName = null;

        // 1. Try highestBidder object
        if (auction.getHighestBidder() != null) {
            leaderName = auction.getHighestBidder().getName();
            System.out.println("[DEBUG] Leader from highestBidder: " + leaderName);
        }

        // 2. Try from localBids list
        if (leaderName == null && !localBids.isEmpty()) {
            BidTransaction highestBid = localBids.stream()
                    .max((b1, b2) -> Double.compare(b1.getAmount(), b2.getAmount()))
                    .orElse(null);
            if (highestBid != null && highestBid.getBidder() != null) {
                leaderName = highestBid.getBidder().getName();
                System.out.println("[DEBUG] Leader from localBids: " + leaderName);
            }
        }

        // 3. Display result
        if (leaderName != null && !leaderName.isEmpty()) {
            lblLeader.setText("Leader: 👑 " + leaderName);
        } else {
            lblLeader.setText("Leader: __");
            System.out.println("[DEBUG] No leader found. localBids count: " + localBids.size());
        }

        double minBid = auction.getCurrentPrice() + 1;
        lblMinBid.setText("Min bid: " + formatPrice(minBid) + " ₫");
        lblBidCount.setText(localBids.size() + " bids");
    }

    private void updateBidButton(){
        boolean canBid = auction.getStatus() == AuctionStatus.RUNNING || auction.getStatus() == AuctionStatus.OPEN;
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

    @FXML
    private void onPlaceBid(){
        hideBidError();
        String raw = txtBidAmount.getText().trim().replaceAll("[^0-9]", "");

        if(raw.isEmpty()){
            showBidError("Please enter a bid amount");
            return;
        }

        double amount;
        try{
            amount = Double.parseDouble(raw);
        }
        catch (NumberFormatException e) {
            showBidError("Invalid amount.");
            return;
        }

        // Validate bid amount is greater than current price
        if (amount <= auction.getCurrentPrice()) {
            showBidError("Bid must be greater than current price (" + formatPrice(auction.getCurrentPrice()) + " ₫)");
            return;
        }

        try{
            Auction updatedAuction = AuctionClientService.getInstance().placeBid(auction.getId(), bidder.getId(), amount);
            txtBidAmount.clear();
            // Use the returned auction object which has the updated state
            if (updatedAuction != null) {
                this.auction.setCurrentPrice(updatedAuction.getCurrentPrice());
                if (updatedAuction.getHighestBidder() != null) {
                    this.auction.setHighestBidder(updatedAuction.getHighestBidder());
                    this.auction.setHighestBidderName(updatedAuction.getHighestBidder().getName());
                }
                // Update bids from the returned auction
                if (updatedAuction.getBids() != null && !updatedAuction.getBids().isEmpty()) {
                    localBids.clear();
                    localBids.addAll(updatedAuction.getBids());
                    // Sort bids by amount descending (highest first)
                    localBids.sort((b1, b2) -> Double.compare(b2.getAmount(), b1.getAmount()));
                }
            }
            updatePriceArea();
            refreshBidHistory();
            // Show success message
            showBidError("✅ Bid placed successfully: " + formatPrice(amount) + " ₫");
            lblBidError.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 11px;");
        } catch (RuntimeException e) {
            showBidError(e.getMessage());
        }
    }

    @FXML
    private void onToggleAutoBid(){
        boolean on = chkAutoBid.isSelected();
        autoBidForm.setVisible(on);
        autoBidForm.setManaged(on);
        lblAutoBidHint.setVisible(!on);
        lblAutoBidHint.setManaged(!on);

        if(!on){
            autoBidEnable = false;
            AuctionClientService.getInstance().disableAutoBid(auction.getId(), bidder.getId());
            lblAutoBidStatus.setText("");
            btnEnableAutoBid.setText("⚡ Enable Auto-Bid");
            btnEnableAutoBid.setStyle(
                    "-fx-background-color: #1e3a5f; -fx-text-fill: #60a5fa;"
                            + "-fx-font-size: 12px; -fx-font-weight: bold;"
                            + "-fx-background-radius: 6; -fx-cursor: hand;"
            );
        }
    }

    @FXML
    private void onEnableAutoBid(){
        // Check if bidder is locked/banned
        if (!bidder.isActive()) {
            lblAutoBidStatus.setText("Your account is locked and cannot enable auto-bid.");
            lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
            return;
        }

        String maxBidStr = txtMaxBid.getText().trim().replaceAll("[^0-9]", "");
        String incrStr    = txtIncrement.getText().trim().replaceAll("[^0-9]", "");
        if(maxBidStr.isEmpty() || incrStr.isEmpty()){
            lblAutoBidStatus.setText("Please fill in both fields");
            lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
            return;
        }

        double maxBid;
        double increment;
        try{
            maxBid = Double.parseDouble(maxBidStr);
            increment = Double.parseDouble(incrStr);

        } catch (NumberFormatException e) {
            lblAutoBidStatus.setText("Invalid numbers.");
            lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
            return;
        }

        if(maxBid <= auction.getCurrentPrice()){
            lblAutoBidStatus.setText("Max bid must be higher than current price.");
            lblAutoBidStatus.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11px;");
            return;
        }

        AuctionClientService.getInstance().enableAutoBid(auction.getId(), bidder.getId(), maxBid, increment);

        autoBidEnable = true;
        lblAutoBidStatus.setText("✅ Active — Max: " + formatPrice(maxBid) + " ₫"
                + "  Inc: " + formatPrice(increment) + " ₫");
        btnEnableAutoBid.setText("✅ Auto-Bid Active");
        btnEnableAutoBid.setStyle("-fx-background-color: #052e16; -fx-text-fill: #34d399;"
                + "-fx-font-size: 12px; -fx-font-weight: bold;"
                + "-fx-background-radius: 6;");

        // Clear input fields after saving
        txtMaxBid.clear();
        txtIncrement.clear();

        // Show disable button immediately when auto-bid is enabled
        if (btnDisableAutoBid != null) {
            btnDisableAutoBid.setVisible(true);
            btnDisableAutoBid.setManaged(true);
        }

    }
    @Override
    public void update(String auctionId, double newPrice, String highestBidder){
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
        Map<String, Object> status = AuctionClientService.getInstance().checkAutoBid(auction.getId(), bidder.getId());
        if (status != null) {
            boolean isActive = (Boolean) status.get("active");
            Platform.runLater(() -> {
                if (isActive) {
                    double maxBid = (Double) status.get("maxBid");
                    double increment = (Double) status.get("increment");
                    lblAutoBidActiveStatus.setText("✓ Auto-Bid Active (Max: " + VND_FORMAT.format(maxBid) + ")");
                    lblAutoBidActiveStatus.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px; -fx-font-weight: bold;");
                    if (btnDisableAutoBid != null) {
                        btnDisableAutoBid.setVisible(true);
                        btnDisableAutoBid.setManaged(true);
                    }
                    autoBidEnable = true;
                    chkAutoBid.setSelected(true);
                    // Populate form fields with saved values
                    txtMaxBid.setText(VND_FORMAT.format((long) maxBid));
                    txtIncrement.setText(VND_FORMAT.format((long) increment));
                } else {
                    lblAutoBidActiveStatus.setText("");
                    if (btnDisableAutoBid != null) {
                        btnDisableAutoBid.setVisible(false);
                        btnDisableAutoBid.setManaged(false);
                    }
                    autoBidEnable = false;
                    chkAutoBid.setSelected(false);
                    // Clear form fields when no active auto-bid
                    txtMaxBid.clear();
                    txtIncrement.clear();
                }
            });
        }
    }

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

    private HBox buildBidRow(BidTransaction tx, boolean isTop){
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblPrice = new Label(formatPrice(tx.getAmount()) + " ₫");
        lblPrice.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #c0c43f;");

        Label lblTime = new Label(tx.getTimestamp() != null
                ? tx.getTimestamp().format(TIME_FORMAT) : "—");
        lblTime.setStyle("-fx-font-size: 10px; -fx-text-fill: #c0c43f; -fx-opacity: 0.7");

        row.getChildren().addAll(lblName, spacer, lblPrice, lblTime);
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
    }

    private void updateCountdown() {
        if (auction.getItem().getEndTime() == null
                || auction.getStatus() == AuctionStatus.FINISHED) {
            lblCountdown.setText("ENDED");
            lblCountdown.setStyle(
                    "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #722f37;");
            stopScheduler();
            return;
        }

        long totalSecs = Duration
                .between(LocalDateTime.now(), auction.getItem().getEndTime())
                .getSeconds();

        if (totalSecs <= 0) {
            lblCountdown.setText("ENDED");
            stopScheduler();
            return;
        }

        long h = totalSecs / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));
        if (totalSecs <= 300) {
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

    @FXML
    private void onBack(ActionEvent event) {
        stopScheduler();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        if (comingFromFile.equals("bidder_dashboard.fxml")) {
            BidderDashboardController ctrl = sceneManager.switchSceneAndGetController(stage, comingFromFile, comingFromTitle);
            if (ctrl != null) {
                ctrl.setCurrent(bidder);
            }
        } else {
            sceneManager.switchScene(stage, comingFromFile, comingFromTitle);
        }
    }

    @FXML
    private void onNavAuctions(ActionEvent event) {
        onBack(event);
    }

    @FXML
    private void onSignOut(ActionEvent event) {
        stopScheduler();
        sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);
    }

    @FXML
    private void onNavMyAuctions(ActionEvent event){
        sceneManager.switchScene(event, "bidding.fxml", "Bidding");
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void showBidError(String message) {
        lblBidError.setText(message);
        lblBidError.setVisible(true);
        lblBidError.setManaged(true);
    }

    private void hideBidError() {
        lblBidError.setVisible(false);
        lblBidError.setManaged(false);
    }

    // ── Format helpers ────────────────────────────────────────────────────────

    private String formatPrice(double price) {
        return VND_FORMAT.format((long) price);
    }



}
