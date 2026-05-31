package vn.edu.vnu.auction.util;

import java.io.InputStream;
import javafx.scene.image.Image;

public class ImageLoader {

  private static final String IMAGE_BASE_PATH = "/vn/edu/vnu/auctionclient/";

  public static Image loadFromFile(String path) {
    InputStream is = ImageLoader.class.getResourceAsStream(IMAGE_BASE_PATH + path);
    if (is == null) {
      System.err.println("file not found: " + path);
      return null;
    }
    return new Image(is);
  }
}
