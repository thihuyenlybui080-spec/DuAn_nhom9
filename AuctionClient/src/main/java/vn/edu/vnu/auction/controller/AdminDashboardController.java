package vn.edu.vnu.auction.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import vn.edu.vnu.auction.service.AuctionClientService;
import vn.edu.vnu.auction.service.SceneManager;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.user.Admin;
import vn.edu.vnu.auction.model.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static vn.edu.vnu.auction.controller.MainController.LOGIN_FXML;
import static vn.edu.vnu.auction.controller.MainController.LOGIN_TITLE;

public class AdminDashboardController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);
    private static final String STYLE_NAV_ACTIVE =
            "-fx-background-color: #722f37; -fx-font-weight: bold; -fx-text-fill: #c0c43f";
    private static final String STYLE_NAV_NORMAL =
            "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final NumberFormat VND_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML private Label lblUsername;
    @FXML private Button btnNavUsers;
    @FXML private Button btnNavAuctions;

    @FXML private Label lblPageTitle;
    @FXML private Label lblSubtitle;
    @FXML private TextField txtSearch;

    @FXML private VBox paneUsers;
    @FXML private ComboBox<String> cmbRoleFilter;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private Label lblUserCount;
    @FXML private VBox userListContainer;

    @FXML private VBox paneAuctions;
    @FXML private ComboBox<String> cmbAuctionFilter;
    @FXML private Label lblAuctionCount;
    @FXML private VBox auctionListContainer;

    @FXML private Label lblStatusBar;
    @FXML private BorderPane rootBorderPane;

    private Admin admin;
    private ObservableList<Auction> allAuctions;
    private ObservableList<User> allUsers;
    private ScheduledExecutorService scheduler;

    private final SceneManager sceneManager = new SceneManager(getClass());

    public void initialize(URL url, ResourceBundle resourceBundle){
        cmbAuctionFilter.getSelectionModel().selectFirst();
        cmbRoleFilter.getSelectionModel().selectFirst();
        cmbStatusFilter.getSelectionModel().selectFirst();

        Platform.runLater(() -> {
            Stage stage = (Stage) rootBorderPane.getScene().getWindow();
            stage.setFullScreen(true);
        });
    }

    /**
     * Truyền admin đang đăng nhập vào controller,
     * gọi từ LoginController sau khi load FXML
     * @param admin Admin vừa đăng nhập
     */
    public void setCurrentAdmin(Admin admin){
        this.admin = admin;
        lblUsername.setText(admin.getName());
        loadUsers();
        loadAuctions();
        startAutoRefresh();
    }

    @FXML
    private void onNavUsers(){
        setActive(btnNavUsers);
        paneUsers.setVisible(true);
        paneUsers.setManaged(true);

        paneAuctions.setVisible(false);
        paneAuctions.setManaged(false);
        lblPageTitle.setText("Manage Users");
        lblSubtitle.setText("View and manage all registered users");
        txtSearch.clear();
        applyUserFilter();
    }

    @FXML
    private void onNavAuctions(){
        setActive(btnNavAuctions);
        paneUsers.setVisible(false);
        paneUsers.setManaged(false);

        paneAuctions.setVisible(true);
        paneAuctions.setManaged(true);
        lblPageTitle.setText("Manage Auctions");
        lblSubtitle.setText("View and manage all auction sessions");
        txtSearch.clear();
        applyAuctionFilter();
    }

    @FXML
    private void onSignOut(ActionEvent event){
        stopAutoRefresh();
        sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);
    }

    private void loadUsers(){
        List<User> list = AuctionClientService.getInstance().getAllUsers();
        logger.debug("DEBUG loadUsers: {} users", list.size());
        list.forEach(u -> logger.debug("  - {} | {}", u.getFullName(), u.getRole()));
        allUsers = FXCollections.observableArrayList(list);
        applyUserFilter();
    }

    @FXML
    private void onSearch(){
        if(paneUsers.isVisible()){
            applyUserFilter();
        } else{
            applyAuctionFilter();
        }
    }

    private void applyUserFilter(){
        if(allUsers == null){
            return;
        }
        String role = cmbRoleFilter.getValue();
        String status = cmbStatusFilter.getValue();
        String keyword = txtSearch.getText().trim().toLowerCase();

        List<User> result = allUsers.stream()
                .filter(u -> role.equals("All Roles")
                || u.getRole().equalsIgnoreCase(role))
                .filter(u -> {
                    switch (status){
                        case "Active": return u.isActive();
                        case "Locked": return !u.isActive();
                        default: return true;
                    }
                })
                .filter(u -> keyword.isEmpty()
                || u.getFullName().toLowerCase().contains(keyword)
                || u.getEmail().toLowerCase().contains(keyword))
                .collect(Collectors.toList());

        renderUsers(result);
    }

    private void renderUsers(List<User> list){
        userListContainer.getChildren().clear();
        if(list.isEmpty()){
            userListContainer.getChildren().add(
                    buildEmptyState("👤", "No users found", "Try changing the filter."));
            lblUserCount.setText("0 users");
            return;
        }

        list.forEach(u -> userListContainer.getChildren().add(buildUserCard(u)));
        lblUserCount.setText(list.size() + " users");
    }

    private HBox buildUserCard(User user){
        HBox card = new HBox(14);
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #722f37, #3d1c21);"
                + "-fx-border-color: #3d1c21;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;");

        Label avatar = new Label(String.valueOf(user.getFullName().charAt(0)).toUpperCase());
        avatar.setPrefSize(42, 42);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: " + getRoleColor(user.getRole()) + ";"
                + "-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;"
                + "-fx-background-radius: 50%;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        Label roleBadge = buildRoleBadge(user.getRole());
        Label statusLabel = buildUserStatusBadge(user.isActive());
        row1.getChildren().addAll(nameLabel, roleBadge, statusLabel);

        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #c0c43f; -fx-opacity: 0.7");

        Label idLabel = new Label(user.getId());
        idLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #c0c43f; -fx-opacity: 0.7");

        info.getChildren().addAll(row1, emailLabel, idLabel);

        Button btnToggle = buildLockButton(user);
        card.getChildren().addAll(avatar, info, btnToggle);
        return card;

    }

    private Button buildLockButton(User user){
        boolean isSelf = user.getId().equals(admin.getId());
        Button btnLockOrUnlock = new Button(user.isActive() ? "🔒 Lock" : "🔓 Unlock" );
        btnLockOrUnlock.setDisable(isSelf);

        btnLockOrUnlock.setStyle(user.isActive()
                ? "-fx-background-color: transparent; -fx-text-fill: #c0c43f;"
                + "-fx-border-color: #c0c43f; -fx-border-radius: 4;"
                + "-fx-font-size: 11px; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-text-fill: #fff;"
                + "-fx-border-color: #fff; -fx-border-radius: 4;"
                + "-fx-font-size: 11px; -fx-cursor: hand;");

        btnLockOrUnlock.setPadding(new Insets(6));
        btnLockOrUnlock.setOnAction(e -> onToggleLock(user, btnLockOrUnlock));
        return btnLockOrUnlock;
    }

    private void onToggleLock(User user, Button btn){
        String action = user.isActive() ? "lock" : "unlock";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("confirm");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to " + action + " user \"" + user.getFullName() + "\"?");

        confirm.showAndWait().ifPresent(respone -> {
            if(respone != ButtonType.OK) {
                return;
            }
            boolean wasActive = user.isActive();
            try {
                AuctionClientService.getInstance().toggleUserLock(user);
                if (!wasActive) {
                    btn.setText("🔒 Lock");
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #c0c43f;"
                            + "-fx-border-color: #c0c43f; -fx-border-radius: 4;"
                            + "-fx-font-size: 11px; -fx-cursor: hand;");
                    lblStatusBar.setText("Unlocked: " + user.getFullName());
                } else {
                    btn.setText("🔓 Unlock");
                    btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #fff;"
                            + "-fx-border-color: #fff; -fx-border-radius: 4;"
                            + "-fx-font-size: 11px; -fx-cursor: hand;");
                    lblStatusBar.setText("Locked: " + user.getFullName());
                }
                applyUserFilter();
            }catch (RuntimeException e) {
                new Alert(Alert.AlertType.ERROR, "Action failed: " + e.getMessage())
                        .showAndWait();
            }
        });
    }

    private void loadAuctions(){
        List<Auction> list = AuctionClientService.getInstance().getAllAuctions();
        allAuctions = FXCollections.observableArrayList(list);
        applyAuctionFilter();
    }

    @FXML
    private void onRoleFilterChanged() {
        applyUserFilter();
    }

    @FXML
    private void onStatusFilterChanged() {
        applyUserFilter();
    }

    @FXML
    private void onAuctionFilterChanged() {
        applyAuctionFilter();
    }

    private void applyAuctionFilter(){
        if(allAuctions == null){
            return;
        }
        String status = cmbAuctionFilter.getValue();
        String keyword = txtSearch.getText().trim().toLowerCase();

        List<Auction> result = allAuctions.stream()
                .filter(a -> {
                    switch (status) {
                        case"Live": return a.getStatus() == AuctionStatus.RUNNING;
                        case "Upcoming": return a.getStatus() == AuctionStatus.OPEN;
                        case "Finished": return a.getStatus() == AuctionStatus.FINISHED;
                        default: return true;
                    }
                })
                .filter(a -> keyword.isEmpty()
                || a.getItem().getItemName().toLowerCase().contains(keyword)
                || a.getSeller().getFullName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        renderAuctions(result);
    }

    private void renderAuctions(List<Auction> list){
        auctionListContainer.getChildren().clear();

        if(list.isEmpty()){
            auctionListContainer.getChildren().add(
                    buildEmptyState("📭", "No auctions found", "Try changing the filter."));
            lblAuctionCount.setText("0 auctions");
            return;
        }

        list.forEach(a -> auctionListContainer.getChildren().add(buildAuctionCard(a)));
        lblAuctionCount.setText(list.size() +" auctions");
    }

    private HBox buildAuctionCard(Auction auction){
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
        icon.setStyle("-fx-font-size: 22px; -fx-background-color: #722f37");
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
        Label badge = buildAuctionStatusBadge(auction.getStatus());
        Label idLabel = new Label("ID: " + auction.getId());
        idLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #c0c43f; -fx-opacity: 0.7;");
        row1.getChildren().addAll(nameLabel, badge, idLabel);

        Label priceLabel = new Label(
                "Current: " + formatPrice(auction.getCurrentPrice()) + " ₫"
                        + "  ·  " + auction.getBids().size() + " bids");
        priceLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

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
        actions.setPrefWidth(90);
        if(auction.getStatus() == AuctionStatus.RUNNING
        || auction.getStatus() == AuctionStatus.OPEN){
            Button btnForce = new Button(auction.getStatus() == AuctionStatus.RUNNING ? "Force End" : "Cancel");
            btnForce.setMaxWidth(Double.MAX_VALUE);
            btnForce.setStyle(
                    "-fx-background-color: transparent; -fx-border-color: #c0c43f;"
                            + "-fx-border-radius: 4; -fx-text-fill: #c0c43f; -fx-font-size: 11px;");
            btnForce.setPadding(new Insets(6, 8, 6, 8));
            btnForce.setOnAction(e -> onForceAuction(auction, btnForce));
            actions.getChildren().add(btnForce);
        }

        card.getChildren().addAll(thumb, info, actions);
        return card;
    }

    private void onForceAuction(Auction auction, Button button){
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(button.getText().equals("Force End") ? "Force End Auction" : "Cancel Auction");
        confirm.setHeaderText(null);
        confirm.setContentText(button.getText().equals("Force End") ?
                "Force end auction \"" + auction.getItem().getItemName() + "\"?\n"
                        + "This will immediately close the session."
                : "Are you sure you want to cancel auction ? This will be delete forever");
        confirm.showAndWait().ifPresent(respone -> {
            if(respone != ButtonType.OK){
                return;
            }
            if(button.getText().equals("Force End")){
                AuctionClientService.getInstance().forceEndAuction(auction.getId());
                loadAuctions();
                lblStatusBar.setText("Force End: " + auction.getItem().getItemName());
            }
            else{
                AuctionClientService.getInstance().cancelAuction(auction.getId());
                loadAuctions();
                lblStatusBar.setText("Cancel: " + auction.getItem().getItemName());
            }
            loadAuctions();
        });
    }

    private void startAutoRefresh(){
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                () -> Platform.runLater(() -> {
                    loadUsers();
                    loadAuctions();
                    lblStatusBar.setText("Updated "
                            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                }),
                30, 30, TimeUnit.SECONDS);
    }

    private void stopAutoRefresh(){
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    private void setActive(Button active){
        btnNavUsers.setStyle(STYLE_NAV_NORMAL);
        btnNavAuctions.setStyle(STYLE_NAV_NORMAL);
        active.setStyle(STYLE_NAV_ACTIVE);
    }

    private VBox buildEmptyState(String icon, String title, String hint) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50));
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 38px;");
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        Label hintLabel = new Label(hint);
        hintLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
        box.getChildren().addAll(iconLabel, titleLabel, hintLabel);
        return box;
    }

    private Label buildRoleBadge(String role) {
        Label badge = new Label(role);
        String style = "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 10px;";
        switch (role) {
            case "Bidder":
                badge.setStyle(style + "-fx-background-color: #e8f0fe; -fx-text-fill: #1a56db;");
                break;
            case "Seller":
                badge.setStyle(style + "-fx-background-color: #fef3e2; -fx-text-fill: #b45309;");
                break;
            case "Admin":
                badge.setStyle(style + "-fx-background-color: #f5e8e8; -fx-text-fill: #722f37;");
                break;
            default:
                badge.setStyle(style + "-fx-background-color: #f0f0f0; -fx-text-fill: #888;");
        }
        return badge;
    }
    private Label buildUserStatusBadge(boolean isActive) {
        Label badge = new Label(isActive ? "✓ Active" : "🔒 Locked");
        badge.setStyle(isActive
                ? "-fx-background-color: #e6f4ea; -fx-text-fill: #2d8a4e;"
                        + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 10px;"
                : "-fx-background-color: #c0c43f; -fx-text-fill: #e53935;"
                + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 10px;");
        return badge;
    }

    private Label buildAuctionStatusBadge(AuctionStatus status) {
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

    private String formatPrice(double price) {
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

    private String getRoleColor(String role) {
        switch (role) {
            case "Bidder": return "#1a56db";
            case "Seller": return "#b45309";
            case "Admin":  return "#722f37";
            default:     return "#888";
        }
    }




}
