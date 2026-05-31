package vn.edu.vnu.auction.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ToastNotification {

  public enum Type {SUCCESS, ERROR, WARNING, INFO}

  public static void show(Stage stage, String title, String message, Type type) {
    Platform.runLater(() -> {
      Popup popup = new Popup();

      VBox box = new VBox(4);
      box.setPadding(new Insets(12, 16, 12, 16));
      box.setMinWidth(280);
      box.setMaxWidth(280);
      box.setStyle(getStyle(type));

      HBox titleRow = new HBox(6);
      titleRow.setAlignment(Pos.CENTER_LEFT);

      Label lblIcon = new Label(getIcon(type));
      lblIcon.setStyle("-fx-font-size: 14px;");

      Label lblTitle = new Label(title);
      lblTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
      lblTitle.setStyle("-fx-text-fill: white;");

      titleRow.getChildren().addAll(lblIcon, lblTitle);

      Label lblMsg = new Label(message);
      lblMsg.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px;");
      lblMsg.setWrapText(true);

      box.getChildren().addAll(titleRow, lblMsg);
      popup.getContent().add(box);
      popup.setAutoFix(true);

      double x = stage.getX() + stage.getWidth() - 310;
      double y = stage.getY() + 60;
      popup.show(stage, x, y);

      FadeTransition fadeIn = new FadeTransition(Duration.millis(300), box);
      fadeIn.setFromValue(0);
      fadeIn.setToValue(1);
      fadeIn.play();

      /*Tự động ẩn sau 3 giây */
      PauseTransition pause = new PauseTransition(Duration.seconds(3));
      pause.setOnFinished((ActionEvent e) -> {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), box);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e1 -> popup.hide());
        fadeOut.play();
      });
      pause.play();
    });
  }

  private static String getStyle(Type type) {
    String color = switch (type) {
      case SUCCESS -> "#2d8a4e";
      case ERROR -> "#c0392b";
      case WARNING -> "#e67e22";
      default -> "#185FA5";
    };
    return "-fx-background-color: " + color + ";"
        + "-fx-background-radius: 8;"
        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 4);";
  }

  private static String getIcon(Type type) {
    return switch (type) {
      case SUCCESS -> "✅";
      case ERROR -> "❌";
      case WARNING -> "⚠️";
      default -> "ℹ️";
    };
  }
}
