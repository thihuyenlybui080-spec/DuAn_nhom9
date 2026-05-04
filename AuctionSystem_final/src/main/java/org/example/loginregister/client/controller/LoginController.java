package org.example.loginregister.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.loginregister.client.service.SceneManager;
import org.example.loginregister.client.socket.SocketClient;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    @FXML private Button    loginButton;
    @FXML private Button    cancelButton;
    @FXML private Label     messageLabel;
    @FXML private TextField usernameTF;
    @FXML private TextField passwordTF;

    private final SceneManager sceneManager = new SceneManager(getClass());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        URL test = getClass().getResource("/org/example/loginregister/login.fxml");
    }

    public void LoginButtonAction(ActionEvent event) {
        if (!usernameTF.getText().isBlank() && !passwordTF.getText().isBlank()) {
            validateLogin(event);
        } else {
            messageLabel.setText("Please enter username and password!");
        }
    }

    public void cancelButtonAction(ActionEvent event) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void validateLogin(ActionEvent event) {
        String username = usernameTF.getText();
        String password = passwordTF.getText();

        // Gửi request tới Server qua Socket
        String response = SocketClient.send("LOGIN|" + username + "|" + password);
        String[] parts  = response.split("\\|");

        if (parts[0].equals("OK")) {
            String fullName = parts[1];
            String role     = parts[2];
            messageLabel.setText("Welcome, " + fullName + "!");

            // Điều hướng theo role
            if ("BIDDER".equals(role)) {
                sceneManager.switchScene(event, "bidder_dashboard.fxml", "Bidder Dashboard");
            } else if ("ADMIN".equals(role)) {
                // TODO: tạo admin_dashboard.fxml
                messageLabel.setText("Welcome Admin: " + fullName);
            } else if ("SELLER".equals(role)) {
                // TODO: tạo seller_dashboard.fxml
                messageLabel.setText("Welcome Seller: " + fullName);
            }
        } else {
            messageLabel.setText(parts.length > 1 ? parts[1] : "Login failed!");
        }
    }
}
