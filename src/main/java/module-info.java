module ippp4s4.quicksteel {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires javafx.swing;
    requires com.google.common;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    opens ippp4s4.quicksteel to javafx.fxml;
    exports ippp4s4.quicksteel;

    opens ippp4s4.quicksteel.model to javafx.fxml;
    exports ippp4s4.quicksteel.model;
}