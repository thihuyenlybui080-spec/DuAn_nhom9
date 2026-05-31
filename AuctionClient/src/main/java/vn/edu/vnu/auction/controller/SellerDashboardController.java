package vn.edu.vnu.auction.controller;

import static vn.edu.vnu.auction.controller.MainController.LOGIN_FXML;
import static vn.edu.vnu.auction.controller.MainController.LOGIN_TITLE;

import java.io.File;
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
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionStatus;
import vn.edu.vnu.auction.model.entity.item.Item;
import vn.edu.vnu.auction.model.entity.user.Seller;
import vn.edu.vnu.auction.model.factory.ArtFactory;
import vn.edu.vnu.auction.model.factory.ElectronicsFactory;
import vn.edu.vnu.auction.model.factory.ItemFactory;
import vn.edu.vnu.auction.model.factory.OtherFactory;
import vn.edu.vnu.auction.model.factory.VehicleFactory;
import vn.edu.vnu.auction.service.AuctionClientService;
import vn.edu.vnu.auction.service.SceneManager;

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
  private Button btnProductImage;

  @FXML
  private Label lblStatusBar;
  @FXML
  private ImageView imvProduct;

  private Seller seller;
  private ObservableList<Auction> myAuctions;
  private ObservableList<Item> myItems;
  private ScheduledExecutorService scheduler;
  private String selectedImagePath;

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
   * Truyền user đang đăng nhập vào controller. Gọi từ {@link LoginController} sau khi load FXML.
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
      auctionListContainer.getChildren().add(UIFactory.buildEmptyState(
          "📭", "No auctions yet", "Go to \"Create Auction\" to list your first item."));
      lblAuctionCount.setText("0 auctions");
      return;
    }

    list.forEach(a -> auctionListContainer.getChildren().add(buildAuctionCard(a)));
    lblAuctionCount.setText(list.size() + " auction(s)");
  }

  private HBox buildAuctionCard(Auction auction) {
    HBox card = UIFactory.buildAuctionCard(auction);
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

    card.getChildren().add(actions);
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
      itemListContainer.getChildren().add(UIFactory.buildEmptyState(
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
    Label icon = new Label(UIFactory.getCategoryIcon(item.getCategory()));
    icon.setStyle("-fx-font-size: 20px; -fx-text-fill: #722f37;");
    Label cat = new Label(item.getCategory());
    cat.setStyle("-fx-font-size: 9px; -fx-text-fill: #722f37;");
    thumb.getChildren().addAll(icon, cat);

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

    Auction relatedAuction = myAuctions.stream()
        .filter(a -> a.getItem().getId() == item.getId())
        .findFirst()
        .orElse(null);
    if (relatedAuction != null && relatedAuction.getStatus() == AuctionStatus.RUNNING) {
      btnDelete.setDisable(true);
      btnDelete.setStyle(
          "-fx-background-color: transparent; -fx-border-color: #555;"
              + "-fx-border-radius: 4; -fx-text-fill: #555; -fx-font-size: 11px;");
    }

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
    Auction relatedAuction = myAuctions.stream()
        .filter(a -> a.getItem().getId() == item.getId())
        .findFirst()
        .orElse(null);

    if (relatedAuction != null) {
      if (relatedAuction.getStatus() == AuctionStatus.RUNNING) {
        showErrorAlert("Cannot Delete", "Cannot delete item \"" + item.getItemName()
            + "\" because the auction is currently live.");
        return;
      }
      if (!relatedAuction.getBids().isEmpty()) {
        showErrorAlert("Cannot Delete", "Cannot delete item \"" + item.getItemName()
            + "\" because the related auction has bids placed on it.");
        return;
      }
    }

    String message = "Delete \"" + item.getItemName() + "\"? This cannot be undone.";
    if (relatedAuction != null) {
      message += " This will also cancel the related auction.";
    }

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Delete Item");
    confirm.setHeaderText(null);
    confirm.setContentText(message);
    confirm.showAndWait().ifPresent(btn -> {
      if (btn == ButtonType.OK) {
        try {
          if (relatedAuction != null) {
            AuctionClientService.getInstance().cancelAuction(relatedAuction.getId());
            myAuctions.removeIf(a -> a.getId() == relatedAuction.getId());
          }
          AuctionClientService.getInstance().deleteItem(item.getId());
          myItems.remove(item);
          loadMyItems();
          loadMyAuctions();

          Stage stage = (Stage) rootBorderPane.getScene().getWindow();
          if (relatedAuction != null) {
            ToastNotification.show(stage, "Success",
                "Item deleted and auction cancelled successfully!", ToastNotification.Type.SUCCESS);
            lblStatusBar.setText("Item deleted and auction cancelled: " + item.getItemName());
          } else {
            ToastNotification.show(stage, "Success", "Item deleted successfully!",
                ToastNotification.Type.SUCCESS);
            lblStatusBar.setText("Item deleted: " + item.getItemName());
          }
          new java.util.Timer().schedule(new TimerTask() {
            @Override
            public void run() {
              Platform.runLater(() -> {
                lblStatusBar.setText("");
              });
            }
          }, 3000);

        } catch (IllegalArgumentException e) {
          Stage stage = (Stage) rootBorderPane.getScene().getWindow();
          ToastNotification.show(stage, "Error", e.getMessage(), ToastNotification.Type.ERROR);
          showErrorAlert("Secure error", e.getMessage());
        } catch (IllegalStateException e) {
          showErrorAlert("Cannot delete", e.getMessage());
        } catch (Exception e) {
          showErrorAlert("System error", e.getMessage());
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
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/vn/edu/vnu/auctionclient/auction_detail.fxml"));
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
    } catch (IOException e) {
      showErrorAlert("Load Error", "Failed to load auction detail: " + e.getMessage());
    } catch (Exception e) {
      showErrorAlert("Error", "An error occurred: " + e.getMessage());
    }
  }

  private void onDeleteAuction(Auction auction) {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Cancel Auction");
    confirm.setHeaderText(null);
    confirm.setContentText(
        "Cancel auction for \"" + auction.getItem().getItemName()
            + "\"? This will also delete the related item.");
    confirm.showAndWait().ifPresent(btn -> {
      if (btn == ButtonType.OK) {
        try {
          AuctionClientService.getInstance().cancelAuction(auction.getId());
          Item relatedItem = auction.getItem();
          if (relatedItem != null) {
            try {
              AuctionClientService.getInstance().deleteItem(relatedItem.getId());
              myItems.removeIf(item -> item.getId() == relatedItem.getId());
            } catch (Exception e) {
              System.err.println("Failed to delete related item: " + e.getMessage());
            }
          }
          loadMyAuctions();
          loadMyItems();
          lblStatusBar.setText("Auction cancelled and item deleted.");

          Stage stage = (Stage) rootBorderPane.getScene().getWindow();
          ToastNotification.show(stage, "Success",
              "Auction cancelled and item deleted successfully!", ToastNotification.Type.SUCCESS);

          new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
              Platform.runLater(() -> {
                lblStatusBar.setText("");
              });
            }
          }, 3000);
        } catch (Exception e) {
          Stage stage = (Stage) rootBorderPane.getScene().getWindow();
          ToastNotification.show(stage, "Error", "Failed to cancel auction: " + e.getMessage(),
              ToastNotification.Type.ERROR);
        }
      }
    });
  }

  // ── Create Auction (form) ─────────────────────────────────────────────────

  @FXML
  private void onCreateAuction() {
    hideFormError();
    if (!seller.isActive()) {
      showFormError(
          "Your account has been locked and cannot create auctions. Please contact the administrator.");
      return;
    }

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
    ItemFactory factory = switch (category) {
      case "Electronics" -> new ElectronicsFactory();
      case "Art" -> new ArtFactory();
      case "Vehicle" -> new VehicleFactory();
      case "Other" -> new OtherFactory();
      default -> throw new IllegalArgumentException("Unsupported category: " + category);
    };

    Item item = factory.createItem(itemName, seller.getId(), description, startingPrice, startTime,
        endTime);
    item.setImagePath(selectedImagePath);
    AuctionClientService.getInstance().createItemAndAuction(item);
    onClearForm();
    loadMyAuctions();
    loadMyItems();
    lblStatusBar.setText("✅ Auction created: " + itemName);
    showFormError("✅ Auction created successfully for " + itemName);

    Stage stage = (Stage) rootBorderPane.getScene().getWindow();
    ToastNotification.show(stage, "Success", "Item and Auction created successfully!",
        ToastNotification.Type.SUCCESS);
    lblFormError.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px;");

  }

  @FXML
  private void onClearForm() {
    txtItemName.clear();
    cmbCategory.getSelectionModel().selectFirst();
    txtDescription.clear();
    txtStartingPrice.clear();
    dpStartDate.setValue(null);
    txtStartTime.clear();
    dpEndDate.setValue(null);
    txtEndTime.clear();
    selectedImagePath = null;
    imvProduct.setImage(null);
    hideFormError();
  }

  @FXML
  private void onChooseProductImage() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select Product Image");
    fileChooser.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
    );

    Stage stage = (Stage) rootBorderPane.getScene().getWindow();
    File selectedFile = fileChooser.showOpenDialog(stage);

    if (selectedFile != null) {
      selectedImagePath = selectedFile.getAbsolutePath();
      try {
        Image image = new Image(selectedFile.toURI().toString());
        imvProduct.setImage(image);
        imvProduct.setPreserveRatio(true);
        imvProduct.setFitHeight(150);
      } catch (Exception e) {
        System.err.println("Error loading image: " + e.getMessage());
      }
    }
  }

  private void startAutoRefresh() {
    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(() -> Platform.runLater(() -> {
          loadMyAuctions();
          lblStatusBar.setText("Updated"
              + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }),
        30, 30, TimeUnit.SECONDS);
  }

  private void stopAutoRefresh() {
    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdownNow();
    }
  }

  private void showPane(Node target) {
    paneMyAuctions.setVisible(false);
    paneMyItems.setVisible(false);
    paneCreateAuction.setVisible(false);

    paneMyAuctions.setManaged(false);
    paneMyItems.setManaged(false);
    paneCreateAuction.setManaged(false);

    target.setVisible(true);
    target.setManaged(true);
  }

  private void setActiveNav(Button active) {
    btnNavMyAuctions.setStyle(STYLE_NAV_NORMAL);
    btnNavMyItems.setStyle(STYLE_NAV_NORMAL);
    btnNavCreateAuction.setStyle(STYLE_NAV_NORMAL);

    active.setStyle(STYLE_NAV_ACTIVE);
  }

  private void showFormError(String message) {
    lblFormError.setText(message);
    lblFormError.setVisible(true);
    lblFormError.setManaged(true);

    // Auto-hide after 3 seconds
    new java.util.Timer().schedule(new java.util.TimerTask() {
      @Override
      public void run() {
        Platform.runLater(() -> {
          lblFormError.setVisible(false);
          lblFormError.setManaged(false);
        });
      }
    }, 3000);
  }

  private void hideFormError() {
    lblFormError.setVisible(false);
    lblFormError.setManaged(false);
  }

  private String formatPrice(double price) {
    return VND_FORMAT.format((long) price);
  }
}
