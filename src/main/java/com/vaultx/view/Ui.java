package com.vaultx.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public final class Ui {
    public static final Color GREEN = Color.web("#83d7a0");
    public static final Color MUTED = Color.web("#a5b2ae");

    private Ui() { }

    public static Label label(String text, String... classes) {
        Label label = new Label(text);
        label.getStyleClass().addAll(classes);
        return label;
    }

    public static VBox page(String title, String description) {
        VBox page = new VBox(22);
        page.getStyleClass().add("page-content");
        Label heading = label(title, "page-title");
        Label summary = label(description, "page-description");
        heading.setWrapText(true); summary.setWrapText(true);
        heading.setMaxWidth(Double.MAX_VALUE); summary.setMaxWidth(Double.MAX_VALUE);
        page.getChildren().addAll(heading, summary);
        return page;
    }

    public static VBox card(Node... children) {
        VBox box = new VBox(14, children);
        box.getStyleClass().add("card");
        return box;
    }

    public static HBox row(double gap, Node... children) {
        HBox box = new HBox(gap, children);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, javafx.scene.layout.Priority.ALWAYS);
        return region;
    }

    public static Button button(String text, String... classes) {
        Button button = new Button(text);
        button.getStyleClass().addAll(classes);
        return button;
    }

    public static Button iconButton(String icon, String accessibleText) {
        Button button = new Button();
        button.setGraphic(IconFactory.create(icon, 16, Color.web("#a5b2ae")));
        button.setAccessibleText(accessibleText);
        button.setTooltip(new javafx.scene.control.Tooltip(accessibleText));
        button.getStyleClass().add("button-quiet");
        button.setPadding(new Insets(8));
        return button;
    }

    public static Label empty(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("text-muted");
        return label;
    }
}
