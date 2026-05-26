package vn.edu.vnu.auction.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import vn.edu.vnu.auction.model.entity.BidTransaction;
import vn.edu.vnu.auction.service.AuctionClientService;
import vn.edu.vnu.auction.service.SceneManager;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static vn.edu.vnu.auction.controller.MainController.LOGIN_FXML;
import static vn.edu.vnu.auction.controller.MainController.LOGIN_TITLE;
import static vn.edu.vnu.auction.model.entity.AuctionStatus.*;

public class BidderDashboardController implements Initializable {
    @FXML private Button btnNavAuctions;
    @FXML private Button btnNavHistory;
    @FXML private Button btnNavWon;
    @FXML private Button btnNavPayment;
    @FXML private Label lblUserName;

    @FXML private Label lblPageTitle;
    @FXML private Label lblSubtitle;
    @FXML private HBox searchBox;
    @FXML private TextField txtSearch;

    @FXML private HBox filterbar;
    @FXML private ToggleGroup categoryGroup;
    @FXML private ToggleButton btnAll;
    @FXML private ToggleButton btnVehicles;
    @FXML private ToggleButton btnElectronics;
    @FXML private ToggleButton btnOthers;
    @FXML private ToggleButton btnArt;
    @FXML private ComboBox<String> cmbStatus;

    @FXML private ScrollPane paneAuctions;
    @FXML private VBox auctionContainer;
    @FXML private VBox paneHistory;
    @FXML private VBox paneWon;
    @FXML private VBox panePayment;

    @FXML private Label lblCount;
    @FXML private Label lblUpdate;

    @FXML
    private ScrollPane historyContainer;

    @FXML private BorderPane rootBorderPane;

    private static final String STYLE_NAV_ACTIVE =
            "-fx-background-color: #722f37; -fx-font-weight: bold; -fx-text-fill: #c0c43f; -fx-background-radius: 10;";
    private static final String STYLE_FILTER_ACTIVE =
            "-fx-background-color: #722f37; -fx-text-fill: #c0c43f; -fx-background-radius: 15; -fx-font-weight: bold;";
    private static final String STYLE_FILTER_NORMAL =
            "-fx-background-color: transparent; -fx-text-fill: #fff; -fx-background-radius: 15; -fx-font-weight: bold;";
    private static final String STYLE_NAV_NORMAL =
            "-fx-background-color: transparent; -fx-font-weight: bold; -fx-text-fill: #fff; -fx-background-radius: 10;";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private Bidder bidder;
    private ObservableList<Auction> allAutions;
    private String currentCategory = "All";
    private String currentStatus = "All Status";
    private ScheduledExecutorService scheduler;

