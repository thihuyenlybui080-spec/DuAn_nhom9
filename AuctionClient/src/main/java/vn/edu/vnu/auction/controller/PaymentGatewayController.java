package vn.edu.vnu.auction.controller;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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

    private static final String COLOR_PRIMARY   = "#722f37";
    private static final String COLOR_ACCENT    = "#c0c43f";
    private static final String COLOR_DARK      = "#1a0d0f";
    private static final String COLOR_CARD      = "#3d1c21";
    private static final String COLOR_SUCCESS   = "#2d8a4e";
    private static final String COLOR_TEXT      = "#ffffff";

    public static void Show(AuctionResult result, Runnable onSuccess ){
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

    private static VBox buildRoot(AuctionResult result, Stage stage, Runnable onSuccess){
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " +COLOR_DARK + ";" +
                "-fx-border-color: " + COLOR_ACCENT + ";" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;"
        );

        root.getChildren().addAll(
                buildHeader(stage),
                buildOrderInfo(result),
                buildMethodSelector(),
                buildPayButton(result, stage, onSuccess)
        );
        return root;
    }

    private static HBox buildHeader(Stage stage){
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, " + COLOR_PRIMARY + ", " + COLOR_CARD + ");" +
                "-fx-background-radius: 10 10 0 0;");

        Label icon = new Label("🏦");
        icon.setStyle("-fx-font-size: 22px;");

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        titleBox.setPadding(new Insets(0, 0, 0, 10));

        Label title = new Label("Payment Gateway");
        title.setFont(Font.font("System", FontWeight.BOLD, 15));
        title.setStyle("-fx-text-fill: " + COLOR_ACCENT + ";");

        Label subtitle = new Label("Secure Simulated Payment");
        subtitle.setStyle("-fx-text-fill: #ffffff; -fx-opacity: 0.6; -fx-font-size: 11px;");

        titleBox.getChildren().addAll(title, subtitle);

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;" +
                        "-fx-opacity: 0.7;"
        );

        btnClose.setOnAction(e -> stage.close());
        btnClose.setOnMouseEntered( e -> btnClose.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #e53935;" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;"
        ));

        btnClose.setOnMouseExited(e -> btnClose.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #ffffff;" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;" +
                        "-fx-opacity: 0.7;"
        ));

        header.getChildren().addAll(icon, titleBox, btnClose);
        return header;
    }

    private static VBox buildOrderInfo(AuctionResult result){
        VBox box = new VBox(12);
        box.setPadding(new Insets(20, 20, 10, 20));

        Label lblSection = new Label("ORDER SUMMARY");
        lblSection.setStyle(
                "-fx-text-fill: " + COLOR_ACCENT + ";" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-opacity: 0.8;"
        );

        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: " + COLOR_CARD + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: rgba(192,196,63,0.3);" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-width: 1;"
        );

        String itemName = result.getItem() != null ? result.getItem().getItemName() : "Unknown Item";
        int auctionId = result.getAuctionId();

        card.getChildren().addAll(
                buildInfoRow("🛍 Item", itemName),
                buildDivider(),
                buildInfoRow("🔖 Order ID", "AUCTION-" + auctionId),
                buildDivider(),
                buildAmountRow(result.getFinalPrice())
        );

        box.getChildren().addAll(lblSection, card);
        return box;
    }

    private static HBox buildInfoRow(String label, String value){
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

    private static VBox buildMethodSelector(){
        VBox box = new VBox(10);
        box.setPadding(new Insets(16, 20, 10, 20));

        Label lblSection = new Label("PAYMENT METHOD");
        lblSection.setStyle(
                "-fx-text-fill: " + COLOR_ACCENT + ";" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-opacity: 0.8;"
        );

        ToggleGroup group = new ToggleGroup();

        HBox methods = new HBox(10);
        methods.getChildren().addAll(
                buildMethodBtn("🏧\nATM Card",  group, true),
                buildMethodBtn("📱\nQR Code",   group, false),
                buildMethodBtn("💳\nVisa/MC",   group, false)
        );
        box.getChildren().addAll(lblSection, methods);
        return box;
    }

    private static ToggleButton buildMethodBtn(String text, ToggleGroup group, boolean selected){
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setTextAlignment(TextAlignment.CENTER);
        btn.setPrefSize(116, 60);
        btn.setFont(Font.font("System", 11));

        String normal  = "-fx-background-color: " + COLOR_CARD +
                "; -fx-text-fill: #fff; -fx-background-radius: 8;" +
                " -fx-border-color: rgba(192,196,63,0.3);" +
                " -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;";
        String active  = "-fx-background-color: " + COLOR_PRIMARY +
                "; -fx-text-fill: " + COLOR_ACCENT +
                "; -fx-background-radius: 8; -fx-border-color: "
                + COLOR_ACCENT + "; -fx-border-radius: 8; -fx-border-width: 1.5;" +
                " -fx-font-weight: bold; -fx-cursor: hand;";

        btn.setStyle(selected ? active : normal);
        btn.selectedProperty().addListener((obs, wasSelected, isNowSelected) ->
                btn.setStyle(isNowSelected ? active : normal)
        );
        return btn;
    }

    private static VBox buildPayButton(AuctionResult result, Stage stage, Runnable onSuccess){
        VBox box = new VBox(12);
        box.setPadding(new Insets(10, 20, 24, 20));
        box.setAlignment(Pos.CENTER);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(380);
        progressBar.setPrefHeight(6);
        progressBar.setStyle(
                "-fx-accent: " + COLOR_ACCENT + ";" +
                        "-fx-background-color: " + COLOR_CARD + ";" +
                        "-fx-background-radius: 3;" +
                        "-fx-border-radius: 3;"
        );

        progressBar.setVisible(false);
        Label lblStatus = new Label("");
        lblStatus.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-opacity: 0.8;");
        lblStatus.setVisible(false);

        Button btnPay = new Button("  Confirm Payment  💳");
        btnPay.setPrefWidth(380);
        btnPay.setPrefHeight(48);
        btnPay.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnPay.setStyle(
                "-fx-background-color: " + COLOR_ACCENT + ";" +
                        "-fx-text-fill: " + COLOR_PRIMARY + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );

        btnPay.setOnAction(e -> {
            btnPay.setDisable(true);
            progressBar.setVisible(true);
            lblStatus.setVisible(true);
            lblStatus.setText("Connecting to payment server...");

            Timeline timeline = new Timeline();

            KeyFrame step1 = new KeyFrame(Duration.millis(600), event -> {
                progressBar.setProgress(0.35);
                lblStatus.setText("Verifying order information...");
            });

            KeyFrame step2 = new KeyFrame(Duration.millis(1400), ev -> {
                progressBar.setProgress(0.70);
                lblStatus.setText("Processing payment...");
            });

            KeyFrame step3 = new KeyFrame(Duration.millis(2200), ev -> {
                progressBar.setProgress(1.0);
                lblStatus.setText("✅  Payment successful!");
                lblStatus.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 12px; -fx-font-weight: bold;");
            });

            KeyFrame step4 = new KeyFrame(Duration.millis(2800), event -> {
                try{
                    AuctionClientService.getInstance().payAuction(result.getAuctionId());
                }catch (Exception ex){
                    ex.printStackTrace();
                }
                if(onSuccess != null) onSuccess.run();
                stage.close();
            });
            timeline.getKeyFrames().addAll(step1, step2, step3, step4);
            timeline.play();
        });
        box.getChildren().addAll(progressBar, lblStatus, btnPay);
        return box;

    }
    private static String formatPrice(double price) {
        if (price >= 1_000_000_000) return String.format("%.1f B", price / 1_000_000_000);
        if (price >= 1_000_000)     return String.format("%.1f M", price / 1_000_000);
        return String.format("%.0f", price);
    }
}
