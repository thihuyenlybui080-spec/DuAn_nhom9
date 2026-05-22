package org.example.loginregister.client.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.loginregister.client.service.AuctionClientService;
import org.example.loginregister.client.service.SceneManager;
import org.example.loginregister.server.model.entity.user.Bidder;
import org.example.loginregister.server.model.entity.user.Seller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static org.example.loginregister.client.controller.MainController.LOGIN_FXML;
import static org.example.loginregister.client.controller.MainController.LOGIN_TITLE;

public class RegisterController implements Initializable {
    private final String AUCTION_REGISTER_IMAGE_PATH = "mini.png";


    private static final String ROLE_BIDDER = "Bidder";
    private static final String ROLE_SELLER = "Seller";

    private static final int PASSWORD_MIN_LENGTH = 6;
    @FXML
    private Button signIn2Button;
    @FXML
    private ImageView libraImageView;
    @FXML
    private Button closeButton;
    @FXML
    private Label registrationMessageLabel;
    @FXML
    private PasswordField passwordPF;
    @FXML
    private PasswordField confirmPasswordPF;
    @FXML
    private Label confirmPasswordLabel;
    @FXML
    private TextField firstNameTF;
    @FXML
    private TextField lastnameTF;
    @FXML
    private TextField userNameTF;
    @FXML
    private TextField emailTF;
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private RadioButton maleRButton, femaleRButton, otherRButton;
    @FXML
    private TextField phoneNumberTF;
    @FXML
    private ImageView auctionImageView;
    @FXML
    private StackPane rootStackPane;

    String selectedGender = "";

    @FXML
    ToggleGroup genderGroup = new ToggleGroup();
    private final SceneManager sceneManager = new SceneManager(getClass());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        URL test = getClass().getResource("/org/example/loginregister/register.fxml");

        roleComboBox.getItems().addAll(ROLE_BIDDER, ROLE_SELLER);
        ToggleGroup genderGroup = new ToggleGroup();
        maleRButton.setToggleGroup(genderGroup);
        femaleRButton.setToggleGroup(genderGroup);
        otherRButton.setToggleGroup(genderGroup);

        Platform.runLater(() -> {
            if(rootStackPane.getScene() != null) {
                Stage stage = (Stage) rootStackPane.getScene().getWindow();
                stage.setFullScreen(true);
            }
        });

        genderGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> observableValue, Toggle oldToggle, Toggle newToggle) {
                onGenderSelected(newToggle);
            }
        });
    }

    public void onGenderSelected(Toggle selectedToggle) {
        if (selectedToggle == null) {
            String selectedGender = "";
            return;
        }
        RadioButton selected = (RadioButton) selectedToggle;
        selectedGender = selected.getText();
    }

    public void onRegisterButtonClicked(ActionEvent event) {
        registrationMessageLabel.setText("");
        confirmPasswordLabel.setText("");

        boolean isValid = true;
        if (!areAllFieldsFilled()) {
            registrationMessageLabel.setText("Please fill in all required fields");
            isValid = false;
        }

        if (!isPasswordLongEnough(passwordPF.getText())) {
            confirmPasswordLabel.setText("Password must be at least " + PASSWORD_MIN_LENGTH + " characters!");
            isValid = false;
        }

        if (!doPasswordsMatch()) {
            confirmPasswordLabel.setText("Password do not match!");
            isValid = false;
        }

        if (!isPhoneNumberValid(phoneNumberTF.getText())) {
            registrationMessageLabel.setText("Phone must be 10 - 11 digits");
            isValid = false;
        }
        if(!isEmailValid(emailTF.getText())){
            registrationMessageLabel.setText("Email must be abc@gmail.com");
            isValid = false;
        }
        if (!isValid) return;
        boolean isSuccess = registerUser();
        if (isSuccess) {
            String selectedRole = roleComboBox.getValue();
            if (ROLE_SELLER.equalsIgnoreCase(selectedRole)) {
                String fullName = firstNameTF.getText() + lastnameTF.getText();
                try{
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/org/example/loginregister/seller_dashboard.fxml"));
                    Scene scene = new Scene(loader.load());

                    SellerDashboardController ctrl = loader.getController();
                    ctrl.setCurrentUser(new Seller(userNameTF.getText(), passwordPF.getText(), emailTF.getText(), fullName ));
                    Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
                    stage.setScene(scene);
                    stage.show();
                }  catch (IOException e){
                    e.printStackTrace();
                }
            } else {
                String fullName = firstNameTF.getText() + lastnameTF.getText();
                try{
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/org/example/loginregister/bidder_dashboard.fxml"));
                    Scene scene = new Scene(loader.load());

                    BidderDashboardController ctrl = loader.getController();
                    ctrl.setCurrent(new Bidder(userNameTF.getText(), passwordPF.getText(), emailTF.getText(), fullName));
                    Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
                    stage.setScene(scene);
                    stage.show();
                }  catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean areAllFieldsFilled() {
        return !firstNameTF.getText().trim().isEmpty()
                && !lastnameTF.getText().trim().isEmpty()
                && !userNameTF.getText().trim().isEmpty()
                && !passwordPF.getText().trim().isEmpty()
                && !confirmPasswordPF.getText().trim().isEmpty()
                && !phoneNumberTF.getText().trim().isEmpty()
                && roleComboBox.getValue() != null
                && !selectedGender.isEmpty();
    }
    private boolean isEmailValid(String email){
        return email.endsWith("@gmail.com");
    }

    private boolean isPhoneNumberValid(String phone) {
        return phone.matches("^[0-9]{10,11}$");
    }

    private boolean isPasswordLongEnough(String password) {
        return password.length() >= PASSWORD_MIN_LENGTH;
    }

    private boolean doPasswordsMatch() {
        return passwordPF.getText().equals(confirmPasswordPF.getText());
    }

    public void closeButtonOnAction(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
        Platform.exit();
    }

    public void returnLogin(ActionEvent event) {
        sceneManager.switchScene(event, LOGIN_FXML, LOGIN_TITLE);
    }

    public boolean registerUser() {
        String fullName = firstNameTF.getText().trim() + " " + lastnameTF.getText().trim();
        String username = userNameTF.getText().trim();
        String password = passwordPF.getText();
        String phone    = phoneNumberTF.getText().trim();
        String role     = roleComboBox.getValue().toUpperCase();
        String email    = emailTF.getText().trim();

        try {
            AuctionClientService.getInstance().register(username, password, fullName, email, phone, selectedGender, role);
            registrationMessageLabel.setText("Registration successful!");
            return true;
        } catch (RuntimeException e) {
            String message = e.getMessage() != null ? e.getMessage() : "Registration failed";
            if (message.toLowerCase().contains("duplicate") || message.toLowerCase().contains("already exists")) {
                registrationMessageLabel.setText("Username already exists, please choose another!");
            } else {
                registrationMessageLabel.setText(message);
            }
            return false;
        }
    }
}

