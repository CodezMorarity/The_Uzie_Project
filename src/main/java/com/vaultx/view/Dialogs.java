package com.vaultx.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

public final class Dialogs {
    private Dialogs() { }

    public static boolean confirm(Window owner, String title, String message, String action) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, new ButtonType(action));
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(owner);
        style(alert);
        return alert.showAndWait().filter(button -> button.getText().equals(action)).isPresent();
    }

    public static void error(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(owner);
        style(alert);
        alert.showAndWait();
    }

    public static void info(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(owner);
        style(alert);
        alert.showAndWait();
    }

    public static void style(Alert alert) {
        var css = Dialogs.class.getResource("/com/vaultx/styles.css");
        if (css != null) alert.getDialogPane().getStylesheets().add(css.toExternalForm());
    }
}
