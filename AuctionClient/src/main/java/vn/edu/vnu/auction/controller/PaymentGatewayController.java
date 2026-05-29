package vn.edu.vnu.auction.controller;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import vn.edu.vnu.auction.model.entity.AuctionResult;
import vn.edu.vnu.auction.service.AuctionClientService;

public class PaymentGatewayController {

    private static final String COLOR_PRIMARY = "#722f37";
    private static final String COLOR_ACCENT  = "#c0c43f";
    private static final String COLOR_DARK    = "#1a0d0f";
    private static final String COLOR_CARD    = "#3d1c21";
    private static final String COLOR_TEXT    = "#ffffff";
    private static String selectedMethod = null;

    public static void Show(AuctionResult result, Runnable onSuccess) {
        selectedMethod = null;

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);

        VBox root = buildRoot(result, stage, onSuccess);
        Scene scene = new Scene(root, 420, 580);
        stage.setScene(scene);
        stage.show();

        root.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(300), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    // ─── Root ───────────────────────────────────────────────────────────────────
    private static VBox buildRoot(AuctionResult result, Stage stage, Runnable onSuccess) {
        VBox root = new VBox(0);
        root.setStyle(
                "-fx-background-color: " + COLOR_DARK + ";" +
                        "-fx-border-color: " + COLOR_ACCENT + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;"
        );

        VBox methodContent = new VBox();
        methodContent.setPadding(new Insets(0, 20, 0, 20));

        VBox scrollContent = new VBox(0);
        scrollContent.getChildren().addAll(
                buildOrderInfo(result),
                buildMethodSelector(methodContent, result),
                methodContent,
                buildPayButton(result, stage, onSuccess)
        );

        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background: " + COLOR_DARK + ";" +
                        "-fx-background-color: " + COLOR_DARK + ";" +
                        "-fx-border-color: transparent;"
        );

