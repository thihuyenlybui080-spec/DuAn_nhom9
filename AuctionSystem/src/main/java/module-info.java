module org.example.loginregister {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.sql;

    opens org.example.loginregister to javafx.fxml;
    exports org.example.loginregister;
    exports org.example.loginregister.client.util;
    exports org.example.loginregister.client.service;
    exports org.example.loginregister.client.controller;
    opens org.example.loginregister.client.controller to javafx.fxml;
}