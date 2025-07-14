module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires java.prefs;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens com.example.demo.controllers to javafx.fxml;
    exports com.example.demo.controllers;
}