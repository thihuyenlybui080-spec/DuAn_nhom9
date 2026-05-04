package org.example.loginregister;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.loginregister.server.socket.SocketServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Fix hiển thị tiếng Việt
        System.setProperty("file.encoding", "UTF-8");

        // Khởi động Socket Server (chạy ngầm trên máy này)
        // Máy khác sẽ kết nối qua port 9999
        SocketServer.start();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/org/example/loginregister/main.fxml"));
        fxmlLoader.setCharset(StandardCharsets.UTF_8);
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Auction System");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Tắt server khi đóng app
        SocketServer.stop();
    }
}
