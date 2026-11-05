package com.vaultx.view;

import com.vaultx.model.AppSettings;
import com.vaultx.model.Credential;
import com.vaultx.util.ValidationUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public final class SettingsController implements ScreenController {
    private final AppContext context;
    private final BackgroundTasks tasks;
    private final Runnable onLock;
    private final Runnable onExit;
    private final Runnable onSettingsChanged;
    private final VBox root = new VBox();
    private final ScrollPane scroll = new ScrollPane();
    private final ComboBox<Integer> autoLock = new ComboBox<>();
    private final ComboBox<Integer> clipboard = new ComboBox<>();
    private final ComboBox<AppSettings.Theme> theme = new ComboBox<>();
    private final Label status = Ui.label("", "text-muted");
    private boolean disposed;

    public SettingsController(AppContext context, BackgroundTasks tasks, Runnable onLock,
                              Runnable onExit, Runnable onSettingsChanged) {
        this.context = context; this.tasks = tasks; this.onLock = onLock; this.onExit = onExit;
        this.onSettingsChanged = onSettingsChanged;
        build();
    }

    @Override public Node view() { return scroll; }

    @Override public void dispose() { disposed = true; }

    private void build() {
        root.getStyleClass().add("page-root");
        scroll.setContent(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("app-scroll");
        VBox page = Ui.page("Settings", "Choose how VaultX locks, clears copied values, and appears on this device.");
        configureChoices();
        GridPane preferences = new GridPane();
        preferences.setHgap(22); preferences.setVgap(18);
        preferences.add(Ui.label("Automatic lock", "credential-name"), 0, 0);
        preferences.add(Ui.label("Lock the vault after a period without keyboard or mouse activity.", "credential-subtitle"), 0, 1);
        preferences.add(autoLock, 1, 0, 1, 2);
        preferences.add(Ui.label("Clipboard clearing", "credential-name"), 0, 2);
        preferences.add(Ui.label("Remove a copied value if it is still on the clipboard after this delay.", "credential-subtitle"), 0, 3);
        preferences.add(clipboard, 1, 2, 1, 2);
        preferences.add(Ui.label("Appearance", "credential-name"), 0, 4);
        preferences.add(Ui.label("Choose a comfortable workspace theme.", "credential-subtitle"), 0, 5);
        preferences.add(theme, 1, 4, 1, 2);
        GridPane.setHgrow(autoLock, Priority.ALWAYS); GridPane.setHgrow(clipboard, Priority.ALWAYS); GridPane.setHgrow(theme, Priority.ALWAYS);
        HBox saveRow = Ui.row(12, Ui.button("Save preferences", "button-primary"), status);
        Button save = (Button) saveRow.getChildren().getFirst();
        save.setOnAction(event -> savePreferences());
        VBox preferencesCard = new VBox(16, Ui.label("Vault preferences", "section-title"), preferences, saveRow);
        preferencesCard.getStyleClass().add("card");

        VBox securityCard = new VBox(13, Ui.label("Master password", "section-title"),
                Ui.label("Changing it re-encrypts all credential records with a new key.", "credential-subtitle"));
        Button changePassword = Ui.button("Change master password", "button-quiet");
        changePassword.setOnAction(event -> changeMasterPassword());
        securityCard.getChildren().add(changePassword); securityCard.getStyleClass().add("card");

        Label backupDescription = Ui.label("Backups use a separate password. Restore validates the full file before adding new entries; existing entries are never replaced.", "credential-subtitle");
        backupDescription.setWrapText(true);
        VBox backupCard = new VBox(13, Ui.label("Encrypted backup", "section-title"), backupDescription);
        HBox backupActions = Ui.row(10, Ui.button("Export encrypted backup", "button-quiet"), Ui.button("Import backup", "button-quiet"));
        ((Button) backupActions.getChildren().get(0)).setOnAction(event -> exportBackup());
        ((Button) backupActions.getChildren().get(1)).setOnAction(event -> importBackup());
        backupCard.getChildren().add(backupActions); backupCard.getStyleClass().add("card");

        HBox sessionActions = Ui.row(10, Ui.button("Lock vault now", "button-quiet"), Ui.button("Exit VaultX", "button-danger"));
        ((Button) sessionActions.getChildren().get(0)).setOnAction(event -> onLock.run());
        ((Button) sessionActions.getChildren().get(1)).setOnAction(event -> onExit.run());
        VBox session = new VBox(12, Ui.label("Current session", "section-title"), sessionActions);
        session.getStyleClass().add("card");
        page.getChildren().addAll(preferencesCard, securityCard, backupCard, session);
        root.getChildren().add(page);
    }

    private void configureChoices() {
        autoLock.getItems().setAll(0, 1, 3, 5, 10, 15, 30, 60);
        autoLock.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer value) { return value == null || value == 0 ? "Never" : value + (value == 1 ? " minute" : " minutes"); }
            @Override public Integer fromString(String value) { return 0; }
        });
        clipboard.getItems().setAll(0, 15, 30, 60, 120, 300);
        clipboard.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer value) { return value == null || value == 0 ? "Never" : value + " seconds"; }
            @Override public Integer fromString(String value) { return 0; }
        });
        theme.getItems().setAll(AppSettings.Theme.DARK, AppSettings.Theme.LIGHT);
        theme.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(AppSettings.Theme value) { return value == null ? "" : switch (value) { case DARK -> "Dark"; case LIGHT -> "Light"; case SYSTEM -> "System"; }; }
            @Override public AppSettings.Theme fromString(String value) { return AppSettings.Theme.DARK; }
        });
        AppSettings settings = context.currentSettings();
        autoLock.setValue(settings.autoLockMinutes()); clipboard.setValue(settings.clipboardClearSeconds()); theme.setValue(settings.theme());
        autoLock.setPrefWidth(150); clipboard.setPrefWidth(150); theme.setPrefWidth(150);
    }

    private void savePreferences() {
        try {
            AppSettings settings = new AppSettings(autoLock.getValue(), clipboard.getValue(), theme.getValue());
            status.setText("Saving preferences…");
            tasks.submit(() -> { context.settings().save(settings); return settings; }, saved -> {
                context.applySettings(saved);
                onSettingsChanged.run();
                if (!disposed) status.setText("Preferences saved.");
            }, failure -> { if (!disposed) status.setText(message(failure)); });
        } catch (RuntimeException ex) {
            status.setText(ex.getMessage() == null ? "Preferences could not be saved." : ex.getMessage());
        }
    }

    private void changeMasterPassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Change master password"); dialog.setHeaderText(null); dialog.initOwner(root.getScene().getWindow());
        ButtonType apply = new ButtonType("Update password", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(apply, ButtonType.CANCEL);
        PasswordField current = new PasswordField(); current.setPromptText("Current master password");
        PasswordField next = new PasswordField(); next.setPromptText("At least 12 characters; choose a unique password");
        PasswordField confirm = new PasswordField(); confirm.setPromptText("Confirm new master password");
        Label error = Ui.label("", "auth-error"); error.setWrapText(true);
        GridPane fields = new GridPane(); fields.setHgap(12); fields.setVgap(11);
        fields.add(Ui.label("Current", "metric-label"), 0, 0); fields.add(current, 1, 0);
        fields.add(Ui.label("New", "metric-label"), 0, 1); fields.add(next, 1, 1);
        fields.add(Ui.label("Confirm", "metric-label"), 0, 2); fields.add(confirm, 1, 2);
        fields.add(error, 1, 3); GridPane.setHgrow(current, Priority.ALWAYS); GridPane.setHgrow(next, Priority.ALWAYS); GridPane.setHgrow(confirm, Priority.ALWAYS);
        VBox content = new VBox(12, Ui.label("Set a new master key", "section-title"),
                Ui.label("All encrypted entries will be re-encrypted before the change is saved.", "credential-subtitle"), fields);
        content.setPadding(new Insets(12)); dialog.getDialogPane().setContent(content); dialog.getDialogPane().setMinWidth(520);
        var css = SettingsController.class.getResource("/com/vaultx/styles.css");
        if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        Button applyButton = (Button) dialog.getDialogPane().lookupButton(apply);
        applyButton.addEventFilter(ActionEvent.ACTION, event -> {
            char[] newPassword = next.getText().toCharArray(); char[] repeated = confirm.getText().toCharArray();
            boolean valid = Arrays.equals(newPassword, repeated) && ValidationUtil.validMasterPassword(newPassword)
                    && context.strength().meetsMasterPasswordPolicy(newPassword);
            Arrays.fill(newPassword, '\0'); Arrays.fill(repeated, '\0');
            if (!valid) { event.consume(); error.setText("The new password must match, use at least 12 characters, and rate Strong or Very Strong."); }
        });
        var decision = dialog.showAndWait();
        if (decision.filter(button -> button == apply).isEmpty()) {
            current.clear(); next.clear(); confirm.clear();
            return;
        }
        {
            char[] currentPassword = current.getText().toCharArray();
            char[] newPassword = next.getText().toCharArray();
            char[] repeated = confirm.getText().toCharArray();
            if (!Arrays.equals(newPassword, repeated)) {
                Arrays.fill(currentPassword, '\0'); Arrays.fill(newPassword, '\0'); Arrays.fill(repeated, '\0');
                return;
            }
            Arrays.fill(repeated, '\0');
            status.setText("Re-encrypting your vault…");
            tasks.submit(() -> context.authentication().changeMasterPassword(currentPassword, newPassword),
                    () -> { current.clear(); next.clear(); confirm.clear(); if (!disposed) status.setText("Master password changed."); },
                    failure -> { current.clear(); next.clear(); confirm.clear(); if (!disposed) Dialogs.error(root.getScene().getWindow(), "Password not changed", message(failure)); });
        }
    }

    private void exportBackup() {
        FileChooser chooser = backupChooser("Export encrypted backup");
        chooser.setInitialFileName("vaultx-backup-" + LocalDate.now() + ".vxbackup");
        File destination = chooser.showSaveDialog(root.getScene().getWindow());
        if (destination == null) return;
        var password = PasswordPromptDialog.show(root.getScene().getWindow(), "Protect your backup",
                "Create a separate, strong password for this encrypted backup.", true);
        if (password.isEmpty()) return;
        char[] backupPassword = password.get();
        status.setText("Preparing encrypted backup…");
        tasks.submit(() -> {
            List<Credential> credentials = context.credentials().findAll();
            try {
                int count = credentials.size();
                context.backup().exportBackup(credentials, backupPassword, destination.toPath());
                return count;
            } finally { credentials.forEach(Credential::close); Arrays.fill(backupPassword, '\0'); }
        }, count -> {
            if (!disposed) {
                status.setText("Backup exported.");
                Dialogs.info(root.getScene().getWindow(), "Backup ready", count + " entr" + (count == 1 ? "y was" : "ies were") + " written to an encrypted VaultX backup.");
            }
        }, failure -> {
            Arrays.fill(backupPassword, '\0');
            if (!disposed) { status.setText("Backup export failed."); Dialogs.error(root.getScene().getWindow(), "Backup export failed", message(failure)); }
        });
    }

    private void importBackup() {
        FileChooser chooser = backupChooser("Import encrypted backup");
        File source = chooser.showOpenDialog(root.getScene().getWindow());
        if (source == null) return;
        var password = PasswordPromptDialog.show(root.getScene().getWindow(), "Unlock backup",
                "Enter the separate password used when this backup was created.", false);
        if (password.isEmpty()) return;
        char[] backupPassword = password.get();
        status.setText("Validating encrypted backup…");
        tasks.submit(() -> context.backup().importBackup(source.toPath(), backupPassword), credentials -> {
            Arrays.fill(backupPassword, '\0');
            if (disposed) { credentials.forEach(Credential::close); return; }
            status.setText("Backup validated.");
            boolean confirmed = Dialogs.confirm(root.getScene().getWindow(), "Import backup",
                    credentials.size() + " entries are valid. New entries will be added; matching identifiers will be skipped and nothing will be overwritten.", "Import");
            if (!confirmed) { credentials.forEach(Credential::close); return; }
            status.setText("Importing entries…");
            tasks.submit(() -> context.credentials().importNew(credentials), imported -> {
                credentials.forEach(Credential::close);
                if (!disposed) {
                    status.setText(imported + " new entries imported.");
                    Dialogs.info(root.getScene().getWindow(), "Backup imported", imported + " new entr" + (imported == 1 ? "y was" : "ies were") + " added. Existing entries were not changed.");
                }
            }, failure -> {
                credentials.forEach(Credential::close);
                if (!disposed) { status.setText("Import failed."); Dialogs.error(root.getScene().getWindow(), "Backup import failed", message(failure)); }
            });
        }, failure -> {
            Arrays.fill(backupPassword, '\0');
            if (!disposed) { status.setText("Backup validation failed."); Dialogs.error(root.getScene().getWindow(), "Backup import failed", message(failure)); }
        });
    }

    private static FileChooser backupChooser(String title) {
        FileChooser chooser = new FileChooser(); chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("VaultX encrypted backup (*.vxbackup)", "*.vxbackup"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        return chooser;
    }

    private static String message(Throwable failure) {
        return failure == null || failure.getMessage() == null ? "Try again." : failure.getMessage();
    }
}
