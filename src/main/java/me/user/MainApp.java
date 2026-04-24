package me.user;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Tạo một cái nhãn chữ
        Label label = new Label("Trời ơi cứu tui!");

        // Cho chữ vào giữa màn hình
        StackPane root = new StackPane();
        root.getChildren().add(label);

        // Tạo khung cửa sổ kích thước 400x300
        Scene scene = new Scene(root, 400, 300);

        primaryStage.setTitle("App JavaFX Đầu Tiên");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}