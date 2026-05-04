package org.example.loginregister.client.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.loginregister.client.service.SceneManager;
import org.example.loginregister.client.socket.SocketClient;
import org.example.loginregister.client.util.ImageLoader;

import java.net.URL;
import java.text.Normalizer;
import java.util.ResourceBundle;

import static org.example.loginregister.client.controller.MainController.LOGIN_FXML;
import static org.example.loginregister.client.controller.MainController.LOGIN_TITLE;

public class RegisterController implements Initializable {
    private final String AUCTION_REGISTER_IMAGE_PATH = "auctionres.png";
    private final String LIBRA_REGISTER_IMAGE_PATH   = "libra.png";

    private static final String ROLE_BIDDER      = "Bidder";
    private static final String ROLE_SELLER      = "Seller";
    private static final int    PASSWORD_MIN_LENGTH = 6;

    @FXML private Button        signIn2Button;
    @FXML private ImageView     libraImageView;
    @FXML private Button        closeButton;
    @FXML private Label         registrationMessageLabel;
    @FXML private PasswordField passwordPF;
    @FXML private PasswordField confirmPasswordPF;
    @FXML private Label         confirmPasswordLabel;
    @FXML private TextField     firstNameTF;
    @FXML private TextField     lastnameTF;
    @FXML private TextField     userNameTF;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private RadioButton   maleRButton, femaleRButton, otherRButton;
    @FXML private TextField     phoneNumberTF;
    @FXML private ImageView     auctionImageView;

    private String selectedGender = "";
    private final SceneManager sceneManager = new SceneManager(getClass());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        URL test = getClass().getResource("/org/example/loginregister/register.fxml");
        auctionImageView.setImage(ImageLoader.loadFromFile(AUCTION_REGISTER_IMAGE_PATH));
        libraImageView.setImage(ImageLoader.loadFromFile(LIBRA_REGISTER_IMAGE_PATH));

        roleComboBox.getItems().addAll(ROLE_BIDDER, ROLE_SELLER);

        ToggleGroup genderGroup = new ToggleGroup();
        maleRButton.setToggleGroup(genderGroup);
        femaleRButton.setToggleGroup(genderGroup);
        otherRButton.setToggleGroup(genderGroup);

        genderGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> obs, Toggle oldToggle, Toggle newToggle) {
                if (newToggle != null) {
                    selectedGender = ((RadioButton) newToggle).getText();
                }
            }
        });
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

        if (isValid) {
            confirmPasswordLabel.setText("Password match");
            registerUser(event);
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

    public void registerUser(ActionEvent event) {

        String raw      = firstNameTF.getText().trim() + " " + lastnameTF.getText().trim();
        String fullName = Normalizer.normalize(raw, Normalizer.Form.NFC);
        String username = userNameTF.getText().trim();
        String password = passwordPF.getText();
        String phone    = phoneNumberTF.getText().trim();
        String role     = roleComboBox.getValue().toUpperCase();
        String email    = username + "@auction.com";

        // Gửi request tới Server qua Socket
        String response = SocketClient.send(
                "REGISTER|" + username + "|" + password + "|" + email
                + "|" + fullName + "|" + selectedGender + "|" + phone + "|" + role
        );
        String[] parts = response.split("\\|");

        if (parts[0].equals("OK")) {
            registrationMessageLabel.setText("Registration successful!");

            // Điều hướng sau đăng ký
            if ("BIDDER".equals(role)) {
                sceneManager.switchScene(event, "bidder_dashboard.fxml", "Bidder Dashboard");
            } else {
                // TODO: tạo seller_dashboard.fxml
                registrationMessageLabel.setText("Registration successful! Welcome, " + fullName);
            }
        } else {
            registrationMessageLabel.setText(parts.length > 1 ? parts[1] : "Registration failed!");
        }
    }
}
