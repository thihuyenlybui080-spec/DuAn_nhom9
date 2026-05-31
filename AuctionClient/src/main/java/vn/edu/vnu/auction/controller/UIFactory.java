package vn.edu.vnu.auction.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import vn.edu.vnu.auction.model.entity.Auction;
import vn.edu.vnu.auction.model.entity.AuctionStatus;

import java.time.format.DateTimeFormatter;

import static vn.edu.vnu.auction.controller.PaymentGatewayController.formatPrice;

public class UIFactory {
    public static Label buildStatusBadge(AuctionStatus status){
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
    public static String getCategoryIcon(String category){
        if(category == null){
            return "📦";
        }
        return switch (category.toLowerCase()) {
            case "electronics" -> "💻";
            case "art" -> "🎨";
            case "vehicle" -> "🚗";
            default -> "📦";
        };

    }

    public static VBox buildEmptyState(String icon, String title, String hint) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50));
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 38px;");
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        Label hintLabel = new Label(hint);
        hintLabel.setStyle("-fx-text-fill: #c0c43f; -fx-font-size: 12px;");
        box.getChildren().addAll(iconLabel, titleLabel, hintLabel);
        return box;
    }

    public static HBox buildAuctionCard(Auction auction) {
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
        Label icon = new Label(UIFactory.getCategoryIcon(auction.getItem().getCategory()));
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
        Label badge = UIFactory.buildStatusBadge(auction.getStatus());
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
        card.getChildren().addAll(thumb, info);
        return card;
    }

    public static void updateStatusBadge(Auction auction, Label lblStatusBadge) {
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
}
