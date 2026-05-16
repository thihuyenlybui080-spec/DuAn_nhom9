package org.example.loginregister.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.loginregister.client.service.AuctionClientService;
import org.example.loginregister.client.service.NotificationListener;
import org.example.loginregister.client.service.SceneManager;
import org.example.loginregister.common.exception.AuctionClosedException;
import org.example.loginregister.common.exception.InvalidBidException;
import org.example.loginregister.common.network.NotificationMessage;
import org.example.loginregister.common.observer.Observer;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionResult;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.BidTransaction;
import org.example.loginregister.server.model.entity.auto_bidding.AutoBidConfig;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.util.AuctionHistoryManager;
import org.example.loginregister.server.util.AuctionManager;

import java.net.URL;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
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

    static final String BIDDER_DASHBOARD_FXML  = "bidder_dashboard.fxml";
    static final String BIDDER_DASHBOARD_TITLE = "Bidder Dashboard";
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

    @FXML private Label lblSeller;
    @FXML private Label lblStartPrice;
    @FXML private Label lblEndTime;
    @FXML private Label lblAuctionId;
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

    // Auto-Bid
    @FXML private CheckBox chkAutoBid;
    @FXML private VBox autoBidForm;
    @FXML private TextField txtMaxBid;
    @FXML private TextField txtIncrement;
    @FXML private Button btnEnableAutoBid;
    @FXML private Label lblAutoBidStatus;
    @FXML private Label lblAutoBidHint;

    // ── FXML – Status bar ─────────────────────────────────────────────────────

    @FXML private Label lblConnectionStatus;
    @FXML private Label lblLastUpdate;
    @FXML private BorderPane rootBorderPane;

    private Auction auction;
    private Bidder bidder;
    private boolean autoBidEnable = false;
    private ScheduledExecutorService scheduler;

    private final SceneManager sceneManager = new SceneManager(getClass());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        Platform.runLater(() ->{
            if(rootBorderPane != null) {
                Stage stage = (Stage) rootBorderPane.getScene().getWindow();
                stage.setFullScreen(true);
            }
        });
    }

    public void setData(Auction auction, Bidder bidder, String fromFXML, String fromTitle){
        this.auction = auction;
        this.bidder = bidder;
        this.comingFromFile = fromFXML;
        this.comingFromTitle = fromTitle;
        if(this.auction != null){
            this.auction.addObserver(this);
        }
        populateView();
        AuctionClientService.getInstance().watchAuction(auction.getId());
        NotificationListener.getInstance().register(auction.getId(), notification -> {
            switch (notification.getType()){
                case NotificationMessage.TYPE_BID_UPDATED:
                    Auction updated = (Auction) notification.getData();
                    Platform.runLater(() -> {
                        this.auction = updated;
                        updatePriceArea();
                        refreshBidHistory();
                        updateStatusBadge();
                        updateBidButton();
                    });
                    break;
                case NotificationMessage.TYPE_AUCTION_ENDED:
                    Platform.runLater(() -> {
                        this.auction = (Auction) notification.getData();
                        updateBidButton();
                        updateStatusBadge();
                        lblCountdown.setText("ENDED");
                    });
                    break;
            }
        } );
        startAutoRefresh();
    }
    private void populateView() {
        lblUsername.setText(bidder.getFullname());
        lblItemName.setText(auction.getItem().getItemName());
        lblCategory.setText(auction.getItem().getCategory());
        lblSeller.setText(auction.getSeller().getFullname());
        lblStartPrice.setText(formatPrice(auction.getItem().getStartingPrice()) + " ₫");
        lblEndTime.setText(auction.getItem().getEndTime() != null
                ? auction.getItem().getEndTime().format(DT_FORMAT) : "—");
        lblAuctionId.setText("#" + auction.getId());
        updateStatusBadge();
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
        if(auction.getHighestBidder() != null){
            lblLeader.setText("Leader: 👑 " + auction.getHighestBidder().getName());
        } else{
            lblLeader.setText("Leader: __");
        }

        double minBid = auction.getCurrentPrice() + 1;
        lblMinBid.setText("Min bid: " + formatPrice(minBid) + " ₫");
        lblBidCount.setText(auction.getBids().size() + " bids");
    }

    private void updateBidButton(){
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

        try{
            AuctionClientService.getInstance().placeBid(auction.getId(), bidder.getId(), amount);
            txtBidAmount.clear();
            updatePriceArea();
            refreshBidHistory();
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
            bidder.disableAutoBid(auction.getId());
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

        bidder.enableAutoBid(auction, new AutoBidConfig(maxBid, increment));

        autoBidEnable = true;
        lblAutoBidStatus.setText("✅ Active — Max: " + formatPrice(maxBid) + " ₫"
                + "  Inc: " + formatPrice(increment) + " ₫");
        btnEnableAutoBid.setText("✅ Auto-Bid Active");
        btnEnableAutoBid.setStyle("-fx-background-color: #052e16; -fx-text-fill: #34d399;"
                + "-fx-font-size: 12px; -fx-font-weight: bold;"
                + "-fx-background-radius: 6;");

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
    private void refreshBidHistory(){
        List<BidTransaction> txList = AuctionClientService.getInstance().getBidHistory(auction.getId());
        bidHistoryContainer.getChildren().clear();

        if(txList.isEmpty()){
            Label empty = new Label("No bids yet. Be the first!");
            empty.setStyle("-fx-text-fill: #c0c43f; -fx-font-size: 12px;");
            empty.setPadding(new Insets(8, 0, 0, 0));
            bidHistoryContainer.getChildren().add(empty);
            return;
        }

        for(int i = 0; i < txList.size(); i++){
            bidHistoryContainer.getChildren().add(buildBidRow(txList.get(i), i == 0));
        }

    }

    private HBox buildBidRow(BidTransaction tx, boolean isTop){
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 0, 6, 0));
        row.setStyle(
                "-fx-border-color: transparent transparent #c0c43f transparent;"
                        + "-fx-border-width: 0 0 1 0;");

        String nameText = isTop
                ? "👑 " + tx.getBidder().getFullname()
                : tx.getBidder().getFullname();

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
        sceneManager.switchScene(event, comingFromFile, comingFromTitle);
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
