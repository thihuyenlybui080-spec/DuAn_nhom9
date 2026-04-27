package org.example.loginregister;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {
    
    // MySQL Database connection constants
    private static final String DB_URL = "jdbc:mysql://localhost:3306/loginregister";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";
    
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
    
    public void initialize(URL url, ResourceBundle resourceBundle){
        File libraFile = new File("libra.png");
        Image libraImage = new Image(libraFile.toURI().toString());
        libraImageView.setImage(libraImage);
    }
    
    public void registerButtonOnAction(ActionEvent event){
        if(passwordPF.getText().equals(confirmPasswordPF.getText())){
            confirmPasswordLabel.setText("You are set");
            registrationMessageLabel.setText("User has been registered successfully!");
        } else {
            confirmPasswordLabel.setText("Password does not match!");
        }
        registerUser();
    }
    
    public void closeButtonOnAction(ActionEvent event){
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
        Platform.exit();
    }

    public void registerUser() {
        String firstname = firstNameTF.getText();
        String lastname  = lastnameTF.getText();
        String username  = userNameTF.getText();
        String password  = passwordPF.getText();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

            // Kiểm tra username đã tồn tại chưa
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                registrationMessageLabel.setText("Username already exists, please choose another!");
                return;
            }

            // Thêm người dùng mới vào database
            String insertSql = "INSERT INTO users (firstname, lastname, username, password) VALUES (?, ?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, firstname);
            insertStmt.setString(2, lastname);
            insertStmt.setString(3, username);
            insertStmt.setString(4, password);
            insertStmt.executeUpdate();

            registrationMessageLabel.setText("Registration successful!");

        } catch (Exception e) {
            registrationMessageLabel.setText("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
