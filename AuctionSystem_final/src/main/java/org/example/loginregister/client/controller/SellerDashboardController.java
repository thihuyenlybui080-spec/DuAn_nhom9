package org.example.loginregister.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.loginregister.client.service.AuctionClientService;
import org.example.loginregister.client.service.SceneManager;
import org.example.loginregister.server.model.entity.Auction;
import org.example.loginregister.server.model.entity.AuctionStatus;
import org.example.loginregister.server.model.entity.item.Item;
import org.example.loginregister.server.model.entity.user.Seller;
import org.example.loginregister.server.model.entity.user.User;
import org.example.loginregister.server.model.factory.ArtFactory;
import org.example.loginregister.server.model.factory.ElectronicsFactory;
import org.example.loginregister.server.model.factory.ItemFactory;
import org.example.loginregister.server.model.factory.VehicleFactory;
import org.example.loginregister.server.service.AuctionService;
import org.example.loginregister.server.service.ItemService;
import org.example.loginregister.server.util.AuctionManager;


import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.example.loginregister.client.controller.MainController.LOGIN_FXML;
import static org.example.loginregister.client.controller.MainController.LOGIN_TITLE;

/**
 * Controller cho màn hình Seller Dashboard.
 *

 */
public class SellerDashboardController implements Initializable {

    private static final String STYLE_NAV_ACTIVE =
            "-fx-background-color: #722f37; -fx-font-weight: bold; -fx-text-fill: #c0c43f";
    private static final String STYLE_NAV_NORMAL =
            "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final NumberFormat VND_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));


    @FXML
    private Label lblUsername;
    @FXML
    private Button btnNavMyAuctions;
    @FXML
    private Button btnNavMyItems;
    @FXML
    private Button btnNavCreateAuction;

    @FXML
    private Label lblPageTitle;
    @FXML
    private Label lblSubtitle;
    @FXML
    private HBox searchBox;
    @FXML
    private TextField txtSearch;


    @FXML
    private VBox paneMyAuctions;
    @FXML
    private ComboBox<String> cmbAuctionFilter;
    @FXML
    private Label lblAuctionCount;
    @FXML
    private VBox auctionListContainer;


    @FXML
    private VBox paneMyItems;
    @FXML
    private Label lblItemCount;
    @FXML
    private VBox itemListContainer;

    // ── FXML – Pane Create Auction ────────────────────────────────────────────

    @FXML
    private ScrollPane paneCreateAuction;
    @FXML
    private TextField txtItemName;
    @FXML
    private ComboBox<String> cmbCategory;
    @FXML
    private TextArea txtDescription;
    @FXML
    private TextField txtStartingPrice;
    @FXML
    private DatePicker dpStartDate;
    @FXML
    private TextField txtStartTime;
    @FXML
    private DatePicker dpEndDate;
    @FXML
    private TextField txtEndTime;
    @FXML
    private Label lblFormError;
    @FXML
    private BorderPane rootBorderPane;

    @FXML
    private Label lblStatusBar;

    private Seller seller;
    private ObservableList<Auction> myAuctions;
    private ObservableList<Item> myItems;
    private ScheduledExecutorService scheduler;

