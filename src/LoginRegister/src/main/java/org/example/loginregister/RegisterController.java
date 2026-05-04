package org.example.loginregister;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Stack;

public class RegisterController implements Initializable {
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

    public void registerUser(){
        // chỗ làm database
    }
}