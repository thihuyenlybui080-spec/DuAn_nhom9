package org.example.loginregister.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.loginregister.client.service.SceneManager;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.user.User;
import org.example.loginregister.server.util.AuctionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.example.loginregister.client.controller.MainController.LOGIN_FXML;
import static org.example.loginregister.client.controller.MainController.LOGIN_TITLE;
import static org.example.loginregister.server.model.entity.Auction.*;

public class BidderDashboardController implements Initializable {
    @FXML private Button btnNavAuctions;
    @FXML private Button btnNavHistory;
    @FXML private Button btnNavWon;
    @FXML private Label lbtlUserName;

    @FXML private Label lblPageTitle;
    @FXML private Label lblSubtitle;
    @FXML private HBox searchBox;
    @FXML private TextField txtSearch;

    @FXML private HBox filterbar;
    @FXML private ToggleGroup categoryGroup;
    @FXML private ToggleButton btnAll;
    @FXML private ToggleButton btnVehicles;
    @FXML private ToggleButton btnElectronics;
    @FXML private ToggleButton btnArt;
    @FXML private ComboBox<String> cmbStatus;

    @FXML private ScrollPane paneAuctions;
    @FXML private VBox auctionContainer;
    @FXML private VBox paneHistory;
    @FXML private VBox paneWon;

    @FXML private Label lblCount;
    @FXML private Label lblUpdate;

