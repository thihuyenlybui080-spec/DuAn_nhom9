package org.example.loginregister.client.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.loginregister.client.service.SceneManager;
import org.example.loginregister.client.service.AuctionClientService;
import org.example.loginregister.server.model.entity.user.User;

import java.net.URL;
import java.util.ResourceBundle;
import org.example.loginregister.server.model.entity.user.Admin;
import org.example.loginregister.server.model.entity.user.Seller;
import org.example.loginregister.server.model.entity.user.Bidder;

public class LoginController implements Initializable {
    @FXML
    private Button loginButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Label messageLabel;
    @FXML
    private TextField usernameTF;
    @FXML
    private TextField passwordTF;
    @FXML
    private Button btnRegister;
    @FXML
    private StackPane rootStackPane;

    private final String REGISTER_FXML = "register.fxml";
    private final String REGISTER_TITLE = "Register";

    private final SceneManager sceneManager = new SceneManager(getClass());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        URL test = getClass().getResource("/org/example/loginregister/login.fxml");
        Platform.runLater(() -> {
            if(rootStackPane.getScene() != null) {
                Stage stage = (Stage) rootStackPane.getScene().getWindow();
                stage.setFullScreen(true);
            }
        });
    }

    public void onRegister(ActionEvent event){
        sceneManager.switchScene(event, REGISTER_FXML, REGISTER_TITLE);
    }
    public void LoginButtonAction(ActionEvent event){
        messageLabel.setText("You try to login");
        if(usernameTF.getText().isBlank() == false && passwordTF.getText().isBlank() == false){
            validateLogin();
        }
        else {
            messageLabel.setText("Please enter username and password!");
        }
    }
    public void cancelButtonAction(ActionEvent event){
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    public void validateLogin() {
        String username = usernameTF.getText();
        String password = passwordTF.getText();

        try {
            User user = AuctionClientService.getInstance().login(username, password);

            if (user != null) {
                String rawId = user.getId();
                int userId = Integer.parseInt(rawId.contains("-") ? rawId.substring(rawId.lastIndexOf("-") + 1) : rawId);
                String role = user.getRole() != null ? user.getRole().trim().toUpperCase() : "";

                messageLabel.setText("Login successful! Welcome, " + user.getName());

                PauseTransition pause = new PauseTransition(Duration.seconds(1));
                pause.setOnFinished(e -> {
                    Stage stage = (Stage) rootStackPane.getScene().getWindow();
                    switch (role) {
                        case "ADMIN" -> {
                            AdminDashboardController ctrl = sceneManager.switchSceneAndGetController(
                                    stage, "admin_dashboard.fxml", "Admin Dashboard");
                            if (ctrl != null) ctrl.setCurrentAdmin((Admin) user);
                        }
                        case "SELLER" -> {
                            SellerDashboardController ctrl = sceneManager.switchSceneAndGetController(
                                    stage, "seller_dashboard.fxml", "Seller Dashboard");
                            if (ctrl != null) ctrl.setCurrentUser((Seller) user);
                        }
                        default -> {
                            BidderDashboardController ctrl = sceneManager.switchSceneAndGetController(
                                    stage, "bidder_dashboard.fxml", "Bidder Dashboard");
                            if (ctrl != null) ctrl.setCurrent((Bidder) user);
                        }
                    }
                });
                pause.play();

            } else {
                messageLabel.setText("Incorrect username or password!");
            }

        } catch (Exception e) {
            messageLabel.setText(e.getMessage());
            e.printStackTrace();
        }
    }

}
