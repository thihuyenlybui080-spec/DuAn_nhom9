module org.example.loginregister {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.sql;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires java.desktop;
    requires java.management;
    requires org.yaml.snakeyaml;
    requires com.zaxxer.hikari;

    opens org.example.loginregister to javafx.fxml;
    exports org.example.loginregister;
    exports org.example.loginregister.client.util;
    exports org.example.loginregister.client.service;
    exports org.example.loginregister.client.controller;
    exports org.example.loginregister.server.database;
    exports org.example.loginregister.server.dao;
    opens org.example.loginregister.client.controller to javafx.fxml;
    exports org.example.loginregister.server;
    opens org.example.loginregister.server to javafx.fxml;
}