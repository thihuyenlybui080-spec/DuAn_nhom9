package vn.edu.vnu.auction.controller;

import java.net.URL;
import java.util.ResourceBundle;
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
import vn.edu.vnu.auction.model.entity.user.Admin;
import vn.edu.vnu.auction.model.entity.user.Bidder;
import vn.edu.vnu.auction.model.entity.user.Seller;
import vn.edu.vnu.auction.model.entity.user.User;
import vn.edu.vnu.auction.service.AuctionClientService;
import vn.edu.vnu.auction.service.SceneManager;

public class LoginController implements Initializable {

  @FXML
  private Button cancelButton;
  @FXML
  private Label messageLabel;
  @FXML
  private TextField usernameTF;
  @FXML
  private TextField passwordTF;
  @FXML
  private StackPane rootStackPane;

  private final String REGISTER_FXML = "register.fxml";
  private final String REGISTER_TITLE = "Register";

  private final SceneManager sceneManager = new SceneManager(getClass());

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    URL test = getClass().getResource("/vn/edu/vnu/auctionclient/login.fxml");
    Platform.runLater(() -> {
      if (rootStackPane.getScene() != null) {
        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        stage.setFullScreen(true);
      }
    });
  }

  public void onRegister(ActionEvent event) {
    sceneManager.switchScene(event, REGISTER_FXML, REGISTER_TITLE);
  }

  public void LoginButtonAction(ActionEvent event) {
    messageLabel.setText("You try to login");
    if (usernameTF.getText().isBlank() == false && passwordTF.getText().isBlank() == false) {
      validateLogin();
    } else {
      messageLabel.setText("Please enter username and password!");
    }
  }

  public void cancelButtonAction(ActionEvent event) {
    Stage stage = (Stage) cancelButton.getScene().getWindow();
    stage.close();
  }

  public void validateLogin() {
    String username = usernameTF.getText();
    String password = passwordTF.getText();

    try {
      User user = AuctionClientService.getInstance().login(username, password);

      if (user != null) {
        int userId = user.getId();
        String role = user.getRole() != null ? user.getRole().trim().toUpperCase() : "";
        RegisterController.setCurrentUserId(userId);

        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        ToastNotification.show(stage, "Success", "Login successful! Welcome, " + user.getName(),
            ToastNotification.Type.SUCCESS);
        messageLabel.setText("Login successful! Welcome, " + user.getName());

        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
          switch (role) {
            case "ADMIN" -> {
              AdminDashboardController ctrl = sceneManager.switchSceneAndGetController(
                  stage, "admin_dashboard.fxml", "Admin Dashboard");
              if (ctrl != null) {
                ctrl.setCurrentAdmin((Admin) user);
              }
            }
            case "SELLER" -> {
              SellerDashboardController ctrl = sceneManager.switchSceneAndGetController(
                  stage, "seller_dashboard.fxml", "Seller Dashboard");
              if (ctrl != null) {
                ctrl.setCurrentUser((Seller) user);
              }
            }
            default -> {
              BidderDashboardController ctrl = sceneManager.switchSceneAndGetController(
                  stage, "bidder_dashboard.fxml", "Bidder Dashboard");
              if (ctrl != null) {
                ctrl.setCurrent((Bidder) user);
              }
            }
          }
        });
        pause.play();

      } else {
        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        ToastNotification.show(stage, "Error", "Incorrect username or password!",
            ToastNotification.Type.ERROR);
        messageLabel.setText("Incorrect username or password!");
      }

    } catch (Exception e) {
      Stage stage = (Stage) rootStackPane.getScene().getWindow();
      ToastNotification.show(stage, "Error", e.getMessage(), ToastNotification.Type.ERROR);
      messageLabel.setText(e.getMessage());
      e.printStackTrace();
    }
  }

}
