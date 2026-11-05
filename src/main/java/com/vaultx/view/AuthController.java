package com.vaultx.view;

import com.vaultx.exception.VaultException;
import com.vaultx.model.PasswordStrength;
import com.vaultx.model.StrengthLevel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.function.Consumer;

public final class AuthController implements ScreenController {
    private final AppContext context;
    private final BackgroundTasks tasks;
    private final Runnable onUnlocked;
    private final boolean locked;
    private final HBox root = new HBox();
    private final PasswordField master = new PasswordField();
    private final PasswordField confirm = new PasswordField();
    private final Label error = Ui.label("", "auth-error");
    private final Label strengthLabel = Ui.label("Choose a strong master password", "strength-label", "text-muted");
    private final Region strengthBar = new Region();
    private final Button submit = Ui.button("Create secure vault", "button-primary");

    public AuthController(AppContext context, BackgroundTasks tasks, Runnable onUnlocked, boolean locked) {
        this.context = context;
        this.tasks = tasks;
        this.onUnlocked = onUnlocked;
        this.locked = locked;
        build();
    }

    @Override public Node view() { return root; }

    private void build() {
        root.getStyleClass().add("auth-root");
        VBox story = new VBox(25);
        story.getStyleClass().add("auth-story");
        HBox brand = new HBox(12, IconFactory.create("shield", 34, Ui.GREEN),
                new VBox(3, Ui.label("VaultX", "brand-name"), Ui.label("SECURE PASSWORD MANAGER", "brand-tagline")));
        brand.setAlignment(Pos.CENTER_LEFT);
        Label promise = Ui.label("Your credentials,\nin your hands.", "auth-hero");
        promise.setWrapText(true);
        Label copy = Ui.label("A private vault that stays on this device. No account, no cloud sync, no connection required.", "auth-copy");
        copy.setWrapText(true);
        VBox principles = new VBox(13,
                feature("shield", "Encrypted on this device", "Your vault entries are encrypted before they are saved."),
                feature("lock", "One key, yours alone", "Your master password unlocks the encryption key."),
                feature("check", "Ready offline", "Your vault remains available without an internet connection."));
        Region push = new Region(); VBox.setVgrow(push, Priority.ALWAYS);
        story.getChildren().addAll(brand, promise, copy, principles, push,
                Ui.label("VaultX  •  Private by design", "text-subtle"));
        story.setPadding(new Insets(46, 56, 30, 56));
        HBox.setHgrow(story, Priority.ALWAYS);
        story.setPrefWidth(650);

        boolean setup = !context.authentication().isConfigured();
        VBox form = new VBox(16);
        form.getStyleClass().add("auth-form-panel");
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPrefWidth(500);
        form.setMinWidth(410);
        Label formTitle = Ui.label(setup ? "Create your vault" : locked ? "Vault locked" : "Welcome back", "auth-title");
        Label formSummary = Ui.label(setup ? "Start with a master password you have not used anywhere else." : "Enter your master password to unlock your local vault.", "page-description");
        formTitle.setWrapText(true); formSummary.setWrapText(true);
        formTitle.setMaxWidth(Double.MAX_VALUE); formSummary.setMaxWidth(Double.MAX_VALUE);
        form.getChildren().addAll(
                formTitle, formSummary);

        addField(form, "Master password", master, setup ? "At least 12 characters, with a strong rating" : "Enter your master password");
        if (setup) {
            strengthBar.setMinHeight(4);
            strengthBar.setMaxHeight(4);
            strengthBar.setStyle("-fx-background-color: #354243; -fx-background-radius: 4;");
            VBox strengthMeter = new VBox(7, Ui.label("Password strength", "text-subtle"), strengthBar, strengthLabel);
            strengthMeter.setMaxWidth(Double.MAX_VALUE);
            form.getChildren().add(strengthMeter);
            addField(form, "Confirm master password", confirm, "Re-enter your master password");
            Label note = Ui.label("VaultX does not store the master password. If you forget it, there is no recovery service.", "text-subtle");
            note.setWrapText(true);
            form.getChildren().add(note);
        }
        error.setWrapText(true);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setDefaultButton(true);
        submit.setText(setup ? "Create secure vault" : "Unlock vault");
        submit.setOnAction(event -> {
            if (setup) createVault();
            else unlockVault();
        });
        master.setOnAction(event -> {
            if (setup) confirm.requestFocus();
            else submit.fire();
        });
        confirm.setOnAction(event -> submit.fire());
        master.textProperty().addListener((observable, oldValue, newValue) -> {
            if (setup) updateStrength(newValue.toCharArray());
            error.setText("");
        });
        form.getChildren().addAll(error, submit);
        Region formSpacer = new Region(); VBox.setVgrow(formSpacer, Priority.ALWAYS);
        form.getChildren().add(formSpacer);
        form.setPadding(new Insets(42, 44, 42, 44));
        root.getChildren().addAll(story, form);
        HBox.setHgrow(form, Priority.SOMETIMES);
        VBox.setVgrow(root, Priority.ALWAYS);
    }

