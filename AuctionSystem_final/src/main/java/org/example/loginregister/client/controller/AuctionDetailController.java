package org.example.loginregister.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.example.loginregister.client.service.AuctionClientService;
import org.example.loginregister.client.service.SceneManager;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.User;
import org.example.loginregister.server.util.AuctionManager;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.example.loginregister.client.controller.MainController.LOGIN_FXML;
import static org.example.loginregister.client.controller.MainController.LOGIN_TITLE;
import static org.example.loginregister.server.model.entity.Auction.*;
import static org.example.loginregister.server.model.entity.AuctionStatus.*;

public class AuctionDetailController implements Initializable {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final NumberFormat VND_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML private Label lblStatusBadge;;
    @FXML Button btnSignOut;
    @FXML private Label lblCategory;
    @FXML private Label lblItemName;
    @FXML private Label lblDescription;
    @FXML private Label lblSeller;
    @FXML private Label lblStartPrice;
    @FXML private Label lblStartTime;
    @FXML private Label lblEndTime;
    @FXML private Label lblItemId;
    @FXML private Label lblUsername;

    @FXML private Label lblCurrentPrice;
    @FXML private Label lblBidCount;
    @FXML private Label lblCountdown;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnBack;
    @FXML private Label lblAuctionIdBar;
    @FXML private Label lblConnectionStatus;
    @FXML private BorderPane rootBorderPane;
    @FXML Label lblThumbIcon;


    static final String BIDDER_DASHBOARD_FXML = "bidder_dashboard.fxml";
    static final String BIDDER_DASHBOARD_TITLE = "bidder";

    private Auction auction;
    private User currentUser;
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

    private String comingFromFxml;
    private String comingFromTitle;
    public void setData(Auction auction, User currentUser,
                        String comingFromFxml, String comingFromTitle){
        System.out.println("received auction data: " + (auction != null ? auction.getId() : "NULL"));
        this.auction = auction;
        this.currentUser = currentUser;
        this.comingFromFxml = comingFromFxml;
        this.comingFromTitle = comingFromTitle;
        populateView();
        startAutoRefresh();
    }
    private void populateView(){
        if (currentUser != null) {
            lblUsername.setText(currentUser.getName());
        } else {
            lblUsername.setText("Guest");
            System.out.println("Warning: currentUser is null");
        }

        updateStatusBadge();
        lblThumbIcon.setText(getCategoryIcon(auction.getItem().getCategory()));
        lblCategory.setText(auction.getItem().getCategory());

        lblItemId.setText(auction.getItem().getId());
        lblItemName.setText(auction.getItem().getItemName());
        lblDescription.setText(auction.getItem().getDescription());

        lblSeller.setText(auction.getSeller().getName());
        lblStartPrice.setText(formatPrice(auction.getItem().getStartingPrice()) + " ₫");
        lblStartTime.setText(auction.getItem().getStartTime() != null
                ? auction.getItem().getStartTime().format(DT_FORMAT) : "—");
        lblEndTime.setText(auction.getItem().getEndTime() != null
                ? auction.getItem().getEndTime().format(DT_FORMAT) : "—");
        lblAuctionIdBar.setText("Auction ID: #" + auction.getId());

        updatePriceArea();
        updateBidButton();
    }

    private void updateStatusBadge(){
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
        lblBidCount.setText(auction.getBids().size() + " bids placed");
    }

    @FXML
    private void onPlaceBid(ActionEvent event){
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/loginregister/bidding.fxml"));
            Scene scene = new Scene(loader.load());

            BiddingController ctrl = loader.getController();
            ctrl.setData(
                    auction,
                    (Bidder)currentUser,
                    "auction_detail.fxml",  // ← Back từ Bidding → quay lại AuctionDetail
                    "Auction Detail"
            );

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Bidding");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateBidButton(){
        boolean canBid = auction.getStatus() == RUNNING || auction.getStatus() == OPEN;
        btnPlaceBid.setDisable(!canBid);
        if(!canBid){
            btnPlaceBid.setStyle(
                    "-fx-background-color: #ccc; -fx-text-fill: white;"
                            + "-fx-font-size: 18px; -fx-font-weight: bold;"
                            + "-fx-background-radius: 8;");
            btnPlaceBid.setText(
                    auction.getStatus() == OPEN ? "⏳ Not Started" : "🔒 Auction Ended");
        }
    }

    private void startAutoRefresh(){
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                () -> Platform.runLater(this :: tick),
                0, 1, TimeUnit.SECONDS
        );
    }

    private void tick(){
        updateCountdown();
        long elapsed = Duration.between(auction.getItem().getStartTime(), LocalDateTime.now()).getSeconds();
        if(elapsed % 5 == 0){
            Auction updated = AuctionClientService.getInstance().getAuctionById(auction.getId());
            if(updated != null){
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

    private void updateCountdown(){
        if(auction.getItem().getEndTime() == null
                || auction.getStatus() == FINISHED) {
            lblCountdown.setText("ENDED");
            lblCountdown.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #888;");
            stopAutoRefresh();
            return;
        }
        long totalSecs = Duration.between(LocalDateTime.now(), auction.getItem().getEndTime()).getSeconds();
        if(totalSecs <= 0){
            lblCountdown.setText("ENDED");
            stopAutoRefresh();
            return;
        }

        long  h = totalSecs / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));

        if(totalSecs <= 300){
            lblCountdown.setStyle(
                    "-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e53935;"
            );
        } else if (totalSecs <= 1800) {
            lblCountdown.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #f57c00;");
        }
        else{
            lblCountdown.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");
        }
    }

    private void stopAutoRefresh(){
        if(scheduler != null && !scheduler.isShutdown()){
            scheduler.shutdownNow();
        }
    }

    @FXML
    public void onBack(ActionEvent event){
        stopAutoRefresh();
        sceneManager.switchScene(event, comingFromFxml, comingFromTitle);
    }

    public void onSignOut(ActionEvent event){
        stopAutoRefresh();
        sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);

    }

    private String formatPrice(double price){
        return VND_FORMAT.format((long) price);
    }
    private String getCategoryIcon(String category) {
        if (category == null) {
            return "📦";
        }
        switch (category.toLowerCase()) {
            case "electronics": return "💻";
            case "art":         return "🎨";
            case "vehicle":     return "🚗";
            default:            return "📦";
        }
    }
}
