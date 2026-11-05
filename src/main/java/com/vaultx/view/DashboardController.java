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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class DashboardController implements ScreenController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault());
    private final AppContext context;
    private final BackgroundTasks tasks;
    private final Runnable onAdd;
    private final VBox root = new VBox();
    private final HBox metrics = new HBox(13);
    private final VBox recent = new VBox(4);
    private final VBox health = new VBox(12);
    private final Label state = Ui.label("Loading your vault summary…", "text-muted");
    private List<Credential> loaded = List.of();
    private boolean disposed;

    public DashboardController(AppContext context, BackgroundTasks tasks, Runnable onAdd) {
        this.context = context;
        this.tasks = tasks;
        this.onAdd = onAdd;
        build();
        load();
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
        VBox page = Ui.page("Your vault, at a glance", "A calm view of the accounts you keep protected.");
        HBox header = Ui.row(12, new VBox(5,
                Ui.label("Vault overview", "section-title"), Ui.label("Only account details and security summaries appear here.", "text-muted")), Ui.spacer());
        metrics.setFillHeight(true);
        VBox metricsCard = new VBox(14, Ui.label("Stored credentials", "metric-label"), metrics);
        metricsCard.getStyleClass().add("card");
        HBox lower = new HBox(16);
        VBox recentCard = new VBox(13, Ui.label("Recently added", "section-title"), state, recent);
        recentCard.getStyleClass().add("card");
        VBox.setVgrow(recent, Priority.ALWAYS);
        VBox healthCard = new VBox(14, Ui.label("Password health", "section-title"),
                Ui.label("An educational estimate across saved entries.", "credential-subtitle"), health,
                Ui.label("This is not a formal security audit.", "text-subtle"));
        healthCard.getStyleClass().add("card");
        lower.getChildren().addAll(recentCard, healthCard);
        HBox.setHgrow(recentCard, Priority.ALWAYS);
        HBox.setHgrow(healthCard, Priority.ALWAYS);
        page.getChildren().addAll(header, metricsCard, lower);
        root.getChildren().add(page);
        VBox.setVgrow(page, Priority.ALWAYS);
    }

    private void load() {
        tasks.submit(context.credentials()::findAll, credentials -> {
            if (disposed) { credentials.forEach(Credential::close); return; }
            loaded = credentials;
            updateSummary(credentials);
        }, failure -> { if (!disposed) state.setText("Vault summary could not be loaded. " + safeMessage(failure)); });
    }

    private void updateSummary(List<Credential> credentials) {
        int favorites = (int) credentials.stream().filter(Credential::favorite).count();
        long categoryCount = credentials.stream().map(Credential::category).distinct().count();
        metrics.getChildren().setAll(
                metric(credentials.size(), "TOTAL ENTRIES", "Stored in this vault", "#f0f4f2"),
                metric(favorites, "FAVORITES", "Pinned for quick access", "#83d7a0"),
                metric(categoryCount, "CATEGORIES", "Used to organize entries", "#c3d0cb"));

        List<Credential> recentEntries = credentials.stream()
                .sorted(Comparator.comparing(Credential::createdAt).reversed()).limit(5).toList();
        recent.getChildren().clear();
        if (recentEntries.isEmpty()) {
            state.setText("Your vault is ready for its first entry.");
            var add = Ui.button("Add a credential", "button-primary");
            add.setOnAction(event -> onAdd.run());
            recent.getChildren().add(add);
        } else {
            state.setText("Most recently added accounts");
            for (Credential credential : recentEntries) {
                HBox line = new HBox(12,
                        new VBox(4, Ui.label(credential.name(), "credential-name"),
                                Ui.label(credential.username().isBlank() ? credential.category() : credential.username(), "credential-subtitle")),
                        Ui.spacer(), Ui.label(DATE.format(credential.createdAt()), "text-subtle"));
                line.setAlignment(Pos.CENTER_LEFT);
                line.getStyleClass().add("credential-row");
                recent.getChildren().add(line);
            }
        }

        int[] counts = new int[StrengthLevel.values().length];
        for (Credential credential : credentials) {
            char[] password = credential.password();
            try {
                PasswordStrength result = context.strength().evaluate(password);
                counts[result.level().ordinal()]++;
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        }
        health.getChildren().setAll(
                strengthLine(StrengthLevel.VERY_STRONG, counts[3], credentials.size()),
                strengthLine(StrengthLevel.STRONG, counts[2], credentials.size()),
                strengthLine(StrengthLevel.MEDIUM, counts[1], credentials.size()),
                strengthLine(StrengthLevel.WEAK, counts[0], credentials.size()));
        credentials.forEach(Credential::close);
        loaded = List.of();
    }

    private VBox metric(long number, String label, String detail, String valueColor) {
        Label value = Ui.label(Long.toString(number), "metric-value");
        value.setTextFill(Color.web(valueColor));
        VBox card = new VBox(7, value, Ui.label(label, "metric-label"), Ui.label(detail, "metric-detail"));
        card.setPadding(new javafx.geometry.Insets(4, 12, 5, 12));
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox strengthLine(StrengthLevel level, int count, int total) {
        String color = switch (level) {
            case VERY_STRONG, STRONG -> "#83d7a0";
            case MEDIUM -> "#e7bd70";
            case WEAK -> "#e58a88";
        };
        Region track = new Region(); track.getStyleClass().add("progress-track"); track.setMinHeight(5); track.setMaxHeight(5);
        Region fill = new Region(); fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");
        fill.setMinHeight(5); fill.setMaxHeight(5);
        HBox bar = new HBox(fill);
        HBox.setHgrow(track, Priority.ALWAYS);
        if (total > 0) fill.prefWidthProperty().bind(track.widthProperty().multiply((double) count / total));
        else fill.setPrefWidth(0);
        StackBox stacked = new StackBox(track, bar);
        HBox row = new HBox(10, Ui.label(level.label(), "metric-label"), stacked, Ui.spacer(), Ui.label(Integer.toString(count), "metric-label"));
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(stacked, Priority.ALWAYS);
        return row;
    }

    private static final class StackBox extends javafx.scene.layout.StackPane {
        StackBox(Node track, Node bar) { getChildren().addAll(track, bar); }
    }

    private static String safeMessage(Throwable failure) {
        return failure == null || failure.getMessage() == null ? "Try opening the screen again." : failure.getMessage();
    }
}
