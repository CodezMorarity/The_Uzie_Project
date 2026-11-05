package com.vaultx.view;

import com.vaultx.model.AppSettings;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.function.Consumer;

public final class MainShell implements ScreenController {
    private final AppContext context;
    private final BackgroundTasks tasks;
    private final Consumer<Boolean> onLock;
    private final Runnable onExit;
    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final TextField globalSearch = new TextField();
    private final Label pageTitle = Ui.label("Dashboard", "topbar-title");
    private final Timeline lockTimer;
    private ScreenController active;
    private boolean disposed;

    public MainShell(AppContext context, BackgroundTasks tasks, Consumer<Boolean> onLock, Runnable onExit) {
        this.context = context; this.tasks = tasks; this.onLock = onLock; this.onExit = onExit;
        build();
        route(NavigationSection.DASHBOARD);
        lockTimer = new Timeline(new KeyFrame(Duration.seconds(8), event -> checkInactivity()));
        lockTimer.setCycleCount(Timeline.INDEFINITE); lockTimer.play();
    }

    @Override public Node view() { return root; }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        lockTimer.stop();
        if (active != null) active.dispose();
    }

    private void build() {
        root.getStyleClass().add("page-root");
        root.setLeft(buildSidebar());
        root.setTop(buildTopbar());
        content.getStyleClass().add("page-root");
        root.setCenter(content);
        applyTheme();
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(5);
        sidebar.getStyleClass().add("sidebar");
        HBox brand = new HBox(10, IconFactory.create("shield", 30, Ui.GREEN),
                new VBox(3, Ui.label("VaultX", "brand-name"), Ui.label("OFFLINE VAULT", "brand-tagline")));
        brand.setAlignment(Pos.CENTER_LEFT); brand.getStyleClass().add("brand-row");
        sidebar.getChildren().addAll(brand, Ui.label("WORKSPACE", "nav-label"));
        for (NavigationSection item : NavigationSection.values()) {
            Button button = Ui.button(item.title(), "nav-button");
            button.setGraphic(IconFactory.create(item.icon(), 17, Color.web("#a5b2ae")));
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> route(item));
            sidebar.getChildren().add(button);
        }
        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox footer = new VBox(7, Ui.label("LOCAL VAULT", "nav-label"), Ui.label("Encrypted on this device", "credential-subtitle"), Ui.label("Works offline", "status-good"));
        footer.getStyleClass().add("sidebar-footer");
        sidebar.getChildren().addAll(spacer, footer);
        sidebar.setPrefWidth(225); sidebar.setMinWidth(205); sidebar.setMaxWidth(235);
        return sidebar;
    }

    private HBox buildTopbar() {
        HBox topbar = new HBox(15);
        topbar.getStyleClass().add("topbar");
        pageTitle.setMinWidth(125);
        HBox search = new HBox(8, IconFactory.create("search", 16, Color.web("#a5b2ae")), globalSearch);
        search.getStyleClass().add("search-box");
        HBox.setHgrow(globalSearch, Priority.ALWAYS);
        HBox.setHgrow(search, Priority.ALWAYS);
        globalSearch.setPromptText("Search accounts, usernames, or categories");
        globalSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            if (active instanceof VaultController vault) vault.setSearchText(newValue);
        });
        globalSearch.setOnAction(event -> route(NavigationSection.VAULT));
        Button add = Ui.button("New credential", "button-primary");
        add.setGraphic(IconFactory.create("plus", 14, Color.web("#102116")));
        add.setOnAction(event -> {
            route(NavigationSection.VAULT);
            javafx.application.Platform.runLater(() -> { if (active instanceof VaultController vault) vault.showAddDialog(); });
        });
        Button lock = Ui.iconButton("lock", "Lock vault");
        lock.setOnAction(event -> lockNow());
        topbar.getChildren().addAll(pageTitle, search, add, lock);
        return topbar;
    }

    private void route(NavigationSection next) {
        if (active != null) active.dispose();
        pageTitle.setText(next.title());
        for (Node node : ((VBox) root.getLeft()).getChildren()) {
            if (node instanceof Button button && button.getText().equals(next.title())) {
                button.getStyleClass().add("nav-button-active");
                if (button.getGraphic() instanceof javafx.scene.shape.SVGPath icon) icon.setFill(Ui.GREEN);
            } else if (node instanceof Button button) {
                button.getStyleClass().remove("nav-button-active");
                if (button.getGraphic() instanceof javafx.scene.shape.SVGPath icon) icon.setFill(Color.web("#a5b2ae"));
            }
        }
        active = switch (next) {
            case DASHBOARD -> new DashboardController(context, tasks, () -> route(NavigationSection.VAULT));
            case VAULT -> new VaultController(context, tasks, () -> { });
            case GENERATOR -> new GeneratorController(context);
            case SECURITY -> new SecurityController(context, tasks);
            case SETTINGS -> new SettingsController(context, tasks, this::lockNow, onExit, this::applyTheme);
        };
        if (active instanceof VaultController vault) vault.setSearchText(globalSearch.getText());
        content.getChildren().setAll(active.view());
    }

    private void applyTheme() {
        root.getStyleClass().remove("theme-light");
        if (context.currentSettings().theme() == AppSettings.Theme.LIGHT) root.getStyleClass().add("theme-light");
    }

    private void checkInactivity() {
        if (context.session().hasExpired(context.currentSettings().autoLockMinutes())) lockNow();
    }

    private void lockNow() {
        if (disposed) return;
        dispose();
        context.clipboard().clearOwned();
        context.authentication().lock();
        onLock.accept(true);
    }
}