        root.getChildren().addAll(buildHeader(stage), scrollPane);
        return root;
    }

    // ─── Header ─────────────────────────────────────────────────────────────────
    private static HBox buildHeader(Stage stage) {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, " + COLOR_PRIMARY + ", " + COLOR_CARD + ");" +
                        "-fx-background-radius: 10 10 0 0;"
        );

        Label icon = new Label("🏦");
        icon.setStyle("-fx-font-size: 22px;");

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        titleBox.setPadding(new Insets(0, 0, 0, 10));

        Label title = new Label("Payment Gateway");
        title.setFont(Font.font("System", FontWeight.BOLD, 15));
        title.setStyle("-fx-text-fill: " + COLOR_ACCENT + ";");

        Label subtitle = new Label("Secure Payment");
        subtitle.setStyle("-fx-text-fill: #ffffff; -fx-opacity: 0.6; -fx-font-size: 11px;");
        titleBox.getChildren().addAll(title, subtitle);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-cursor: hand; -fx-opacity: 0.7;");
        btnClose.setOnAction(e -> stage.close());
        btnClose.setOnMouseEntered(e -> btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #e53935; -fx-font-size: 14px; -fx-cursor: hand;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 14px; -fx-cursor: hand; -fx-opacity: 0.7;"));

        header.getChildren().addAll(icon, titleBox, btnClose);
        return header;
    }
    private static VBox buildOrderInfo(AuctionResult result) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(20, 20, 10, 20));

        Label lblSection = new Label("ORDER SUMMARY");
        lblSection.setStyle("-fx-text-fill: " + COLOR_ACCENT + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-opacity: 0.8;");

        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: " + COLOR_CARD + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: rgba(192,196,63,0.3);" +
                        "-fx-border-radius: 8; -fx-border-width: 1;"
        );

        String itemName = result.getItem() != null ? result.getItem().getItemName() : "Unknown Item";
        card.getChildren().addAll(
                buildInfoRow("🛍 Item", itemName),
                buildDivider(),
                buildInfoRow("🔖 Order ID", "AUCTION-" + result.getAuctionId()),
                buildDivider(),
                buildAmountRow(result.getFinalPrice())
        );

        box.getChildren().addAll(lblSection, card);
        return box;
    }

    // ─── Method Selector ────────────────────────────────────────────────────────
    private static VBox buildMethodSelector(VBox methodContent, AuctionResult result) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(16, 20, 10, 20));

        Label lblSection = new Label("PAYMENT METHOD");
        lblSection.setStyle("-fx-text-fill: " + COLOR_ACCENT + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-opacity: 0.8;");

        ToggleGroup group = new ToggleGroup();

        // All false — no method selected by default
        ToggleButton btnATM  = buildMethodBtn("🏧\nATM Card", group, false);
        ToggleButton btnQR   = buildMethodBtn("📱\nQR Code",  group, false);
        ToggleButton btnVisa = buildMethodBtn("💳\nVisa/MC",  group, false);

        btnATM.setOnAction(e -> {
            selectedMethod = "ATM";
            showATMForm(methodContent);
        });
        btnQR.setOnAction(e -> {
            selectedMethod = "QR";
            showQRForm(methodContent, result);
        });
        btnVisa.setOnAction(e -> {
            selectedMethod = "VISA";
            showVisaForm(methodContent);
        });

        HBox methods = new HBox(10);
        methods.getChildren().addAll(btnATM, btnQR, btnVisa);
        box.getChildren().addAll(lblSection, methods);
        return box;
    }

    // ─── ATM Form ───────────────────────────────────────────────────────────────
    private static void showATMForm(VBox container) {
        container.getChildren().clear();
        container.setSpacing(10);

        Label lbl = new Label("ATM Card Information");
        lbl.setStyle("-fx-text-fill: " + COLOR_ACCENT + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        TextField txtCardNumber = buildField("Card Number (16 digits)");
        TextField txtName       = buildField("Cardholder Name");

        ComboBox<String> cmbBank = new ComboBox<>();
        cmbBank.getItems().addAll("Vietcombank", "BIDV", "Techcombank", "Agribank", "MB Bank", "VPBank");
        cmbBank.setPromptText("Select Bank");
        cmbBank.setPrefWidth(Double.MAX_VALUE);
        cmbBank.setStyle(
                "-fx-background-color: " + COLOR_CARD + ";" +
                        "-fx-text-fill: #fff;" +
                        "-fx-border-color: rgba(192,196,63,0.4);" +
                        "-fx-border-radius: 6; -fx-background-radius: 6;"
        );

        container.getChildren().addAll(lbl, txtCardNumber, txtName, cmbBank);
    }

    // ─── QR Form ────────────────────────────────────────────────────────────────
    private static void showQRForm(VBox container, AuctionResult result) {
        container.getChildren().clear();
        container.setSpacing(8);
        container.setAlignment(Pos.CENTER);

        Label lbl = new Label("Scan QR Code to Pay");
        lbl.setStyle("-fx-text-fill: " + COLOR_ACCENT + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Load QR image from resources
        java.io.InputStream qrStream = PaymentGatewayController.class.getResourceAsStream("/vn/edu/vnu/auctionclient/qr.png");
        if (qrStream == null) {
            qrStream = PaymentGatewayController.class.getResourceAsStream("qr.png");
        }

        if (qrStream != null) {
            Image qrImage = new Image(qrStream);
            ImageView qrView = new ImageView(qrImage);
            qrView.setFitWidth(180);
            qrView.setFitHeight(180);
            qrView.setPreserveRatio(true);

            VBox qrWrapper = new VBox(qrView);
            qrWrapper.setAlignment(Pos.CENTER);
            qrWrapper.setPadding(new Insets(12));
            qrWrapper.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8;");
            qrWrapper.setMaxWidth(Region.USE_PREF_SIZE);

            Label lblAmount = new Label(formatPrice(result.getFinalPrice()) + " ₫");
            lblAmount.setFont(Font.font("System", FontWeight.BOLD, 16));
            lblAmount.setStyle("-fx-text-fill: " + COLOR_ACCENT + ";");

            Label lblHint = new Label("Open your banking app → Scan QR → Confirm");
            lblHint.setStyle("-fx-text-fill: #ffffff; -fx-opacity: 0.6; -fx-font-size: 10px;");

            container.getChildren().addAll(lbl, qrWrapper, lblAmount, lblHint);
        } else {
            System.err.println("❌ qr.png not found in resources! Place it at src/main/resources/qr.png");
            Label lblErr = new Label("⚠️ QR image not found.\nPlace qr.png in src/main/resources/");
            lblErr.setStyle("-fx-text-fill: #facc15; -fx-font-size: 11px; -fx-text-alignment: center;");
            lblErr.setWrapText(true);

            Label lblAmount = new Label(formatPrice(result.getFinalPrice()) + " ₫");
            lblAmount.setFont(Font.font("System", FontWeight.BOLD, 16));
            lblAmount.setStyle("-fx-text-fill: " + COLOR_ACCENT + ";");

            Label lblHint = new Label("Open your banking app → Scan QR → Confirm");
            lblHint.setStyle("-fx-text-fill: #ffffff; -fx-opacity: 0.6; -fx-font-size: 10px;");

            container.getChildren().addAll(lbl, lblErr, lblAmount, lblHint);
        }
    }

    // ─── Visa Form ──────────────────────────────────────────────────────────────
    private static void showVisaForm(VBox container) {
        container.getChildren().clear();
        container.setSpacing(10);
        container.setAlignment(Pos.CENTER);

        Label lbl = new Label("💳  You will be redirected to Stripe\nto complete your payment securely.");
        lbl.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-text-alignment: center; -fx-opacity: 0.8;");
        lbl.setWrapText(true);

        Label lblNote = new Label("🔒  Secured by Stripe");
        lblNote.setStyle("-fx-text-fill: " + COLOR_ACCENT + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        container.getChildren().addAll(lbl, lblNote);
    }

    // ─── Pay Button ─────────────────────────────────────────────────────────────
    private static VBox buildPayButton(AuctionResult result, Stage stage, Runnable onSuccess) {
        VBox box = new VBox(12);
        box.setPadding(new Insets(14, 20, 24, 20));
        box.setAlignment(Pos.CENTER);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(380);
        progressBar.setPrefHeight(6);
        progressBar.setStyle("-fx-accent: " + COLOR_ACCENT + "; -fx-background-color: " + COLOR_CARD + "; -fx-background-radius: 3; -fx-border-radius: 3;");
        progressBar.setVisible(false);

        Label lblStatus = new Label("");
        lblStatus.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-opacity: 0.8;");
        lblStatus.setVisible(false);

        Button btnPay = new Button("  Confirm Payment  💳");
        btnPay.setPrefWidth(380);
        btnPay.setPrefHeight(48);
        btnPay.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnPay.setStyle("-fx-background-color: " + COLOR_ACCENT + "; -fx-text-fill: " + COLOR_PRIMARY + "; -fx-background-radius: 8; -fx-cursor: hand;");

        btnPay.setOnAction(e -> {
            // Check no method selected
            if (selectedMethod == null) {
                lblStatus.setVisible(true);
                lblStatus.setText("⚠️ Please select a payment method!");
                lblStatus.setStyle("-fx-text-fill: #facc15; -fx-font-size: 12px; -fx-font-weight: bold;");
                return;
            }

            btnPay.setDisable(true);
            progressBar.setVisible(true);
            lblStatus.setVisible(true);
            lblStatus.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-opacity: 0.8;");

            Timeline timeline = new Timeline();

            if ("VISA".equals(selectedMethod)) {
                KeyFrame s1 = new KeyFrame(Duration.millis(400), ev -> {
                    progressBar.setProgress(0.3);
                    lblStatus.setText("Connecting to Stripe...");
                    // ✅ Mở Stripe ở đây — đúng lúc bấm Confirm
                    try {
                        String url = AuctionClientService.getInstance().createPaymentLink(result.getAuctionId());
                        if (url != null) java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        lblStatus.setText("❌ Failed to open payment page: " + ex.getMessage());
                        lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
                        btnPay.setDisable(false);
                        progressBar.setVisible(false);
                        return;
                    }
                });
                KeyFrame s2 = new KeyFrame(Duration.millis(1200), ev -> {
                    progressBar.setProgress(0.7);
                    lblStatus.setText("Waiting for payment on Stripe...");
                });
                KeyFrame s3 = new KeyFrame(Duration.millis(2200), ev -> {
                    progressBar.setProgress(0.95);
                    lblStatus.setText("Finalizing...");
                });
                KeyFrame s4 = new KeyFrame(Duration.millis(2800), ev ->
                        confirmPayment(result, stage, onSuccess, btnPay, progressBar, lblStatus));
                timeline.getKeyFrames().addAll(s1, s2, s3, s4);

            } else if ("ATM".equals(selectedMethod)) {
                KeyFrame s1 = new KeyFrame(Duration.millis(500),  ev -> { progressBar.setProgress(0.25); lblStatus.setText("Connecting to bank..."); });
                KeyFrame s2 = new KeyFrame(Duration.millis(1200), ev -> { progressBar.setProgress(0.5);  lblStatus.setText("Verifying card information..."); });
                KeyFrame s3 = new KeyFrame(Duration.millis(2000), ev -> { progressBar.setProgress(0.75); lblStatus.setText("Sending OTP to your phone..."); });
                KeyFrame s4 = new KeyFrame(Duration.millis(3000), ev -> { progressBar.setProgress(0.95); lblStatus.setText("Finalizing..."); });
                KeyFrame s5 = new KeyFrame(Duration.millis(3600), ev -> confirmPayment(result, stage, onSuccess, btnPay, progressBar, lblStatus));
                timeline.getKeyFrames().addAll(s1, s2, s3, s4, s5);

            } else { // QR
                KeyFrame s1 = new KeyFrame(Duration.millis(500),  ev -> { progressBar.setProgress(0.3);  lblStatus.setText("Waiting for QR scan..."); });
                KeyFrame s2 = new KeyFrame(Duration.millis(1500), ev -> { progressBar.setProgress(0.6);  lblStatus.setText("Transaction detected..."); });
                KeyFrame s3 = new KeyFrame(Duration.millis(2500), ev -> { progressBar.setProgress(0.9);  lblStatus.setText("Processing payment..."); });
                KeyFrame s4 = new KeyFrame(Duration.millis(3200), ev -> { progressBar.setProgress(0.95); lblStatus.setText("Finalizing..."); });
                KeyFrame s5 = new KeyFrame(Duration.millis(3800), ev -> confirmPayment(result, stage, onSuccess, btnPay, progressBar, lblStatus));
                timeline.getKeyFrames().addAll(s1, s2, s3, s4, s5);
            }

            timeline.play();
        });

        box.getChildren().addAll(progressBar, lblStatus, btnPay);
        return box;
    }

    // ─── Confirm Payment ────────────────────────────────────────────────────────
    private static void confirmPayment(AuctionResult result, Stage stage, Runnable onSuccess,
                                       Button btnPay, ProgressBar bar, Label lbl) {
        try {
            AuctionClientService.getInstance().payAuction(result.getAuctionId());
            bar.setProgress(1.0);
            lbl.setText("✅ Payment successful!");
            lbl.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px; -fx-font-weight: bold;");
            
            ToastNotification.show(stage, "Success", "Payment successful! Your order is confirmed.", ToastNotification.Type.SUCCESS);
            
            stage.close();
            if (onSuccess != null) onSuccess.run();
        } catch (Exception ex) {
            ex.printStackTrace();
            bar.setVisible(false);
            lbl.setVisible(true);
            lbl.setText("❌ Payment failed: " + ex.getMessage());
            lbl.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px; -fx-font-weight: bold;");
            
            ToastNotification.show(stage, "Error", "Payment failed: " + ex.getMessage(), ToastNotification.Type.ERROR);
            
            btnPay.setDisable(false);
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────
    private static TextField buildField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(Double.MAX_VALUE);
        tf.setPrefHeight(38);
        tf.setMinWidth(40);
        tf.setMinHeight(38);
        tf.setStyle(
                "-fx-background-color: " + COLOR_CARD + ";" +
                        "-fx-text-fill: #fff;" +
                        "-fx-prompt-text-fill: rgba(255,255,255,0.4);" +
                        "-fx-border-color: rgba(192,196,63,0.4);" +
                        "-fx-border-radius: 6; -fx-background-radius: 6;" +
                        "-fx-padding: 8;"
        );
        return tf;
    }

    private static HBox buildInfoRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #ffffff; -fx-opacity: 0.6; -fx-font-size: 12px;");
        lbl.setPrefWidth(110);
        Label val = new Label(value);
        val.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-font-weight: bold;");
        val.setWrapText(true);
        HBox.setHgrow(val, Priority.ALWAYS);
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private static HBox buildAmountRow(double price) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("💰  Amount");
        lbl.setStyle("-fx-text-fill: #ffffff; -fx-opacity: 0.6; -fx-font-size: 12px;");
        lbl.setPrefWidth(110);
        Label val = new Label(formatPrice(price) + " ₫");
        val.setFont(Font.font("System", FontWeight.BOLD, 18));
        val.setStyle("-fx-text-fill: " + COLOR_ACCENT + ";");
        row.getChildren().addAll(lbl, val);
        return row;
    }

    private static Region buildDivider() {
        Region div = new Region();
        div.setPrefHeight(1);
        div.setStyle("-fx-background-color: rgba(192,196,63,0.15);");
        return div;
    }

    private static ToggleButton buildMethodBtn(String text, ToggleGroup group, boolean selected) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setTextAlignment(TextAlignment.CENTER);
        btn.setPrefSize(116, 60);
        btn.setFont(Font.font("System", 11));

        String normal = "-fx-background-color: " + COLOR_CARD + "; -fx-text-fill: #fff; -fx-background-radius: 8; -fx-border-color: rgba(192,196,63,0.3); -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;";
        String active = "-fx-background-color: " + COLOR_PRIMARY + "; -fx-text-fill: " + COLOR_ACCENT + "; -fx-background-radius: 8; -fx-border-color: " + COLOR_ACCENT + "; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-font-weight: bold; -fx-cursor: hand;";

        btn.setStyle(selected ? active : normal);
        btn.setSelected(selected);
        btn.selectedProperty().addListener((obs, was, is) -> btn.setStyle(is ? active : normal));
        return btn;
    }

    private static String formatPrice(double price) {
        if (price >= 1_000_000_000) return String.format("%,.1f B", price / 1_000_000_000);
        if (price >= 1_000_000)     return String.format("%,.1f M", price / 1_000_000);
        return String.format("%,.0f", price);
    }
}