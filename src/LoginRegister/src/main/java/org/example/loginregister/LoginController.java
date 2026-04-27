package org.example.loginregister;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){

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
    public void validateLogin(){

    }
}