    private final SceneManager sceneManager = new SceneManager(getClass());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        cmbStatus.getSelectionModel().selectFirst();
        loadAuctions();
        startAutoRefresh();
        Platform.runLater(() ->{
            if(rootBorderPane != null) {
                Stage stage = (Stage) rootBorderPane.getScene().getWindow();
                stage.setFullScreen(true);
            }

        });
    }

    public void setCurrent(Bidder bidder){
        this.bidder = bidder;
        lblUserName.setText(bidder.getName());
    }

    @FXML
    public void onNavAuctions(){
        setActiveNav(btnNavAuctions);
        showPane(paneAuctions);
        filterbar.setVisible(true);
        filterbar.setManaged(true);
        searchBox.setVisible(true);
        searchBox.setManaged(true);
        lblPageTitle.setText("Auction Sessions");
        updateSubtitle();
    }

    @FXML
    private void onNavHistory(){
        setActiveNav(btnNavHistory);
        showPane(historyContainer);
        filterbar.setVisible(false);
        filterbar.setManaged(false);
        searchBox.setVisible(false);
        searchBox.setManaged(false);
        lblPageTitle.setText("Bidding History");
        lblSubtitle.setText("Your past Bid");
        lblCount.setText("");
        loadBiddingHistory();
    }

    @FXML
    private void onNavWon(){
        setActiveNav(btnNavWon);
        showPane(paneWon);
        filterbar.setVisible(false);
        filterbar.setManaged(false);
        searchBox.setVisible(false);
        searchBox.setManaged(false);
        lblPageTitle.setText("Won Items");
        lblSubtitle.setText("Auctions you won");
        lblCount.setText("");
        loadWonItems();
    }
    @FXML
    private void onNavPayment(){
        setActiveNav(btnNavPayment);
        showPane(panePayment);
        filterbar.setVisible(false);
        filterbar.setManaged(false);
        searchBox.setVisible(false);
        searchBox.setManaged(false);
        lblPageTitle.setText("Complete Payment");
        lblSubtitle.setText("Pay within 24h to complete your order");
        lblCount.setText("");
        loadPaymentPane();
    }
    @FXML
    private void onSignOut(ActionEvent event){
        stopAutoRefresh();
        sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);
    }

    @FXML
    private void onCategoryAll(){
        currentCategory = "All";
        updateCategoryStyles(btnAll);
        applyFilter();
    }

    @FXML
    private void onCategoryVehicles(){
        currentCategory = "Vehicle";
        updateCategoryStyles(btnVehicles);
        applyFilter();
    }

    @FXML
    private void onCategoryElectronics(){
        currentCategory = "Electronics";
        updateCategoryStyles(btnElectronics);
        applyFilter();
    }

    @FXML
    private void onCategoryArt(){
        currentCategory = "Art";
        updateCategoryStyles(btnArt);
        applyFilter();
    }

    @FXML
    private void onStatusChanged(){
        currentStatus = cmbStatus.getValue();
        applyFilter();
    }

    @FXML
    private void onSearch(){
        applyFilter();
    }

    @FXML
    private void onCategoryOther(){
        currentCategory = "Other";
        updateCategoryStyles(btnOthers);
        applyFilter();
    }

    private void loadAuctions(){
        List<Auction> list = AuctionClientService.getInstance().getAllAuctions();
        allAutions = FXCollections.observableArrayList(list);
        renderAuctions(allAutions);
        updateSubtitle();
    }
    private boolean matchesCategory(Auction a){
        return currentCategory.equals("All") || a.getItem().getCategory().equalsIgnoreCase(currentCategory);
    }
    private void applyFilter(){
        if(allAutions == null){
            return;
        }

        String keyword = txtSearch.getText().trim().toLowerCase();
        List<Auction> result = allAutions.stream()
                .filter(this::matchesCategory)
                .filter(a -> {
                    switch (currentStatus) {
                        case "Live": return RUNNING.equals(a.getStatus());
                        case "Upcoming": return OPEN.equals(a.getStatus());
                        case "Finished": return FINISHED.equals(a.getStatus());
                        default: return true;
                    }
                })
                .filter(a -> keyword.isEmpty()
                || a.getItem().getItemName().contains(keyword)
                || a.getSeller().getName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        renderAuctions(result);

    }

    private void renderAuctions(List<Auction> list) {
        auctionContainer.getChildren().clear();

        if(list.isEmpty()){
            auctionContainer.getChildren().add(buildEmptyState());
        } else {
            list.forEach(a -> auctionContainer.getChildren().add(buildCard(a)));
        }

        lblCount.setText("Showing" + list.size() + " auctions");
    }

    private void updateSubtitle(){
        if(allAutions == null){
            return;
        }
        long live = allAutions.stream()
                .filter(a -> RUNNING.equals(a.getStatus())).count();
        long upcoming = allAutions.stream()
                .filter(a -> OPEN.equals(a.getStatus())).count();
        lblSubtitle.setText(
                "Live:" + live + " · Upcoming:" + upcoming + " · Total:" + allAutions.size()
        );
    }

    private void startAutoRefresh(){
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                () -> Platform.runLater(() -> {
                    List<Auction> updated = AuctionClientService.getInstance().getAllAuctions();
                    allAutions.setAll(updated);
                    applyFilter();
                    updateSubtitle();
                    lblUpdate.setText("Updated " + LocalDateTime.now().format(TIME_FORMAT));
                }),
                30, 30, TimeUnit.SECONDS
        );
    }

    private void stopAutoRefresh(){
        if(scheduler != null && !scheduler.isShutdown()){
            scheduler.shutdownNow();
        }
    }

    private HBox buildCard(Auction auction){
        HBox card = new HBox(14); //spacing
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #722f37, #3d1c21); -fx-border-color: #3d1c21; -fx-border-radius: 8; -fx-background-radius: 8;");

        VBox thumb = buildThumb(auction.getItem().getCategory());

        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(auction.getItem().getItemName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        nameLabel.setStyle("-fx-text-fill: #c0c43f;");
        Label badge = buildStatusBadge(auction.getStatus());
        row1.getChildren().addAll(nameLabel, badge);

        Label sellerLabel = new Label("Seller: " + auction.getSeller().getName());
        sellerLabel.setStyle("-fx-text-fill: #c0c43f; -fx-opacity: 0.7;");

        HBox row2 = new HBox(8);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label(formatPrice(auction.getCurrentPrice()) + " đ");
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        priceLabel.setStyle("-fx-text-fill: #ffffff;");
        Label bidsLabel = new Label("· " + auction.getBids().size() + " bids");
        bidsLabel.setStyle("-fx-text-fill: #c0c43f; -fx-font-size: 11px; -fx-opacity: 0.7;");
        row2.getChildren().addAll(priceLabel, bidsLabel);

        Label startTimeLabel = new Label("Starts: " + (auction.getItem().getStartTime() != null
                ? auction.getItem().getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "—"));
        startTimeLabel.setStyle("-fx-text-fill: #c0c43f; -fx-font-size: 11px; -fx-opacity: 0.7;");

        Label timeLabel = new Label("⏱ " + formatTimeRemaining(auction));
        timeLabel.setStyle(getTimeStyle(auction));

        info.getChildren().addAll(row1, sellerLabel, row2, timeLabel);

        VBox actions = buildCardActions(auction);

        card.getChildren().addAll(thumb, info, actions);
        return card;
    }

    private VBox buildThumb(String category){
        VBox thumb = new VBox(3);
        thumb.setAlignment(Pos.CENTER);
        thumb.setPrefSize(70, 70); // size lý tưởng, đề xuất
        thumb.setStyle("-fx-background-color: #f5e8e8; -fx-background-radius: 8;");

        Label icon = new Label(getCategoryIcon(category));
        icon.setStyle("-fx-font-size: 24px;");
        Label catLabel = new Label(category);
        catLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #722f37;");

        thumb.getChildren().addAll(icon, catLabel);
        return thumb;
    }

    private VBox buildCardActions(Auction auction){
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);
        actions.setPrefWidth(100);

        Button btnBid = new Button(getBidButtonText(auction.getStatus()));
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setStyle(getBidButtonStyle(auction.getStatus()));
        btnBid.setFont(Font.font("System", FontWeight.BOLD, 12));
        btnBid.setOnAction(e -> onBidClicked(auction));

        Button btnDetail = new Button("Details");
        btnDetail.setMaxWidth(Double.MAX_VALUE);
        btnDetail.setStyle("-fx-background-color: transparent; -fx-border-color: #c0c43f; -fx-border-radius: 4; -fx-text-fill: #c0c43f; -fx-font-size: 12px; -fx-cursor: hand;");
        btnDetail.setOnAction(e -> onDetailClicked(auction));
        actions.getChildren().addAll(btnBid, btnDetail);
        return actions;
    }

    private Label buildStatusBadge(AuctionStatus status){
        Label badge = new Label();
        switch (status){
            case RUNNING:
                badge.setText("● Live");
                badge.setStyle("-fx-background-color: #e6f4ea; -fx-text-fill: #2d8a4e; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
                break;
            case OPEN:
                badge.setText("● Upcoming");
                badge.setStyle("-fx-background-color: #e8f0fe; -fx-text-fill: #1a56db; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
                break;
            case FINISHED:
                badge.setText("● Finished");
                badge.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #777; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
                break;
            default:
                badge.setText(status.toString());
                badge.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #777; -fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
        }
        return badge;
    }

    private VBox buildEmptyState(){
        VBox empty = new VBox(10);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(50));

        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size: 40px;");
        Label msg = new Label("No auctions found");
        msg.setFont(Font.font("System", FontWeight.BOLD, 16));
        Label hint = new Label("Try changing the filter or search keyword.");
        hint.setStyle("-fx-text-fill: #c0c43f; -fx-opacity: 0.7;");

        empty.getChildren().addAll(icon, msg, hint);
        return empty;
    }


    private void onBidClicked(Auction auction) {
        if (auction.getStatus() == FINISHED) {
            new Alert(Alert.AlertType.INFORMATION, "This auction has ended.").showAndWait();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/vn/edu/vnu/auctionclient/bidding.fxml"));
            Scene scene = new Scene(loader.load());

            BiddingController ctrl = loader.getController();
            ctrl.setData(
                    auction,
                    bidder,
                    "bidder_dashboard.fxml",  // ← màn quay lại
                    "Bidder Dashboard"
            );

            Stage stage = (Stage) auctionContainer.getScene().getWindow();
            stopAutoRefresh();
            stage.setScene(scene);
            stage.setTitle("Bidding");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load bidding screen: " + e.getMessage()).showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }

    private void onDetailClicked(Auction auction){
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/vn/edu/vnu/auctionclient/auction_detail.fxml"));
            Scene scene = new Scene(loader.load());

            AuctionDetailController ctrl = loader.getController();

            String dashboardFxml = "bidder_dashboard.fxml";
            String dashboardTitle = "Bidder Dashboard";
            ctrl.setData(
                    auction,
                    bidder,
                    dashboardFxml,
                    dashboardTitle
            );

            Stage stage = (Stage) auctionContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Auction detail");
            stage.show();
            stopAutoRefresh();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to load auction detail: " + e.getMessage()).showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).showAndWait();
        }
    }

    private void navigateTo(String fxmlFile, String title){
        Stage stage = (Stage) auctionContainer.getScene().getWindow();
        sceneManager.switchScene(stage, fxmlFile, title);
        stopAutoRefresh();
    }

    //Chỉ hiện pane được chọn, ẩn các pane còn lại
    private void showPane(javafx.scene.Node target){
        paneAuctions.setVisible(false);
        paneAuctions.setManaged(false);
        historyContainer.setVisible(false);
        historyContainer.setManaged(false);
        paneWon.setVisible(false);
        paneWon.setManaged(false);
        panePayment.setVisible(false);
        panePayment.setManaged(false);
        target.setVisible(true);
        target.setManaged(true);
    }

    private void loadBiddingHistory() {
        paneHistory.getChildren().clear();

        if (bidder == null) {
            Label error = new Label("Bidder information not available");
            error.setStyle("-fx-text-fill: #e53935; -fx-font-size: 14px;");
            paneHistory.getChildren().add(error);
            return;
        }

        try {
            List<BidTransaction> history =
                    AuctionClientService.getInstance().getBidderHistory(bidder.getId());

            // Add column headers
            HBox headerRow = new HBox(15);
            headerRow.setAlignment(Pos.CENTER_LEFT);
            headerRow.setPadding(new Insets(10, 0, 10, 0));
            headerRow.setStyle("-fx-border-color: transparent transparent #c0c43f transparent; -fx-border-width: 0 0 2 0;");

            Label lblItemHeader = new Label("ITEM");
            lblItemHeader.setFont(Font.font("System", FontWeight.BOLD, 10));
            lblItemHeader.setStyle("-fx-text-fill: #c0c43f;");
            lblItemHeader.setPrefWidth(200);

            Label lblAuctionIdHeader = new Label("AUCTION ID");
            lblAuctionIdHeader.setFont(Font.font("System", FontWeight.BOLD, 10));
            lblAuctionIdHeader.setStyle("-fx-text-fill: #c0c43f;");
            lblAuctionIdHeader.setPrefWidth(100);

            Label lblAmountHeader = new Label("AMOUNT");
            lblAmountHeader.setFont(Font.font("System", FontWeight.BOLD, 10));
            lblAmountHeader.setStyle("-fx-text-fill: #c0c43f;");
            lblAmountHeader.setPrefWidth(150);

            Label lblTimeHeader = new Label("TIME");
            lblTimeHeader.setFont(Font.font("System", FontWeight.BOLD, 10));
            lblTimeHeader.setStyle("-fx-text-fill: #c0c43f;");
            lblTimeHeader.setPrefWidth(150);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            headerRow.getChildren().addAll(lblItemHeader, lblAuctionIdHeader, spacer, lblAmountHeader, lblTimeHeader);
            paneHistory.getChildren().add(headerRow);

            if (history.isEmpty()) {
                Label empty = new Label("No records found yet.");
                empty.setStyle("-fx-text-fill: #c0c43f; -fx-font-size: 14px;");
                paneHistory.getChildren().add(empty);
                return;
            }

            for (BidTransaction bid : history) {
                HBox row = new HBox(15);
                HBox.setHgrow(paneHistory, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 0, 10, 0));
                row.setStyle("-fx-border-color: transparent transparent #c0c43f transparent; -fx-border-width: 0 0 1 0;");

                Label lblItem = new Label(bid.getItem() != null ? bid.getItem().getItemName() : "Unknown Item");
                lblItem.setFont(Font.font("System", FontWeight.BOLD, 13));
                lblItem.setStyle("-fx-text-fill: #fff;");
                lblItem.setPrefWidth(200);

                Label lblAuctionId = new Label(String.valueOf(bid.getAuctionId()));
                lblAuctionId.setStyle("-fx-font-size: 11px; -fx-text-fill: #c0c43f; -fx-opacity: 0.7;");
                lblAuctionId.setPrefWidth(100);

                // Amount
                Label lblAmount = new Label(formatPrice(bid.getAmount()) + " ₫");
                lblAmount.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #c0c43f;");
                lblAmount.setPrefWidth(150);

                // Time
                Label lblTime = new Label(bid.getTimestamp() != null ? bid.getTimestamp().format(TIME_FORMAT) : "—");
                lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #c0c43f; -fx-opacity: 0.7");
                lblTime.setPrefWidth(150);

                spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                row.getChildren().addAll(lblItem, lblAuctionId, spacer, lblAmount, lblTime);
                paneHistory.getChildren().add(row);
            }

            lblCount.setText(history.size() + " records");
        } catch (Exception e) {
            Label error = new Label("Failed to load history: " + e.getMessage());
            error.setStyle("-fx-text-fill: #e53935; -fx-font-size: 14px;");
            paneHistory.getChildren().add(error);
            e.printStackTrace();
        }
    }

    private void loadWonItems() {
        paneWon.getChildren().clear();

        if (bidder == null) {
            Label error = new Label("Bidder information not available");
            error.setStyle("-fx-text-fill: #e53935; -fx-font-size: 14px;");
            paneWon.getChildren().add(error);
            return;
        }

        try {
            List<AuctionResult> wonAuctions = new ArrayList<>();

            if (wonAuctions.isEmpty()) {
                Label empty = new Label("No won items yet.");
                empty.setStyle("-fx-text-fill: #c0c43f; -fx-font-size: 14px;");
                paneWon.getChildren().add(empty);
                return;
            }

            for (AuctionResult result : wonAuctions) {
                HBox row = new HBox(15);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 0, 10, 0));
                row.setStyle("-fx-border-color: transparent transparent #c0c43f transparent; -fx-border-width: 0 0 1 0;");

                // Item name
                Label lblItem = new Label(result.getItem() != null ? result.getItem().getItemName() : "Unknown Item");
                lblItem.setFont(Font.font("System", FontWeight.BOLD, 13));
                lblItem.setStyle("-fx-text-fill: #fff;");
                lblItem.setPrefWidth(200);

                // Auction ID
                Label lblAuctionId = new Label(String.valueOf(result.getAuctionId()));
                lblAuctionId.setStyle("-fx-font-size: 11px; -fx-text-fill: #c0c43f; -fx-opacity: 0.7;");
                lblAuctionId.setPrefWidth(100);

                // Final price
                Label lblPrice = new Label(formatPrice(result.getFinalPrice()) + " ₫");
                lblPrice.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #c0c43f;");
                lblPrice.setPrefWidth(150);

                // Status
                Label lblStatus = new Label(result.getStatus() != null ? result.getStatus().toString() : "—");
                lblStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #4ade80; -fx-font-weight: bold;");

                // End time
                Label lblTime = new Label(result.getEndTime() != null ? result.getEndTime().format(TIME_FORMAT) : "—");
                lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #c0c43f; -fx-opacity: 0.7");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                row.getChildren().addAll(lblItem, lblAuctionId, spacer, lblPrice, lblStatus, lblTime);
                paneWon.getChildren().add(row);
            }

            lblCount.setText(wonAuctions.size() + " items");
        } catch (Exception e) {
            Label error = new Label("Failed to load won items: " + e.getMessage());
            error.setStyle("-fx-text-fill: #e53935; -fx-font-size: 14px;");
            paneWon.getChildren().add(error);
            e.printStackTrace();
        }
    }

    private  void loadPaymentPane(){
        panePayment.getChildren().clear();
        if(bidder == null) return;
        try{
            List<AuctionResult> wonList = AuctionClientService.getInstance().getWonAuctions(bidder.getId());
            if (wonList == null || wonList.isEmpty()) {
                return;
            }

            List<AuctionResult> unpaid = wonList.stream()
                    .filter(result -> result.getStatus() == FINISHED)
                    .collect(Collectors.toList());
            if(unpaid.isEmpty()){
                Label empty = new Label("No pending payments");
                empty.setStyle("-fx-text-fill: #c0c43f; -fx-font-size: 14px;");
                panePayment.getChildren().add(empty);
                return;
            }
            for (AuctionResult result : unpaid){
                panePayment.getChildren().add(buildPaymentCard(result));
            }
        } catch (Exception e) {
            Label error = new Label("Failed to load payment items: " + e.getMessage());
            error.setStyle("-fx-text-fill: #e53935; -fx-font-size: 14px;");
            panePayment.getChildren().add(error);
            e.printStackTrace();
        }
    }

    private VBox buildPaymentCard(AuctionResult result){
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #722f37, #3d1c21);"
                + "-fx-background-radius: 8; -fx-border-color: #c0c43f;"
                + "-fx-border-radius: 8; -fx-border-width: 1;");

        Label lblItem = new Label(result.getItem() != null
                        ? result.getItem().getItemName() : "Unknow Item");
        lblItem.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblItem.setStyle("-fx-text-fill: #c0c43f;");

        Label lblPrice = new Label("Final price: "
                        + formatPrice(result.getFinalPrice()) + " ₫");
        lblPrice.setStyle("-fx-text-fill: #fff; -fx-font-size: 13px;");

        Label lblDeadline = new Label("⚠ Pay within 24 hours or order will be cancelled");
        lblDeadline.setStyle("-fx-text-fill: #f57c00; -fx-font-size: 11px;");

        Button btnPay = new Button("💳 Pay Now");
        btnPay.setStyle("-fx-background-color: #c0c43f; -fx-text-fill: #722f37;"
                + "-fx-font-weight: bold; -fx-background-radius: 6;"
                + "-fx-cursor: hand; -fx-font-size: 13px;");
        btnPay.setOnAction(event -> onPayClicked(result, card, btnPay));
        card.getChildren().addAll(lblItem,lblPrice, lblDeadline, btnPay);
        return  card;
    }

    private void onPayClicked(AuctionResult result, VBox card, Button btn){
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Payment");
        confirm.setHeaderText(null);
        confirm.setContentText("Pay " + formatPrice(result.getFinalPrice())
                + " ₫ for \"" + result.getItem().getItemName() + "\"?");
        confirm.showAndWait().ifPresent(reponse -> {
                    try {
                        AuctionClientService.getInstance().payAuction(result.getAuctionId());

                        btn.setText("✅ Paid");
                        btn.setDisable(true);
                        btn.setStyle("-fx-background-color: #2d8a4e; -fx-text-fill: #fff;"
                                + "-fx-font-weight: bold; -fx-background-radius: 6;"
                                + "-fx-font-size: 13px;");
                        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #1a3a1a, #0d1f0d);"
                                + "-fx-background-radius: 8; -fx-border-color: #2d8a4e;"
                                + "-fx-border-radius: 8; -fx-border-width: 1;");
                    } catch (RuntimeException e){
                        new Alert(Alert.AlertType.ERROR,
                                "Payment failed: " + e.getMessage()).showAndWait();
                    }
                }
        );
    }

    private void setActiveNav(Button active){
        btnNavWon.setStyle(STYLE_NAV_NORMAL);
        btnNavAuctions.setStyle(STYLE_NAV_NORMAL);
        btnNavHistory.setStyle(STYLE_NAV_NORMAL);
        btnNavPayment.setStyle(STYLE_NAV_NORMAL);

        active.setStyle(STYLE_NAV_ACTIVE);

    }

    private  void updateCategoryStyles(ToggleButton active){
        btnArt.setStyle(STYLE_FILTER_NORMAL);
        btnElectronics.setStyle(STYLE_FILTER_NORMAL);
        btnVehicles.setStyle(STYLE_FILTER_NORMAL);
        btnOthers.setStyle(STYLE_FILTER_NORMAL);
        btnAll.setStyle(STYLE_FILTER_NORMAL);

        active.setStyle(STYLE_FILTER_ACTIVE);
    }

    private String getCategoryIcon(String category){
        if(category == null){
            return "📦";
        }
        switch (category.toLowerCase()){
            case "electronics": return "💻";
            case "art":         return "🎨";
            case "vehicle":     return "🚗";
            case "other":       return "📦";
            default:            return "📦";
        }
    }

    private String formatPrice(double price){
        if(price >= 1_000_000_000){
            return String.format("%.1f B", price / 1000000000);
        }
        if(price >= 1_000_000){
            return String.format("%.1f M", price / 1000000);
        }
        return String.format("%.0f", price);
    }

    private String formatTimeRemaining(Auction auction){
        if(auction.getStatus() == FINISHED) {
            return "Ended";
        }

        if(auction.getStatus() == OPEN && auction.getItem().getStartTime() != null){
            long totalSeconds = java.time.Duration.between(LocalDateTime.now(), auction.getItem().getStartTime()).getSeconds();
            if(totalSeconds <= 0){
                return "Starting soon";
            }
            long hours   = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            if(hours > 24){
                return "Starts in " + (hours / 24) + " days";
            }
            if(hours > 0) return "Starts in " + hours + "h " + minutes + "m";
            return "Starts in " + minutes + "m " + seconds + "s";
        }

        LocalDateTime end = auction.getItem().getEndTime();
        if(end == null){
            return "--";
        }
        long totalSeconds = java.time.Duration.between(LocalDateTime.now(), end).getSeconds();
        if(totalSeconds <= 0){
            return "Ended";
        }
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if(hours > 24){
            return (hours / 24) + " days left";
        }
        if(hours > 0) return hours + "h " + minutes + "m left";
        return minutes + "m " + seconds + "s left";
    }

    private String getTimeStyle(Auction auction){
        if(auction.getStatus() == FINISHED){
            return "-fx-text-fill: #999; -fx-font-size: 12px;";
        }

        if(auction.getStatus() == OPEN){
            return "-fx-text-fill: #1a56db; -fx-font-weight: bold; -fx-font-size: 12px;";
        }

        if(auction.getItem().getEndTime() == null){
            return "-fx-text-fill: #999; -fx-font-size: 12px;";
        }

        long minutes = java.time.Duration.between(LocalDateTime.now(), auction.getItem().getEndTime()).toMinutes();
        if(minutes <= 5 ){
            return "-fx-text-fill: #e53935; -fx-font-weight: bold; -fx-font-size: 12px;";
        }

        if(minutes <= 30){
            return "-fx-text-fill: #f57c00; -fx-font-weight: bold; -fx-font-size: 12px;";
        }
        return "-fx-text-fill: #fff; -fx-font-size: 12px; -fx-opacity: 0.7;";
    }

    private String getBidButtonText(AuctionStatus status){
        switch (status){
            case RUNNING: return "Place Bid";
            case OPEN: return "Preview";
            default: return "View Result";
        }
    }

    private String getBidButtonStyle(AuctionStatus status){
        switch (status){
            case RUNNING:
                return "-fx-background-color: #c0c43f; -fx-text-fill: #722f37; -fx-background-radius: 4; -fx-font-size: 12px;";
            case OPEN:
                return "-fx-background-color: transparent; -fx-text-fill: #c0c43f; -fx-background-radius: 4; -fx-font-size: 12px; -fx-border-color: #c0c43f; -fx-border-radius: 4";
            default:
                return "-fx-background-color: #999; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 12px;";
        }
    }
}
