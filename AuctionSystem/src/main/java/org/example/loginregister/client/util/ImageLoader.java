package org.example.loginregister.client.util;

import javafx.scene.image.Image;

import java.io.InputStream;

public class ImageLoader {
    private static final String IMAGE_BASE_PATH = "/org/example/loginregister/";
    public static Image loadFromFile(String path){
        InputStream is = ImageLoader.class.getResourceAsStream(IMAGE_BASE_PATH + path);
        if (is == null) {
            System.err.println("file not found: " + path);
            return null;
        }
        return new Image(is);
    }
}
