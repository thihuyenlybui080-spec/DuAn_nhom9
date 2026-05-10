package org.example.loginregister;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/org/example/loginregister/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setFullScreen(true);
        stage.setTitle("Auction System");
        stage.setScene(scene);
        stage.show();
    }
}
