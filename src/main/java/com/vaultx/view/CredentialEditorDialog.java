package com.vaultx.view;

import com.vaultx.exception.VaultException;
import com.vaultx.model.Credential;
import com.vaultx.util.ValidationUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class CredentialEditorDialog {
    private static final List<String> CATEGORIES = List.of("Social Media", "Education", "Finance", "Work", "Gaming", "Other");

    private CredentialEditorDialog() { }

    public static Optional<Credential> show(Window owner, Credential existing) {
        Dialog<Credential> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add credential" : "Edit credential");
        dialog.setHeaderText(null);
        dialog.initOwner(owner);
        ButtonType saveType = new ButtonType(existing == null ? "Save credential" : "Save changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField name = new TextField(existing == null ? "" : existing.name());
        TextField url = new TextField(existing == null ? "" : existing.url());
        TextField username = new TextField(existing == null ? "" : existing.username());
        PasswordField masked = new PasswordField();
        TextField visible = new TextField();
        CheckBox reveal = new CheckBox("Show password");
        StackPane passBox = new StackPane(masked, visible);
        visible.setVisible(false); visible.setManaged(false);
        reveal.selectedProperty().addListener((observable, oldValue, shown) -> {
            if (shown) { visible.setText(masked.getText()); visible.setVisible(true); visible.setManaged(true); masked.setVisible(false); masked.setManaged(false); }
            else { masked.setText(visible.getText()); masked.setVisible(true); masked.setManaged(true); visible.setVisible(false); visible.setManaged(false); }
        });
        ComboBox<String> category = new ComboBox<>(); category.getItems().setAll(CATEGORIES);
        category.setValue(existing == null ? "Other" : existing.category());
        TextArea notes = new TextArea(existing == null ? "" : existing.notes());
        notes.setPromptText("Optional notes"); notes.setPrefRowCount(4); notes.setWrapText(true);
        CheckBox favorite = new CheckBox("Mark as favorite"); favorite.setSelected(existing != null && existing.favorite());
        Label error = Ui.label("", "auth-error"); error.setWrapText(true);
        Label passwordHelp = Ui.label(existing == null ? "Use a unique password for this account." : "Leave blank to keep the current password.", "text-subtle");

        GridPane fields = new GridPane();
        fields.setHgap(14); fields.setVgap(10); fields.setPadding(new Insets(8, 2, 8, 2));
        fields.add(Ui.label("Account name", "metric-label"), 0, 0); fields.add(name, 1, 0);
        fields.add(Ui.label("Website URL", "metric-label"), 0, 1); fields.add(url, 1, 1);
        fields.add(Ui.label("Username / email", "metric-label"), 0, 2); fields.add(username, 1, 2);
        fields.add(Ui.label("Password", "metric-label"), 0, 3); fields.add(passBox, 1, 3);
        fields.add(passwordHelp, 1, 4); fields.add(reveal, 1, 5);
        fields.add(Ui.label("Category", "metric-label"), 0, 6); fields.add(category, 1, 6);
        fields.add(Ui.label("Notes", "metric-label"), 0, 7); fields.add(notes, 1, 7);
        fields.add(favorite, 1, 8); fields.add(error, 1, 9);
        GridPane.setHgrow(name, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(url, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(username, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(passBox, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(category, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(notes, javafx.scene.layout.Priority.ALWAYS);
        fields.setMinWidth(500);
        VBox content = new VBox(13, Ui.label(existing == null ? "Add an account to your vault" : "Update account details", "section-title"), fields);
        content.setPadding(new Insets(12, 14, 14, 14));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(610);
        var css = CredentialEditorDialog.class.getResource("/com/vaultx/styles.css");
        if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());

        Button save = (Button) dialog.getDialogPane().lookupButton(saveType);
        save.addEventFilter(ActionEvent.ACTION, event -> {
            Credential candidate = null;
            try {
                candidate = fromFields(existing, name, url, username, masked, visible, category, notes, favorite);
                ValidationUtil.validateCredential(candidate);
            } catch (VaultException ex) {
                event.consume();
                error.setText(ex.getMessage());
            } finally {
                if (candidate != null) candidate.close();
            }
        });
        dialog.setResultConverter(button -> button == saveType
                ? fromFields(existing, name, url, username, masked, visible, category, notes, favorite) : null);
        Optional<Credential> result = dialog.showAndWait();
        masked.clear();
        visible.clear();
        return result;
    }

    private static Credential fromFields(Credential existing, TextField name, TextField url, TextField username,
                                         PasswordField masked, TextField visible, ComboBox<String> category,
                                         TextArea notes, CheckBox favorite) {
        String typed = visible.isVisible() ? visible.getText() : masked.getText();
        char[] password;
        if (existing != null && typed.isEmpty()) password = existing.password();
        else password = typed.toCharArray();
        try {
            if (existing == null) {
                return Credential.create(name.getText().strip(), url.getText().strip(), username.getText().strip(),
                        password, category.getValue(), notes.getText(), favorite.isSelected());
            }
            return existing.edited(name.getText().strip(), url.getText().strip(), username.getText().strip(),
                    password, category.getValue(), notes.getText(), favorite.isSelected());
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