    private VBox feature(String icon, String title, String description) {
        HBox row = new HBox(13, IconFactory.create(icon, 20, Ui.GREEN),
                new VBox(4, Ui.label(title, "credential-name"), Ui.label(description, "credential-subtitle")));
        row.setAlignment(Pos.TOP_LEFT);
        return new VBox(row);
    }

    private void addField(VBox form, String labelText, PasswordField field, String prompt) {
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        Label label = Ui.label(labelText, "metric-label");
        form.getChildren().addAll(label, field);
    }

    private void updateStrength(char[] value) {
        PasswordStrength result = context.strength().evaluate(value);
        Arrays.fill(value, '\0');
        strengthLabel.setText(result.level().label() + "  ·  " + result.feedback().getFirst());
        String color = switch (result.level()) {
            case WEAK -> "#e58a88";
            case MEDIUM -> "#e7bd70";
            case STRONG -> "#83d7a0";
            case VERY_STRONG -> "#60c885";
        };
        strengthLabel.setTextFill(Color.web(color));
        strengthBar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4; -fx-pref-width: " + (100 + result.score() * 18) + "px;");
    }

    private void createVault() {
        char[] first = master.getText().toCharArray();
        char[] second = confirm.getText().toCharArray();
        if (!Arrays.equals(first, second)) {
            Arrays.fill(first, '\0'); Arrays.fill(second, '\0');
            error.setText("Those passwords do not match. Check both fields and try again.");
            confirm.requestFocus();
            return;
        }
        Arrays.fill(second, '\0');
        setBusy(true, "Creating your vault…");
        tasks.submit(() -> { context.authentication().initialize(first); return null; },
                ignored -> { master.clear(); confirm.clear(); onUnlocked.run(); }, this::showFailure);
    }

    private void unlockVault() {
        char[] password = master.getText().toCharArray();
        if (password.length == 0) {
            error.setText("Enter your master password to continue.");
            return;
        }
        setBusy(true, "Unlocking your vault…");
        tasks.submit(() -> { context.authentication().unlock(password); return null; },
                ignored -> { master.clear(); onUnlocked.run(); }, this::showFailure);
    }

    private void setBusy(boolean busy, String text) {
        master.setDisable(busy);
        confirm.setDisable(busy);
        submit.setDisable(busy);
        submit.setText(text);
        error.setText("");
    }

    private void showFailure(Throwable failure) {
        setBusy(false, context.authentication().isConfigured() ? "Unlock vault" : "Create secure vault");
        String message = failure.getMessage();
        error.setText(message == null || message.isBlank() ? "The vault could not be opened. Try again." : message);
        master.clear();
        confirm.clear();
        master.requestFocus();
    }
}
