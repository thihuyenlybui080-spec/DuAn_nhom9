package org.example.loginregister.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.loginregister.client.service.SceneManager;

import org.example.loginregister.server.database.DatabaseConfig;


import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

import static org.example.loginregister.client.controller.MainController.LOGIN_FXML;
import static org.example.loginregister.client.controller.MainController.LOGIN_TITLE;

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

        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0) {
                messageLabel.setText("Login successful! Welcome, " + username);
            } else {
                messageLabel.setText("Incorrect username or password!");
            }

        } catch (Exception e) {
            messageLabel.setText("Unable to connect to server, please try again!");
        }
    }

}
