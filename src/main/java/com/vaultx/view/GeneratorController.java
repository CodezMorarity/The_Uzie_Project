package com.vaultx.view;

import com.vaultx.model.PasswordStrength;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GeneratorController implements ScreenController {
    private final AppContext context;
    private final VBox root = new VBox();
    private final Slider length = new Slider(8, 64, 20);
    private final Label lengthLabel = Ui.label("20 characters", "metric-value");
    private final CheckBox uppercase = new CheckBox("Uppercase letters");
    private final CheckBox lowercase = new CheckBox("Lowercase letters");
    private final CheckBox numbers = new CheckBox("Numbers");
    private final CheckBox symbols = new CheckBox("Symbols");
    private final CheckBox exclude = new CheckBox("Exclude ambiguous characters");
    private final VBox results = new VBox(8);
    private final Label error = Ui.label("", "auth-error");
    private final List<TextField> generatedFields = new ArrayList<>();

    public GeneratorController(AppContext context) {
        this.context = context;
        build();
    }

    @Override public Node view() { return root; }

    @Override public void dispose() {
        generatedFields.forEach(TextField::clear);
        generatedFields.clear();
        results.getChildren().clear();
    }

    private void build() {
        root.getStyleClass().add("page-root");
        VBox page = Ui.page("Password generator", "Create a strong password tailored to the account you are adding.");
        length.setShowTickLabels(true); length.setShowTickMarks(true); length.setMajorTickUnit(14); length.setBlockIncrement(1);
        length.valueProperty().addListener((observable, oldValue, newValue) -> lengthLabel.setText(Math.round(newValue.doubleValue()) + " characters"));
        uppercase.setSelected(true); lowercase.setSelected(true); numbers.setSelected(true); symbols.setSelected(true); exclude.setSelected(true);
        javafx.scene.control.Label explanation = Ui.label("Generated passwords use a cryptographically secure random source and include at least one character from every selected type.", "credential-subtitle");
        explanation.setWrapText(true);
        VBox controls = new VBox(19,
                Ui.label("Password options", "section-title"),
                new VBox(9, Ui.label("LENGTH", "table-head"), lengthLabel, length),
                new HBox(26, new VBox(12, uppercase, lowercase), new VBox(12, numbers, symbols)),
                exclude,
                explanation);
        controls.getStyleClass().add("card");
        var generate = Ui.button("Generate 5 passwords", "button-primary");
        generate.setGraphic(IconFactory.create("generator", 15, Color.web("#102116")));
        generate.setOnAction(event -> generate());
        HBox actionRow = Ui.row(12, generate, Ui.spacer(), error);
        VBox output = new VBox(14, Ui.label("Choose a password", "section-title"),
                Ui.label("Copy one to the clipboard or use it in a new vault entry.", "credential-subtitle"), results);
        output.getStyleClass().add("card");
        results.getChildren().add(Ui.empty("Generate a set to see five password options here."));
        page.getChildren().addAll(controls, actionRow, output);
        root.getChildren().add(page);
    }

    private void generate() {
        generatedFields.forEach(TextField::clear);
        generatedFields.clear();
        try {
            int size = (int) Math.round(length.getValue());
            List<char[]> generated = context.generator().generate(size, uppercase.isSelected(), lowercase.isSelected(),
                    numbers.isSelected(), symbols.isSelected(), exclude.isSelected(), 5);
            results.getChildren().clear();
            for (int i = 0; i < generated.size(); i++) {
                char[] password = generated.get(i);
                String text = new String(password);
                Arrays.fill(password, '\0');
                addGeneratedRow(i + 1, text);
            }
            error.setText("");
        } catch (IllegalArgumentException ex) {
            error.setText(ex.getMessage());
        }
    }

    private void addGeneratedRow(int index, String password) {
        TextField value = new TextField(password);
        value.setEditable(false);
        value.setStyle("-fx-font-family: 'Cascadia Code', 'Consolas', monospace; -fx-font-size: 13px;");
        generatedFields.add(value);
        char[] strengthInput = password.toCharArray();
        PasswordStrength strength;
        try { strength = context.strength().evaluate(strengthInput); }
        finally { Arrays.fill(strengthInput, '\0'); }
        Label state = Ui.label(strength.level().label(), "strength-label");
        state.setTextFill(Color.web(strength.level() == com.vaultx.model.StrengthLevel.WEAK ? "#e58a88" : "#83d7a0"));
        var copy = Ui.iconButton("copy", "Copy generated password");
        copy.setOnAction(event -> {
            context.clipboard().copy(value.getText());
            copy.setTooltip(new javafx.scene.control.Tooltip("Copied; clipboard clears automatically"));
        });
        HBox row = Ui.row(12, Ui.label(String.format("%02d", index), "text-subtle"), value, state, copy);
        HBox.setHgrow(value, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("credential-row");
        results.getChildren().add(row);
    }
}
