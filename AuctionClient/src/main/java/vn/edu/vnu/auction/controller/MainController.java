package vn.edu.vnu.auction.controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import vn.edu.vnu.auction.service.SceneManager;
import vn.edu.vnu.auction.util.ImageLoader;

public class MainController implements Initializable {

  private static final String WELCOME_IMAGE_PATH = "hele.jpg";

  static final String LOGIN_FXML = "login.fxml";
  private static final String REGISTER_FXML = "register.fxml";

  static final String LOGIN_TITLE = "Login";
  private static final String REGISTER_TITLE = "Register";

  private static final String STYLE_NAV_ACTIVE =
      "-fx-background-color:  linear-gradient(to bottom right, #8b3a44, #722f37, #5a2028);" +
          "-fx-border-color:  rgba(192,196,63,0.2); -fx-border-radius: 6;" +
          "-fx-background-radius: 6; -fx-cursor: hand; -fx-text-fill: #c0c43f;";
  private static final String STYLE_NAV_NORMAL =
      "-fx-background-color: transparent; -fx-background-radius: 6;" +
          " -fx-cursor: hand; -fx-letter-spacing: 2px; -fx-border-color: rgba(192,196,63,0.4);" +
          " -fx-border-radius: 6; -fx-text-fill: #c0c43f";

  @FXML
  private ImageView welcomeImageView;
  @FXML
  private Button loginButton;
  @FXML
  private Button registerButton;
  @FXML
  private Button exitButton;
  private final SceneManager sceneManager = new SceneManager(getClass());

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    welcomeImageView.setImage(ImageLoader.loadFromFile(WELCOME_IMAGE_PATH));
    Platform.runLater(() -> {
      if (welcomeImageView.getScene() != null && welcomeImageView.getScene().getWindow() != null) {
        Stage stage = (Stage) welcomeImageView.getScene().getWindow();
        stage.setFullScreen(true);
      }
    });
  }

  public void loginButtonOnAction(ActionEvent event) {
    setActiveNav(loginButton);
    sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);
  }

  public void registerButtonOnAction(ActionEvent event) {
    setActiveNav(registerButton);
    sceneManager.switchScene(event, REGISTER_FXML, REGISTER_TITLE);
  }

  public void exitButtonOnAction(ActionEvent event) {
    setActiveNav(exitButton);
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    stage.close();
  }

  private void setActiveNav(Button active) {
    loginButton.setStyle(STYLE_NAV_NORMAL);
    registerButton.setStyle(STYLE_NAV_NORMAL);
    exitButton.setStyle(STYLE_NAV_NORMAL);

    active.setStyle(STYLE_NAV_ACTIVE);
  }
}
