package com.vaultx.view;

import com.vaultx.database.DatabaseInitializer;
import com.vaultx.database.DatabaseManager;
import com.vaultx.model.Credential;
import com.vaultx.repository.CredentialRepository;
import com.vaultx.repository.SettingsRepository;
import com.vaultx.repository.VaultRepository;
import com.vaultx.security.CredentialCodec;
import com.vaultx.security.EncryptionService;
import com.vaultx.security.KeyDerivation;
import com.vaultx.security.SecureRandomProvider;
import com.vaultx.security.SessionManager;
import com.vaultx.service.AuthenticationService;
import com.vaultx.service.BackupService;
import com.vaultx.service.CredentialService;
import com.vaultx.service.PasswordGeneratorService;
import com.vaultx.service.PasswordStrengthService;
import com.vaultx.service.SettingsService;
import com.vaultx.util.ClipboardManager;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Development-only capture of real VaultX JavaFX screens using fictional records. */
public final class ScreenshotCapture extends Application {
    private static final List<String> PAGES = List.of(
            "First run", "Dashboard", "Password Vault", "Password Generator", "Security Overview", "Settings");
    private Stage stage;
    private MainShell shell;
    private AppContext context;
    private BackgroundTasks tasks;
    private int pageIndex;
    private Path output;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        output = Path.of("docs", "screenshots").toAbsolutePath().normalize();
        Files.createDirectories(output);
        Path data = Files.createTempDirectory("vaultx-screenshot-data-");
        DatabaseManager database = new DatabaseManager(data.resolve("vaultx.db"));
        new DatabaseInitializer(database).initialize();
        SecureRandomProvider random = new SecureRandomProvider();
        SessionManager session = new SessionManager();
        EncryptionService encryption = new EncryptionService(random);
        CredentialRepository credentialRepository = new CredentialRepository(database);
        VaultRepository vaultRepository = new VaultRepository(database);
        SettingsRepository settingsRepository = new SettingsRepository(database);
        CredentialCodec codec = new CredentialCodec();
        PasswordStrengthService strength = new PasswordStrengthService();
        AuthenticationService authentication = new AuthenticationService(vaultRepository, credentialRepository,
                new KeyDerivation(), encryption, random, session, strength, 10_000);
        CredentialService credentials = new CredentialService(credentialRepository, encryption, codec, session);
        tasks = new BackgroundTasks();
        ClipboardManager clipboard = new ClipboardManager(30);
        context = new AppContext(authentication, credentials, new PasswordGeneratorService(random), strength,
                new BackupService(codec, encryption, new KeyDerivation(), random, session, strength, 10_000),
                new SettingsService(settingsRepository), session, clipboard);
        AuthController firstRun = new AuthController(context, tasks, this::showVaultScreens, false);
        Scene scene = new Scene((Parent) firstRun.view(), 1440, 920);
        scene.getStylesheets().add(getClass().getResource("/com/vaultx/styles.css").toExternalForm());
        stage.setTitle("VaultX — Screenshot capture");
        stage.setMinWidth(900);
        stage.setWidth(1440);
        stage.setHeight(920);
        stage.setScene(scene);
        stage.show();
        captureAfterRender();
    }

    private void showVaultScreens() {
        context.authentication().initialize("FictionalDemo#Vault248!".toCharArray());
        addDemo(context.credentials(), "Northstar Mail", "https://mail.example.test", "alex@example.test",
                "Fictional!Northstar947", "Work", 0, false);
        addDemo(context.credentials(), "Cedar Learning", "https://learn.example.test", "student@example.test",
                "Fictional&Cedar4827", "Education", 1, true);
        addDemo(context.credentials(), "Juniper Finance", "https://finance.example.test", "member@example.test",
                "Fictional$Juniper591", "Finance", 2, false);
        shell = new MainShell(context, tasks, ignored -> { }, () -> { });
        Scene scene = new Scene((Parent) shell.view(), 1440, 920);
        scene.getStylesheets().add(getClass().getResource("/com/vaultx/styles.css").toExternalForm());
        stage.setScene(scene);
    }

    private void captureAfterRender() {
        PauseTransition delay = new PauseTransition(Duration.millis(1100));
        delay.setOnFinished(event -> {
            try {
                if (pageIndex == 2) {
                    TableView<?> table = findTable(shell.view());
                    if (table != null) table.getSelectionModel().selectFirst();
                } else if (pageIndex == 3) {
                    Button generate = findButton(shell.view(), "Generate 5 passwords");
                    if (generate != null) generate.fire();
                    maskGeneratedValues(shell.view());
                }
                WritableImage image = stage.getScene().snapshot(null);
                savePng(image, output.resolve(slug(PAGES.get(pageIndex)) + ".png"));
                if (pageIndex == 0) {
                    pageIndex++;
                    showVaultScreens();
                    captureAfterRender();
                    return;
                }
                pageIndex++;
                if (pageIndex >= PAGES.size()) {
                    finish();
                    return;
                }
                Button navigation = findButton(shell.view(), PAGES.get(pageIndex));
                if (navigation == null) throw new IllegalStateException("Missing navigation button: " + PAGES.get(pageIndex));
                navigation.fire();
                navigation.requestFocus();
                captureAfterRender();
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                finish();
            }
        });
        delay.play();
    }

    private void finish() {
        shell.dispose();
        context.clipboard().close();
        context.session().lock();
        tasks.close();
        stage.close();
    }

    private static void addDemo(CredentialService service, String name, String url, String username,
                                String password, String category, int daysAgo, boolean favorite) {
        Instant created = Instant.now().minusSeconds(daysAgo * 86_400L);
        Credential credential = new Credential(UUID.randomUUID(), name, url, username, password.toCharArray(),
                category, "Fictional screenshot data", created, created, favorite);
        try { service.add(credential); }
        finally { credential.close(); }
    }

    private static Button findButton(Node node, String text) {
        if (node instanceof Button button && text.equals(button.getText())) return button;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Button button = findButton(child, text);
                if (button != null) return button;
            }
        }
        return null;
    }

    private static TableView<?> findTable(Node node) {
        if (node instanceof TableView<?> table) return table;
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TableView<?> table = findTable(child);
                if (table != null) return table;
            }
        }
        return null;
    }

    private static void maskGeneratedValues(Node node) {
        if (node instanceof TextField field && !field.isEditable() && !field.getText().isBlank()) {
            field.setText("•".repeat(field.getText().length()));
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) maskGeneratedValues(child);
        }
    }

    private static void savePng(WritableImage source, Path destination) throws Exception {
        int width = (int) source.getWidth();
        int height = (int) source.getHeight();
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) target.setRGB(x, y, source.getPixelReader().getArgb(x, y));
        }
        ImageIO.write(target, "png", destination.toFile());
    }

    private static String slug(String title) {
        return title.toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
    }
}