    private final SceneManager sceneManager = new SceneManager(getClass());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cmbAuctionFilter.getSelectionModel().selectFirst();
        cmbCategory.getSelectionModel().selectFirst();
        Platform.runLater(() -> {
            Stage stage = (Stage) rootBorderPane.getScene().getWindow();
            stage.setFullScreen(true);
        });
    }

    /**
     * Truyền user đang đăng nhập vào controller.
     * Gọi từ {@link LoginController} sau khi load FXML.
     *
     * @param seller Seller vừa đăng nhập
     */
    public void setCurrentUser(Seller seller) {
        this.seller = seller;
        lblUsername.setText(seller.getName());
        loadMyAuctions();
        loadMyItems();
        startAutoRefresh();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void onNavMyAuctions() {
        setActiveNav(btnNavMyAuctions);
        showPane(paneMyAuctions);
        lblPageTitle.setText("My Auctions");
        lblSubtitle.setText("Manage your auction sessions");
        searchBox.setVisible(true);
        searchBox.setManaged(true);
        txtSearch.clear();
        renderAuctions(myAuctions);
    }

    @FXML
    private void onNavMyItems() {
        setActiveNav(btnNavMyItems);
        showPane(paneMyItems);
        lblPageTitle.setText("My Items");
        lblSubtitle.setText("Manage your products");
        searchBox.setVisible(true);
        searchBox.setManaged(true);
        txtSearch.clear();
        renderItems(myItems);
    }

    @FXML
    private void onNavCreateAuction() {
        setActiveNav(btnNavCreateAuction);
        showPane(paneCreateAuction);
        lblPageTitle.setText("Create Auction");
        lblSubtitle.setText("List a new item for auction");
        searchBox.setVisible(false);
        searchBox.setManaged(false);
    }

    @FXML
    private void onSignOut(ActionEvent event) {
        stopAutoRefresh();
        sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);
    }

    // ── My Auctions ───────────────────────────────────────────────────────────

    private void loadMyAuctions() {
        List<Auction> list = AuctionClientService.getInstance().getAuctionsBySeller(seller.getId());
        myAuctions = FXCollections.observableArrayList(list);
        renderAuctions(myAuctions);
    }

    @FXML
    private void onAuctionFilterChanged() {
        applyAuctionFilter();
    }

    @FXML
    private void onSearch() {
        applyAuctionFilter();
    }

    private void applyAuctionFilter() {
        if (myAuctions == null) {
            return;
        }
        String status = cmbAuctionFilter.getValue();
        String keyword = txtSearch.getText().trim().toLowerCase();

        List<Auction> result = myAuctions.stream()
                .filter(a -> {
                    switch (status) {
                        case "Live":
                            return a.getStatus() == AuctionStatus.RUNNING;
                        case "Upcoming":
                            return a.getStatus() == AuctionStatus.OPEN;
                        case "Finished":
                            return a.getStatus() == AuctionStatus.FINISHED;
                        default:
                            return true;
                    }
                })
                .filter(a -> keyword.isEmpty()
                        || a.getItem().getItemName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());

        renderAuctions(result);
    }

    private void renderAuctions(List<Auction> list) {
        auctionListContainer.getChildren().clear();

        if (list == null || list.isEmpty()) {
            auctionListContainer.getChildren().add(buildEmptyState(
                    "📭", "No auctions yet", "Go to \"Create Auction\" to list your first item."));
            lblAuctionCount.setText("0 auctions");
            return;
        }

        list.forEach(a -> auctionListContainer.getChildren().add(buildAuctionCard(a)));
        lblAuctionCount.setText(list.size() + " auction(s)");
    }

    private HBox buildAuctionCard(Auction auction) {
        HBox card = new HBox(14);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #722f37, #3d1c21);"
                + "-fx-border-color: #3d1c21;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;");

        // Thumb
        VBox thumb = new VBox(3);
        thumb.setAlignment(Pos.CENTER);
        thumb.setPrefSize(64, 64);
        thumb.setStyle("-fx-background-color: #f5e8e8; -fx-background-radius: 8;");
        Label icon = new Label(getCategoryIcon(auction.getItem().getCategory()));
        icon.setStyle("-fx-font-size: 22px; -fx-text-fill: #722f37");
        Label cat = new Label(auction.getItem().getCategory());
        cat.setStyle("-fx-font-size: 9px; -fx-text-fill: #722f37;");
        thumb.getChildren().addAll(icon, cat);

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(auction.getItem().getItemName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        Label badge = buildStatusBadge(auction.getStatus());
        row1.getChildren().addAll(nameLabel, badge);

        Label priceLabel = new Label(
                "Current: " + formatPrice(auction.getCurrentPrice()) + " ₫"
                        + "  ·  " + auction.getBids().size() + " bids");
        priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #fff; -fx-opacity: 0.7");

        Label timeLabel = new Label(
                "Starts: " + (auction.getItem().getStartTime() != null
                        ? auction.getItem().getStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "—")
                + "  ·  Ends: " + (auction.getItem().getEndTime() != null
                        ? auction.getItem().getEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "—"));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #c0c43f; -fx-opacity: 0.7");

        info.getChildren().addAll(row1, priceLabel, timeLabel);

        // Actions
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);
        actions.setPrefWidth(80);

        Button btnView = new Button("View");
        btnView.setMaxWidth(Double.MAX_VALUE);
        btnView.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #c0c43f;"
                        + "-fx-border-radius: 4; -fx-text-fill: #c0c43f; -fx-font-size: 11px;");
        btnView.setOnAction(e -> onViewAuction(auction));

        Button btnDelete = new Button("Delete");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setDisable(auction.getStatus() != AuctionStatus.OPEN);
        btnDelete.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #fff;"
                        + "-fx-border-radius: 4; -fx-text-fill: #fff; -fx-font-size: 11px;");
        btnDelete.setOnAction(e -> onDeleteAuction(auction));

        actions.getChildren().addAll(btnView, btnDelete);

        card.getChildren().addAll(thumb, info, actions);
        return card;
    }


    private void loadMyItems() {
        List<Item> list = AuctionClientService.getInstance().getItemsBySeller(seller.getId());
        myItems = FXCollections.observableArrayList(list);
        renderItems(myItems);
    }

    private void renderItems(List<Item> list) {
        itemListContainer.getChildren().clear();

        if (list == null || list.isEmpty()) {
            itemListContainer.getChildren().add(buildEmptyState(
                    "📦", "No items yet", "Click \"Add Item\" to create your first product."));
            lblItemCount.setText("0 items");
            return;
        }

        list.forEach(item -> itemListContainer.getChildren().add(buildItemCard(item)));
        lblItemCount.setText(list.size() + " item(s)");
    }

    private HBox buildItemCard(Item item) {
        HBox card = new HBox(14);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #722f37, #3d1c21);"
                        + "-fx-border-color: #3d1c21;"
                        + "-fx-border-radius: 8;"
                        + "-fx-background-radius: 8;");

        // Thumb
        VBox thumb = new VBox(3);
        thumb.setAlignment(Pos.CENTER);
        thumb.setPrefSize(56, 56);
        thumb.setStyle("-fx-background-color: #f5e8e8; -fx-background-radius: 8;");
        Label icon = new Label(getCategoryIcon(item.getCategory()));
        icon.setStyle("-fx-font-size: 20px; -fx-text-fill: #722f37;");
        Label cat = new Label(item.getCategory());
        cat.setStyle("-fx-font-size: 9px; -fx-text-fill: #722f37;");
        thumb.getChildren().addAll(icon, cat);

        // Info
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(item.getItemName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        nameLabel.setStyle("-fx-text-fill: #c0c43f;");

        Label descLabel = new Label(item.getDescription());
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #fff; -fx-opacity: 0.7");

        Label priceLabel = new Label("Starting Price: " + formatPrice(item.getStartingPrice()) + " ₫");
        priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #fff;");

        info.getChildren().addAll(nameLabel, descLabel, priceLabel);

        // Actions
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER);
        actions.setPrefWidth(80);

        Button btnEdit = new Button("Edit");
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnEdit.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #c0c43f;"
                        + "-fx-border-radius: 4; -fx-text-fill: #c0c43f; -fx-font-size: 11px;");
        btnEdit.setOnAction(e -> onEditItem(item));

        Button btnDelete = new Button("Delete");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setStyle(
                "-fx-background-color: transparent; -fx-border-color: #fff;"
                        + "-fx-border-radius: 4; -fx-text-fill: #fff; -fx-font-size: 11px;");
        btnDelete.setOnAction(e -> onDeleteItem(item));

        actions.getChildren().addAll(btnEdit, btnDelete);

        card.getChildren().addAll(thumb, info, actions);
        return card;
    }

    // ── Add / Edit / Delete Item ──────────────────────────────────────────────

    @FXML
    private void onAddItem() {
        onNavCreateAuction();
        lblStatusBar.setText("Fill in the form to create a new auction.");
    }

    private void onEditItem(Item item) {
        onNavCreateAuction();
        txtItemName.setText(item.getItemName());
        cmbCategory.setValue(item.getCategory());
        txtDescription.setText(item.getDescription());
        txtStartingPrice.setText(String.valueOf((long) item.getStartingPrice()));
        lblStatusBar.setText("Editing item: " + item.getItemName());
    }

    private void onDeleteItem(Item item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Item");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete \"" + item.getItemName() + "\"? This cannot be undone.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    seller.deleteItem(item);
                    myItems.remove(item);
                    loadMyItems();
                    lblStatusBar.setText("Item deleted: " + item.getItemName());

                } catch (IllegalArgumentException e) {
                    showErrorAlert("Secure error", e.getMessage());
                } catch (IllegalStateException e) {
                    showErrorAlert("Cannot delete", e.getMessage());
                } catch (Exception e) {
                    showErrorAlert("System error", e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void onViewAuction(Auction auction) {
        lblStatusBar.setText("Viewing: " + auction.getItem().getItemName());
        try{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/org/example/loginregister/auction_detail.fxml"));
            Scene scene = new Scene(loader.load());

            AuctionDetailController ctrl = loader.getController();
            ctrl.setData(
                    auction,
                    seller,
                    "seller_dashboard.fxml",
                    "Seller"
            );
            Stage stage = (Stage) auctionListContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Auction detail");
            stage.show();
        }  catch (IOException e){
            e.printStackTrace();
        }
    }

    private void onDeleteAuction(Auction auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel Auction");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Cancel auction for \"" + auction.getItem().getItemName() + "\"?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                AuctionClientService.getInstance().cancelAuction(auction.getId());
                loadMyAuctions();
                lblStatusBar.setText("Auction cancelled.");
            }
        });
    }

    // ── Create Auction (form) ─────────────────────────────────────────────────

    @FXML
    private void onCreateAuction() {
        hideFormError();

        // 1. Validate
        String itemName = txtItemName.getText().trim();
        String category = cmbCategory.getValue();
        String description = txtDescription.getText().trim();
        String startPriceStr = txtStartingPrice.getText().trim().replaceAll("[^0-9]", "");
        LocalDate startDate = dpStartDate.getValue();
        String startTimeStr = txtStartTime.getText().trim();
        LocalDate endDate = dpEndDate.getValue();
        String endTimeStr = txtEndTime.getText().trim();

        if (itemName.isEmpty() || category == null || startPriceStr.isEmpty()
                || startDate == null || startTimeStr.isEmpty()
                || endDate == null || endTimeStr.isEmpty()) {
            showFormError("Please fill in all required fields (*).");
            return;
        }

        double startingPrice;
        try {
            startingPrice = Double.parseDouble(startPriceStr);
        } catch (NumberFormatException e) {
            showFormError("Starting price and increment must be valid numbers.");
            return;
        }

        LocalDateTime startTime;
        LocalDateTime endTime;
        try {
            startTime = LocalDateTime.of(startDate,
                    java.time.LocalTime.parse(startTimeStr, TIME_FORMAT));
            endTime = LocalDateTime.of(endDate,
                    java.time.LocalTime.parse(endTimeStr, TIME_FORMAT));
        } catch (DateTimeParseException e) {
            showFormError("Invalid time format. Please use HH:mm (e.g. 09:00).");
            return;
        }

        if (!endTime.isAfter(startTime)) {
            showFormError("End time must be after start time.");
            return;
        }
        ItemFactory factory;
        switch (category) {
            case "Electronics":
                factory = new ElectronicsFactory();
                break;
            case "Art":
                factory = new ArtFactory();
                break;
            case "Vehicle":
                factory = new VehicleFactory();
                break;
            default:
                throw new IllegalArgumentException("Unsupported category: " + category);
        }

        Item item = factory.createItem(itemName, seller, description, startingPrice, startTime, endTime);
        AuctionClientService.getInstance().createItemAndAuction(item);
        onClearForm();
        loadMyAuctions();
        loadMyItems();
        onNavMyAuctions();
        lblStatusBar.setText("✅ Auction created: " + itemName);

        new Alert(Alert.AlertType.INFORMATION,
                "Auction created successfully for " + itemName)
                .showAndWait();

    }

    @FXML
    private void onClearForm(){
        txtItemName.clear();
        cmbCategory.getSelectionModel().selectFirst();
        txtDescription.clear();
        txtStartingPrice.clear();
        dpStartDate.setValue(null);
        txtStartTime.clear();
        dpEndDate.setValue(null);
        txtEndTime.clear();
        hideFormError();
    }

    private void startAutoRefresh(){
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            loadMyAuctions();
            lblStatusBar.setText("Updated"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }),
        30, 30, TimeUnit.SECONDS);
    }

    private void stopAutoRefresh(){
        if(scheduler != null && !scheduler.isShutdown()){
            scheduler.shutdownNow();
        }
    }

    private void showPane(Node target){
        paneMyAuctions.setVisible(false);
        paneMyItems.setVisible(false);
        paneCreateAuction.setVisible(false);

        paneMyAuctions.setManaged(false);
        paneMyItems.setManaged(false);
        paneCreateAuction.setManaged(false);

        target.setVisible(true);
        target.setManaged(true);
    }

    private void setActiveNav(Button active){
        btnNavMyAuctions.setStyle(STYLE_NAV_NORMAL);
        btnNavMyItems.setStyle(STYLE_NAV_NORMAL);
        btnNavCreateAuction.setStyle(STYLE_NAV_NORMAL);

        active.setStyle(STYLE_NAV_ACTIVE);
    }

    private void showFormError(String message){
        lblFormError.setText(message);
        lblFormError.setVisible(true);
        lblFormError.setManaged(true);
    }

    private void hideFormError(){
        lblFormError.setVisible(false);
        lblFormError.setManaged(false);
    }

    private VBox buildEmptyState(String icon, String title, String hint){
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.setPadding(new Insets(50));
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 38px;");
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        Label hintLabel = new Label(hint);
        hintLabel.setStyle("-fx-text-fill: #c0c43f; -fx-font-size: 12px");
        vBox.getChildren().addAll(iconLabel, titleLabel, hintLabel);
        return vBox;
    }

    private Label buildStatusBadge(AuctionStatus status){
        Label badge = new Label();
        switch (status) {
            case RUNNING:
                badge.setText("● Live");
                badge.setStyle(
                        "-fx-background-color: #e6f4ea; -fx-text-fill: #2d8a4e;"
                                + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 10px;");
                break;
            case OPEN:
                badge.setText("● Upcoming");
                badge.setStyle(
                        "-fx-background-color: #e8f0fe; -fx-text-fill: #1a56db;"
                                + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 10px;");
                break;
            case FINISHED:
                badge.setText("● Finished");
                badge.setStyle(
                        "-fx-background-color: #f0f0f0; -fx-text-fill: #888;"
                                + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 10px;");
                break;
            default:
                badge.setText(status.toString());
                badge.setStyle(
                        "-fx-background-color: #f0f0f0; -fx-text-fill: #888;"
                                + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 10px;");
        }
        return badge;

    }

    private String formatPrice(double price){
        return VND_FORMAT.format((long) price);
    }

    private String getCategoryIcon(String category){
        if(category == null){
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
