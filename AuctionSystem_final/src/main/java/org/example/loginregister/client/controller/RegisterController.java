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
import org.example.loginregister.client.util.ImageLoader;
import org.example.loginregister.client.service.SceneManager;
import org.example.loginregister.server.db.DatabaseConfig;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

import static org.example.loginregister.client.controller.MainController.LOGIN_FXML;
import static org.example.loginregister.client.controller.MainController.LOGIN_TITLE;

public class RegisterController implements Initializable {
    private final String AUCTION_REGISTER_IMAGE_PATH = "auctionres.png";


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


    //kiểm tra điền đủ thông tin hay chưa
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
        boolean isSuccess = registerUser();
        if (isSuccess) {
            String selectedRole = roleComboBox.getValue();
            if (ROLE_SELLER.equalsIgnoreCase(selectedRole)) {
                sceneManager.switchScene(event, "seller_dashboard.fxml", "Seller Dashboard");
            } else {
                sceneManager.switchScene(event, "bidder_dashboard.fxml", "Bidder Dashboard");
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
        String phone = phoneNumberTF.getText().trim();
        String role = roleComboBox.getValue().toUpperCase();
        String email = username + "@auction.com";

        try (Connection conn = DatabaseConfig.getConnection()) {

            // Kiểm tra username đã tồn tại chưa
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                registrationMessageLabel.setText("Username already exists, please choose another!");
                return true;
            }

            // INSERT đủ cột khớp với loginregister.sql
            String insertSql = "INSERT INTO users (username, password, email, full_name, gender, phone, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, username);
            insertStmt.setString(2, password);
            insertStmt.setString(3, email);
            insertStmt.setString(4, fullName);
            insertStmt.setString(5, selectedGender);
            insertStmt.setString(6, phone);
            insertStmt.setString(7, role);
            insertStmt.executeUpdate();

            registrationMessageLabel.setText("Registration successful!");

        } catch (Exception e) {
            registrationMessageLabel.setText("Unable to connect to server, please try again!");
        }
        return true;
    }
}

