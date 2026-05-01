module org.example.loginregister {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens org.example.loginregister to javafx.fxml;
    exports org.example.loginregister;
}