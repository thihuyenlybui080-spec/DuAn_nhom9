package org.example.loginregister.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.loginregister.client.util.ImageLoader;
import org.example.loginregister.client.service.SceneManager;

import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {
    private static final String WELCOME_IMAGE_PATH = "welcome.png";

    static final String LOGIN_FXML     = "login.fxml";
    private static final String REGISTER_FXML  = "register.fxml";

    static final String LOGIN_TITLE    = "Login";
    private static final String REGISTER_TITLE = "Register";
    
    @FXML
    private ImageView welcomeImageView;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;
    @FXML
    private Button exitButton;
    @FXML
    private Button adminButton;
    private final SceneManager sceneManager = new SceneManager(getClass());
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        welcomeImageView.setImage(ImageLoader.loadFromFile(WELCOME_IMAGE_PATH));
    }
    
    public void loginButtonOnAction(ActionEvent event) {
        sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);
    }

    public void registerButtonOnAction(ActionEvent event) {
        sceneManager.switchScene(event, REGISTER_FXML, REGISTER_TITLE);
    }
    public void adminButtonOnAction(ActionEvent event) {
        sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);
    }
    public void exitButtonOnAction(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
