package vn.edu.vnu.auction;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import vn.edu.vnu.auction.service.ConnectionManager;
import vn.edu.vnu.auction.service.MessageRouter;

import java.io.IOException;

public class AuctionApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        boolean connected = ConnectionManager.getInstance().connect();
        if(!connected){
            new Alert(Alert.AlertType.ERROR,
                    "Cannot connect to server. Please start the server first.")
                    .showAndWait();
            Platform.exit();
            return;
        }
        MessageRouter.getInstance().start();
        FXMLLoader fxmlLoader = new FXMLLoader(AuctionApplication.class.getResource("/vn/edu/vnu/auctionclient/main.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setFullScreen(true);
        stage.setTitle("Auction System");
        stage.setScene(scene);
        stage.show();
    }
}
