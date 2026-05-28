module vn.edu.vnu.auctionclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires org.slf4j;
    requires org.yaml.snakeyaml;
    requires java.desktop;

    opens vn.edu.vnu.auction to javafx.fxml;
    opens vn.edu.vnu.auction.controller to javafx.fxml;
    exports vn.edu.vnu.auction;
    exports vn.edu.vnu.auction.controller;
}