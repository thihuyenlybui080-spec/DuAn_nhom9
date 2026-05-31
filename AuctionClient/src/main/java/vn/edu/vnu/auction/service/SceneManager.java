package vn.edu.vnu.auction.service;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

  private static final String FXML_BASE_PATH = "/vn/edu/vnu/auctionclient/";
  private final Class<?> resourceClass;

  public SceneManager(Class<?> resourceClass) {
    this.resourceClass = resourceClass;
  }

  public void switchScene(ActionEvent event, String fxmlFile, String title) {
    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
    switchScene(stage, fxmlFile, title);
  }

  public void switchScene(Stage stage, String fxmlFile, String title) {
    try {
      FXMLLoader loader = new FXMLLoader(resourceClass.getResource(FXML_BASE_PATH + fxmlFile));
      Scene scene = new Scene(loader.load());
      stage.setTitle(title);
      stage.setScene(scene);
      stage.show();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public <T> T switchSceneAndGetController(Stage stage, String fxmlFile, String title) {
    try {
      FXMLLoader loader = new FXMLLoader(resourceClass.getResource(FXML_BASE_PATH + fxmlFile));
      Scene scene = new Scene(loader.load());
      stage.setTitle(title);
      stage.setScene(scene);
      stage.show();
      return loader.getController();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}
