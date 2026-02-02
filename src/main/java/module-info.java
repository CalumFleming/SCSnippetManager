module world.cals.supercollidersnippetmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    // JSON (Jackson)
    // JSON (Jackson) - automatic modules from the Jackson jars
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    // Optional (RichTextFX). If you decide not to use RichTextFX yet,
    // remove these three lines and also remove the dependency from pom.xml.
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires reactfx;

    // JavaOSC for SuperCollider communication (automatic module)
    requires javaosc.core;

    opens world.cals.supercollidersnippetmanager to javafx.fxml, com.fasterxml.jackson.databind, com.fasterxml.jackson.core;
    exports world.cals.supercollidersnippetmanager;
}