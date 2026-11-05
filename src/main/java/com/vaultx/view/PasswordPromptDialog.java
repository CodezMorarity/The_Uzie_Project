package com.vaultx.view;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.Optional;

public final class PasswordPromptDialog {
    private PasswordPromptDialog() { }

    public static Optional<char[]> show(Window owner, String title, String description, boolean confirm) {
        Dialog<char[]> dialog = new Dialog<>();
        dialog.setTitle(title); dialog.setHeaderText(null); dialog.initOwner(owner);
        ButtonType accept = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(accept, ButtonType.CANCEL);
        PasswordField password = new PasswordField(); password.setPromptText("At least 12 characters");
        PasswordField second = new PasswordField(); second.setPromptText("Re-enter backup password");
        javafx.scene.control.Label error = Ui.label("", "auth-error"); error.setWrapText(true);
        VBox content = new VBox(13, Ui.label(description, "page-description"), password);
        if (confirm) content.getChildren().add(second);
        content.getChildren().add(error); content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content); dialog.getDialogPane().setMinWidth(430);
        var css = PasswordPromptDialog.class.getResource("/com/vaultx/styles.css");
        if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        Button button = (Button) dialog.getDialogPane().lookupButton(accept);
        button.addEventFilter(ActionEvent.ACTION, event -> {
            char[] first = password.getText().toCharArray();
            char[] repeat = second.getText().toCharArray();
            boolean valid = first.length >= 12 && (!confirm || Arrays.equals(first, repeat));
            Arrays.fill(first, '\0'); Arrays.fill(repeat, '\0');
            if (!valid) {
                event.consume();
                error.setText(confirm ? "Use at least 12 characters and make both fields match." : "Enter a password with at least 12 characters.");
            }
        });
        dialog.setResultConverter(type -> type == accept ? password.getText().toCharArray() : null);
        Optional<char[]> result = dialog.showAndWait();
        password.clear();
        second.clear();
        return result;
    }
}
