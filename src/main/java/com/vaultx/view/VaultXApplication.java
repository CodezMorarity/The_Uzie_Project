package com.vaultx.view;

import com.vaultx.database.DatabaseInitializer;
import com.vaultx.database.DatabaseManager;
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
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public final class VaultXApplication extends Application {
    private BackgroundTasks tasks;
    private AppContext context;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        DatabaseManager database = new DatabaseManager();
        new DatabaseInitializer(database).initialize();
        SecureRandomProvider random = new SecureRandomProvider();
        SessionManager session = new SessionManager();
        EncryptionService encryption = new EncryptionService(random);
        CredentialRepository credentialRepository = new CredentialRepository(database);
        VaultRepository vaultRepository = new VaultRepository(database);
        SettingsRepository settingsRepository = new SettingsRepository(database);
        CredentialCodec codec = new CredentialCodec();
        PasswordStrengthService strength = new PasswordStrengthService();
        tasks = new BackgroundTasks();
        ClipboardManager clipboard = new ClipboardManager(30);
        context = new AppContext(
                new AuthenticationService(vaultRepository, credentialRepository, new KeyDerivation(), encryption,
                        random, session, strength),
                new CredentialService(credentialRepository, encryption, codec, session),
                new PasswordGeneratorService(random), strength,
                new BackupService(codec, encryption, new KeyDerivation(), random, session, strength),
                new SettingsService(settingsRepository), session, clipboard);

        stage.setTitle("VaultX — Secure Password Manager");
        stage.setMinWidth(900);
        stage.setMinHeight(680);
        stage.setWidth(1240);
        stage.setHeight(800);
        InputStream icon = getClass().getResourceAsStream("/com/vaultx/brand/vaultx-mark.png");
        if (icon != null) stage.getIcons().add(new Image(icon));
        showAuth(false);
        stage.setOnCloseRequest(event -> shutdown());
        stage.show();
    }

    public void showAuth(boolean locked) {
        if (context.session().isUnlocked()) context.authentication().lock();
        context.clipboard().clearOwned();
        Scene scene = scene(new AuthController(context, tasks, this::showVault, locked).view());
        stage.setScene(scene);
    }

    private void showVault() {
        MainShell shell = new MainShell(context, tasks, this::showAuth, this::shutdown);
        Scene scene = scene(shell.view());
        scene.addEventFilter(javafx.scene.input.MouseEvent.ANY, event -> context.session().touch());
        scene.addEventFilter(javafx.scene.input.KeyEvent.ANY, event -> context.session().touch());
        stage.setScene(scene);
    }

    private Scene scene(javafx.scene.Node node) {
        if (!(node instanceof javafx.scene.Parent root)) throw new IllegalArgumentException("A screen root must be a JavaFX Parent.");
        Scene scene = new Scene(root, 1240, 800);
        scene.getStylesheets().add(getClass().getResource("/com/vaultx/styles.css").toExternalForm());
        if (context.currentSettings().theme() == com.vaultx.model.AppSettings.Theme.LIGHT) root.getStyleClass().add("theme-light");
        return scene;
    }

    private void shutdown() {
        if (context != null) {
            context.clipboard().close();
            context.authentication().lock();
        }
        if (tasks != null) tasks.close();
        Platform.exit();
    }

    @Override
    public void stop() {
        if (context != null) context.session().lock();
        if (context != null) context.clipboard().close();
        if (tasks != null) tasks.close();
    }
}
