package com.vaultx.view;

import com.vaultx.model.Credential;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class VaultController implements ScreenController {
    private static final List<String> CATEGORIES = List.of("All categories", "Social Media", "Education", "Finance", "Work", "Gaming", "Other");
    private final AppContext context;
    private final BackgroundTasks tasks;
    private final Runnable onChanged;
    private final VBox root = new VBox(18);
    private final ObservableList<Credential> entries = FXCollections.observableArrayList();
    private final FilteredList<Credential> filtered = new FilteredList<>(entries, item -> true);
    private final TableView<Credential> table = new TableView<>();
    private final ComboBox<String> category = new ComboBox<>();
    private final CheckBox favoritesOnly = new CheckBox("Favorites");
    private final Label resultCount = Ui.label("Loading entries…", "text-subtle");
    private final VBox detail = new VBox(14);
    private final PasswordField hiddenPassword = new PasswordField();
    private final TextField visiblePassword = new TextField();
    private final ToggleButton revealPassword = new ToggleButton("Show");
    private final Label detailTitle = Ui.label("Select a credential", "section-title");
    private Credential selected;
    private String query = "";
    private boolean disposed;

    public VaultController(AppContext context, BackgroundTasks tasks, Runnable onChanged) {
        this.context = context;
        this.tasks = tasks;
        this.onChanged = onChanged;
        build();
        reload();
    }

    @Override public Node view() { return root; }

    @Override public void dispose() {
        disposed = true;
        entries.forEach(Credential::close);
        entries.clear();
        selected = null;
        hiddenPassword.clear();
        visiblePassword.clear();
        detail.getChildren().clear();
    }

    public void setSearchText(String text) {
        query = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
        updateFilter();
    }

    public void showAddDialog() {
        CredentialEditorDialog.show(root.getScene().getWindow(), null).ifPresent(this::saveNew);
    }

    private void build() {
        root.getStyleClass().add("page-root");
        VBox page = Ui.page("Password vault", "Search, organize, and manage your saved accounts.");
        HBox toolbar = new HBox(11);
        category.getItems().setAll(CATEGORIES); category.setValue(CATEGORIES.getFirst());
        category.setPrefWidth(170);
        category.valueProperty().addListener((observable, oldValue, newValue) -> updateFilter());
        favoritesOnly.selectedProperty().addListener((observable, oldValue, newValue) -> updateFilter());
        Button add = Ui.button("Add credential", "button-primary");
        add.setGraphic(IconFactory.create("plus", 15, Color.web("#102116")));
        add.setOnAction(event -> showAddDialog());
        toolbar.getChildren().addAll(category, favoritesOnly, Ui.spacer(), resultCount, add);

        configureTable();
        detail.getStyleClass().add("card");
        detail.setPadding(new Insets(20));
        detail.setMinWidth(225);
        detail.setPrefWidth(265);
        setEmptyDetail();

        BorderPane listPane = new BorderPane();
        listPane.setCenter(table);
        listPane.setPadding(new Insets(15));
        listPane.getStyleClass().add("card");
        table.setMinWidth(300);
        HBox body = new HBox(14, listPane, detail);
        HBox.setHgrow(listPane, Priority.ALWAYS);
        HBox.setHgrow(table, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        page.getChildren().addAll(toolbar, body);
        VBox.setVgrow(page, Priority.ALWAYS);
        root.getChildren().add(page);
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(Ui.empty("No accounts match this view. Try another search or add a credential."));
        TableColumn<Credential, Credential> account = new TableColumn<>("Account");
        account.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        account.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Credential item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                VBox labels = new VBox(4, Ui.label(item.name(), "credential-name"),
                        Ui.label(item.url().isBlank() ? item.username() : item.url(), "credential-subtitle"));
                setGraphic(labels); setText(null);
                setPadding(new Insets(8, 4, 8, 4));
            }
        });
        TableColumn<Credential, String> username = new TableColumn<>("USERNAME");
        username.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().username()));
        username.setPrefWidth(125);
        TableColumn<Credential, String> categoryColumn = new TableColumn<>("CATEGORY");
        categoryColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().category()));
        categoryColumn.setPrefWidth(90);
        TableColumn<Credential, Credential> favorite = new TableColumn<>("★");
        favorite.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        favorite.setPrefWidth(38); favorite.setMaxWidth(48);
        favorite.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(Credential item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Button pin = Ui.iconButton("star", item.favorite() ? "Remove from favorites" : "Add to favorites");
                pin.setGraphic(IconFactory.create("star", 15, Color.web(item.favorite() ? "#83d7a0" : "#778580")));
                pin.setOnAction(event -> toggleFavorite(item));
                setGraphic(pin);
            }
        });
        table.getColumns().setAll(account, username, categoryColumn, favorite);
        SortedList<Credential> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldItem, newItem) -> showDetails(newItem));
        table.setRowFactory(view -> {
            var row = new javafx.scene.control.TableRow<Credential>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) edit(row.getItem());
            });
            return row;
        });
    }

    private void reload() {
        resultCount.setText("Loading entries…");
        tasks.submit(context.credentials()::findAll, result -> {
            if (disposed) { result.forEach(Credential::close); return; }
            entries.forEach(Credential::close);
            entries.setAll(result);
            updateFilter();
            if (!entries.isEmpty() && selected != null) {
                Credential replacement = entries.stream().filter(item -> item.id().equals(selected.id())).findFirst().orElse(null);
                if (replacement == null) setEmptyDetail(); else table.getSelectionModel().select(replacement);
            } else if (entries.isEmpty()) setEmptyDetail();
        }, failure -> { if (!disposed) resultCount.setText(failure.getMessage() == null ? "Entries could not be loaded." : failure.getMessage()); });
    }

    private void updateFilter() {
        String chosenCategory = category.getValue();
        filtered.setPredicate(item -> {
            boolean categoryMatches = chosenCategory == null || chosenCategory.equals(CATEGORIES.getFirst()) || item.category().equals(chosenCategory);
            boolean favoriteMatches = !favoritesOnly.isSelected() || item.favorite();
            boolean queryMatches = query.isBlank() || item.name().toLowerCase(Locale.ROOT).contains(query)
                    || item.url().toLowerCase(Locale.ROOT).contains(query)
                    || item.username().toLowerCase(Locale.ROOT).contains(query)
                    || item.category().toLowerCase(Locale.ROOT).contains(query);
            return categoryMatches && favoriteMatches && queryMatches;
        });
        resultCount.setText(filtered.size() + (filtered.size() == 1 ? " entry" : " entries"));
    }

    private void showDetails(Credential credential) {
        selected = credential;
        detail.getChildren().clear();
        if (credential == null) { setEmptyDetail(); return; }
        detailTitle.setText(credential.name());
        Label url = Ui.label(credential.url().isBlank() ? credential.category() : credential.url(), "credential-subtitle");
        url.setWrapText(true);
        HBox titleRow = Ui.row(8, new VBox(6, detailTitle, url), Ui.spacer());
        Button edit = Ui.iconButton("edit", "Edit credential"); edit.setOnAction(event -> edit(credential));
        Button remove = Ui.iconButton("trash", "Delete credential"); remove.setOnAction(event -> delete(credential));
        titleRow.getChildren().addAll(edit, remove);
        detail.getChildren().addAll(titleRow, separator());

        detail.getChildren().addAll(Ui.label("USERNAME", "table-head"));
        TextField username = new TextField(credential.username()); username.setEditable(false);
        Button copyUser = Ui.iconButton("copy", "Copy username"); copyUser.setOnAction(event -> {
            context.clipboard().copy(credential.username());
            copyUser.setTooltip(new javafx.scene.control.Tooltip("Copied"));
        });
        detail.getChildren().add(Ui.row(7, username, copyUser));

        detail.getChildren().add(Ui.label("PASSWORD", "table-head"));
        char[] passwordChars = credential.password();
        String passwordText = new String(passwordChars);
        Arrays.fill(passwordChars, '\0');
        hiddenPassword.setText(passwordText);
        visiblePassword.setText(passwordText);
        hiddenPassword.setEditable(false); visiblePassword.setEditable(false);
        hiddenPassword.setPromptText("Stored securely"); visiblePassword.setPromptText("Stored securely");
        revealPassword.getStyleClass().add("button-quiet");
        revealPassword.setSelected(false);
        revealPassword.setOnAction(event -> setPasswordVisible(revealPassword.isSelected()));
        visiblePassword.setVisible(false); visiblePassword.setManaged(false);
        Button copyPassword = Ui.iconButton("copy", "Copy password");
        copyPassword.setOnAction(event -> {
            context.clipboard().copy(credential.password());
            copyPassword.setTooltip(new javafx.scene.control.Tooltip("Copied; clipboard clears automatically"));
        });
        StackPane passwordStack = new StackPane(hiddenPassword, visiblePassword);
        HBox passwordActions = Ui.row(6, revealPassword, copyPassword);
        revealPassword.setMinWidth(58);
        revealPassword.setPadding(new Insets(7, 8, 7, 8));
        detail.getChildren().addAll(passwordStack, passwordActions);
        detail.getChildren().add(separator());
        detail.getChildren().add(Ui.label("NOTES", "table-head"));
        TextArea notes = new TextArea(credential.notes().isBlank() ? "No notes" : credential.notes());
        notes.setEditable(false); notes.setWrapText(true); notes.setPrefRowCount(4);
        detail.getChildren().add(notes);
        Button favorite = Ui.button(credential.favorite() ? "Remove favorite" : "Add to favorites", "button-quiet");
        favorite.setGraphic(IconFactory.create("star", 14, Color.web(credential.favorite() ? "#83d7a0" : "#a5b2ae")));
        favorite.setOnAction(event -> toggleFavorite(credential));
        detail.getChildren().add(favorite);
    }

    private void setPasswordVisible(boolean visible) {
        if (visible) {
            visiblePassword.setText(hiddenPassword.getText()); visiblePassword.setManaged(true); visiblePassword.setVisible(true);
            hiddenPassword.setManaged(false); hiddenPassword.setVisible(false); revealPassword.setText("Hide");
        } else {
            hiddenPassword.setText(visiblePassword.getText()); hiddenPassword.setManaged(true); hiddenPassword.setVisible(true);
            visiblePassword.setManaged(false); visiblePassword.setVisible(false); revealPassword.setText("Show");
        }
    }

    private void setEmptyDetail() {
        selected = null;
        detail.getChildren().setAll(IconFactory.create("key", 22, Ui.GREEN), detailTitle,
                Ui.empty("Choose an account to view its details, copy a username, or reveal its password."));
        detailTitle.setText("Credential details");
    }

    private void saveNew(Credential credential) {
        tasks.submit(() -> context.credentials().add(credential), () -> {
            credential.close();
            if (!disposed) { reload(); onChanged.run(); }
        }, failure -> { credential.close(); if (!disposed) Dialogs.error(root.getScene().getWindow(), "Could not save credential", message(failure)); });
    }

    private void edit(Credential credential) {
        CredentialEditorDialog.show(root.getScene().getWindow(), credential).ifPresent(updated -> tasks.submit(
                () -> context.credentials().update(updated), () -> { updated.close(); if (!disposed) { reload(); onChanged.run(); } },
                failure -> { updated.close(); if (!disposed) Dialogs.error(root.getScene().getWindow(), "Could not update credential", message(failure)); }));
    }

    private void toggleFavorite(Credential credential) {
        char[] password = credential.password();
        Credential updated = credential.edited(credential.name(), credential.url(), credential.username(), password,
                credential.category(), credential.notes(), !credential.favorite());
        Arrays.fill(password, '\0');
        tasks.submit(() -> context.credentials().update(updated), () -> { updated.close(); if (!disposed) { reload(); onChanged.run(); } },
                failure -> { updated.close(); if (!disposed) Dialogs.error(root.getScene().getWindow(), "Could not update favorite", message(failure)); });
    }

    private void delete(Credential credential) {
        Window owner = root.getScene().getWindow();
        if (!Dialogs.confirm(owner, "Delete credential", "Permanently delete “" + credential.name() + "” from this vault?", "Delete")) return;
        UUID id = credential.id();
        tasks.submit(() -> context.credentials().delete(id), () -> { if (!disposed) { setEmptyDetail(); reload(); onChanged.run(); } },
                failure -> { if (!disposed) Dialogs.error(owner, "Could not delete credential", message(failure)); });
    }

    private static Region separator() {
        Region line = new Region(); line.setMinHeight(1); line.setMaxHeight(1); line.getStyleClass().add("divider"); return line;
    }

    private static String message(Throwable failure) {
        return failure == null || failure.getMessage() == null ? "Try again." : failure.getMessage();
    }
}