    private static final String STYLE_NAV_ACTIVE =
            "-fx-background-color: #8b3a44; -fx-font-weight: bold;";
    private static final String STYLE_FILTER_ACTIVE =
            "-fx-background-color: #722f37; -fx-text-fill: white;";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private User currentUser;
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
    }

    public void setCurrent(User user){
        this.currentUser = user;
        lbtlUserName.setText(user.getFullname());
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
        showPane(paneHistory);
        filterbar.setVisible(false);
        filterbar.setManaged(false);
        searchBox.setVisible(false);
        searchBox.setManaged(false);
        lblPageTitle.setText("Bidding History");
        lblSubtitle.setText("Your past Bid");
        lblCount.setText("");
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
    }
    @FXML
    private void onSignOut(ActionEvent event){
        stopAutoRefresh();
        sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);
    }

    @FXML
    private void onCategoryAll(){
        currentCategory = "All";
        updateCaegoryStyles(btnAll);
        applyFilter();
    }

    @FXML
    private void onCategoryVehicles(){
        currentCategory = "Vehicle";
        updateCaegoryStyles(btnVehicles);
        applyFilter();
    }

    @FXML
    private void onCategoryElectronics(){
        currentCategory = "Vehicle";
        updateCaegoryStyles(btnElectronics);
        applyFilter();
    }

    @FXML
    private void onCategoryArt(){
        currentCategory = "Vehicle";
        updateCaegoryStyles(btnArt);
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

    private void loadAuctions(){
        List<Auction> list = AuctionManager.getInstance().getActiveAuctions();
        // để khi thêm sửa xóa dữ liệu trong danh sách thì javFX sẽ tự động vẽ lại
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

    // hiển thị lên màn hình
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
                    List<Auction> updated = AuctionManager.getInstance().getActiveAuctions();
                    allAutions.setAll(updated);
                    applyFilter();
                    updateSubtitle();
                    lblSubtitle.setText("Updated " + LocalDateTime.now().format(TIME_FORMAT));
                }),
                5, 5, TimeUnit.SECONDS
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
        card.setStyle("-fx-background-color: #ffffff;"
                + "-fx-border-color: #ddd;" // xám nhạt
                + "-fx-border-radius: 8;" //bo tròn
                + "-fx-background-radius: 8;");

        VBox thumb = buildThumb(auction.getItem().getCategory());

        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(auction.getItem().getItemName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        Label badge = buildStatusBadge(auction.getStatus());
        row1.getChildren().addAll(nameLabel, badge);

        Label sellerLabel = new Label("Seller: " + auction.getSeller().getName());
        sellerLabel.setStyle("-fx-text-fill: #777;");

        HBox row2 = new HBox(8);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label(formatPrice(auction.getCurrentPrice()) + " đ");
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        priceLabel.setStyle("-fx-text-fill: #722f37;");
        Label bidsLabel = new Label("· " + auction.getBids().size() + " bids");
        bidsLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
        row2.getChildren().addAll(priceLabel, bidsLabel);

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
        btnDetail.setStyle("-fx-background-color: transparent;"
                + "-fx-border-color: #722f37;"
                + "-fx-border-radius: 4;"
                + "-fx-text-fill: #722f37;"
                + "-fx-font-size: 12px;");
        btnDetail.setOnAction(e -> onDetailClicked(auction));

        actions.getChildren().addAll(btnBid, btnDetail);
        return actions;
    }

    private Label buildStatusBadge(String status){
        Label badge = new Label();
        switch (status){
            case RUNNING:
                badge.setText("● Live");
                badge.setStyle("-fx-background-color: #e6f4ea; -fx-text-fill: #2d8a4e;"
                        + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
                break;
            case OPEN:
                badge.setText("● Upcoming");
                badge.setStyle("-fx-background-color: #e6f4ea; -fx-text-fill: #2d8a4e;"
                        + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
                break;
            case FINISHED:
                badge.setText("● Finished");
                badge.setStyle("-fx-background-color: #e6f4ea; -fx-text-fill: #2d8a4e;"
                        + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
                break;
            default:
                badge.setText(status.toString());
                badge.setStyle(
                        "-fx-background-color: #f0f0f0; -fx-text-fill: #777;"
                                + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
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
        hint.setStyle("-fx-text-fill: #999;");

        empty.getChildren().addAll(icon, msg, hint);
        return empty;
    }

    private void onBidClicked(Auction auction){
        if(auction.getStatus() == FINISHED) {
            new Alert(Alert.AlertType.INFORMATION, "This auction has ended.").showAndWait();
            return;
        }
        navigateTo("bidding.fxml"); //truyền vào màn bidding
    }

    private void onDetailClicked(Auction auction){
        navigateTo("auction_detail.fxml"); // truyền vào màn chi tiết, mô tả

    }

    private void navigateTo(String fxmlFile){
        stopAutoRefresh();

    }

    //Chỉ hiện pane được chọn, ẩn các pane còn lại
    private void showPane(javafx.scene.Node target){
        paneAuctions.setVisible(false);
        paneHistory.setVisible(false);
        paneWon.setVisible(false);
        target.setVisible(true);
    }

    private void setActiveNav(Button active){
        active.setStyle(STYLE_NAV_ACTIVE);
    }

    private  void updateCaegoryStyles(ToggleButton active){
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
        if(hours > 0) return hours + " " + minutes + " left";
        return minutes + " " + seconds + " left";
    }

    private String getTimeStyle(Auction auction){
        if(auction.getStatus() != RUNNING || auction.getItem().getEndTime() == null){
            return "-fx-text-fill: #999; -fx-font-size: 12px;";
        }

        long minutes = java.time.Duration.between(LocalDateTime.now(), auction.getItem().getEndTime()).toMinutes();
        if(minutes <= 5 ){
            return "-fx-text-fill: #e53935; -fx-font-weight: bold; -fx-font-size: 12px;";
        }

        if(minutes <= 30){
            return "-fx-text-fill: #f57c00; -fx-font-weight: bold; -fx-font-size: 12px;";
        }
        return "-fx-text-fill: #555; -fx-font-size: 12px;";
    }

    private String getBidButtonText(String status){
        switch (status){
            case RUNNING: return "Place Bid";
            case OPEN: return "Preview";
            default: return "View Result";
        }
    }

    private String getBidButtonStyle(String status){
        switch (status){
            case RUNNING:
                return "-fx-background-color: #722f37; -fx-text-fill: white;"
                        + "-fx-background-radius: 4; -fx-font-size: 12px;";
            case OPEN:
                return "-fx-background-color: #1a56db; -fx-text-fill: white;"
                        + "-fx-background-radius: 4; -fx-font-size: 12px;";
            default:
                return "-fx-background-color: #999; -fx-text-fill: white;"
                        + "-fx-background-radius: 4; -fx-font-size: 12px;";
        }
    }





}
