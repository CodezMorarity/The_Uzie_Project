package com.vaultx.view;

import com.vaultx.model.Credential;
import com.vaultx.model.PasswordStrength;
import com.vaultx.model.StrengthLevel;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.List;

public final class SecurityController implements ScreenController {
    private final AppContext context;
    private final BackgroundTasks tasks;
    private final VBox root = new VBox();
    private final VBox distribution = new VBox(12);
    private final Label summary = Ui.label("Loading security overview…", "text-muted");
    private List<Credential> loaded = List.of();
    private boolean disposed;

    public SecurityController(AppContext context, BackgroundTasks tasks) {
        this.context = context; this.tasks = tasks; build(); load();
    }

    @Override public Node view() { return root; }

    @Override public void dispose() {
        disposed = true;
        loaded.forEach(Credential::close);
        loaded = List.of();
        root.getChildren().clear();
    }

    private void build() {
        root.getStyleClass().add("page-root");
        VBox page = Ui.page("Security overview", "See how your stored passwords rate against VaultX’s local educational checks.");
        VBox health = new VBox(15, Ui.label("Password strength distribution", "section-title"),
                Ui.label("VaultX checks length, character variety, common passwords, simple sequences, and repetition.", "credential-subtitle"), summary, distribution);
        health.getStyleClass().add("card");
        VBox architecture = new VBox(15, Ui.label("How the vault protects data", "section-title"),
                row("At rest", "AES-256-GCM, with a fresh 96-bit nonce per encryption"),
                row("Master password", "PBKDF2WithHmacSHA256 · 600,000 iterations · unique 128-bit salt"),
                row("Vault key", "Derived at unlock and held in memory only while the vault is open"),
                row("Database", "Sensitive credential fields are encrypted before they reach SQLite"));
        architecture.getStyleClass().add("card");
        VBox limits = new VBox(8, Ui.label("What this overview can’t tell you", "section-title"),
                Ui.label("The strength labels are an educational estimator, not a formal security audit. VaultX cannot protect an unlocked computer from malware, keyloggers, or someone with access to the running device. Java garbage collection does not guarantee complete memory erasure.", "page-description"));
        limits.getStyleClass().add("card");
        limits.getChildren().getLast().setStyle("-fx-wrap-text: true;");
        page.getChildren().addAll(health, architecture, limits);
        root.getChildren().add(page);
    }

    private Node row(String title, String detail) {
        Label value = Ui.label(detail, "credential-subtitle");
        value.setWrapText(true);
        HBox row = Ui.row(12, Ui.label(title, "metric-label"), value);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(value, Priority.ALWAYS);
        value.setStyle("-fx-text-alignment: right;");
        return row;
    }

    private void load() {
        tasks.submit(context.credentials()::findAll, credentials -> {
            if (disposed) { credentials.forEach(Credential::close); return; }
            loaded = credentials;
            int[] counts = new int[4];
            for (Credential credential : credentials) {
                char[] password = credential.password();
                try { counts[context.strength().evaluate(password).level().ordinal()]++; }
                finally { Arrays.fill(password, '\0'); }
            }
            int total = credentials.size();
            summary.setText(total == 0 ? "Add credentials to see an overview." : total + (total == 1 ? " stored password" : " stored passwords"));
            distribution.getChildren().setAll(
                    line(StrengthLevel.VERY_STRONG, counts[3], total),
                    line(StrengthLevel.STRONG, counts[2], total),
                    line(StrengthLevel.MEDIUM, counts[1], total),
                    line(StrengthLevel.WEAK, counts[0], total));
            credentials.forEach(Credential::close);
            loaded = List.of();
        }, failure -> { if (!disposed) summary.setText(failure.getMessage() == null ? "Security overview could not be loaded." : failure.getMessage()); });
    }

    private HBox line(StrengthLevel level, int count, int total) {
        String color = switch (level) {
            case VERY_STRONG, STRONG -> "#83d7a0";
            case MEDIUM -> "#e7bd70";
            case WEAK -> "#e58a88";
        };
        Region track = new Region(); track.setMinHeight(6); track.setMaxHeight(6); track.getStyleClass().add("progress-track");
        Region fill = new Region(); fill.setMinHeight(6); fill.setMaxHeight(6);
        fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");
        HBox barContent = new HBox(fill);
        javafx.scene.layout.StackPane bar = new javafx.scene.layout.StackPane(track, barContent);
        HBox.setHgrow(bar, Priority.ALWAYS);
        if (total > 0) fill.prefWidthProperty().bind(track.widthProperty().multiply((double) count / total));
        else fill.setPrefWidth(0);
        HBox row = Ui.row(12, Ui.label(level.label(), "metric-label"), bar, Ui.spacer(), Ui.label(count + "  /  " + total, "metric-label"));
        HBox.setHgrow(bar, Priority.ALWAYS);
        return row;
    }
}
