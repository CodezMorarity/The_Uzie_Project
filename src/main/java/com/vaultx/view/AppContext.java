package com.vaultx.view;

import com.vaultx.model.AppSettings;
import com.vaultx.security.SessionManager;
import com.vaultx.service.AuthenticationService;
import com.vaultx.service.BackupService;
import com.vaultx.service.CredentialService;
import com.vaultx.service.PasswordGeneratorService;
import com.vaultx.service.PasswordStrengthService;
import com.vaultx.service.SettingsService;
import com.vaultx.util.ClipboardManager;

public final class AppContext {
    private final AuthenticationService authentication;
    private final CredentialService credentials;
    private final PasswordGeneratorService generator;
    private final PasswordStrengthService strength;
    private final BackupService backup;
    private final SettingsService settings;
    private final SessionManager session;
    private final ClipboardManager clipboard;
    private AppSettings currentSettings;

    public AppContext(AuthenticationService authentication, CredentialService credentials,
                      PasswordGeneratorService generator, PasswordStrengthService strength,
                      BackupService backup, SettingsService settings, SessionManager session,
                      ClipboardManager clipboard) {
        this.authentication = authentication;
        this.credentials = credentials;
        this.generator = generator;
        this.strength = strength;
        this.backup = backup;
        this.settings = settings;
        this.session = session;
        this.clipboard = clipboard;
        this.currentSettings = settings.load();
        clipboard.setClearTimeoutSeconds(currentSettings.clipboardClearSeconds());
    }

    public AuthenticationService authentication() { return authentication; }
    public CredentialService credentials() { return credentials; }
    public PasswordGeneratorService generator() { return generator; }
    public PasswordStrengthService strength() { return strength; }
    public BackupService backup() { return backup; }
    public SettingsService settings() { return settings; }
    public SessionManager session() { return session; }
    public ClipboardManager clipboard() { return clipboard; }

    public AppSettings currentSettings() {
        if (currentSettings == null) currentSettings = settings.load();
        return currentSettings;
    }

    public void updateSettings(AppSettings newSettings) {
        settings.save(newSettings);
        applySettings(newSettings);
    }

    public void applySettings(AppSettings newSettings) {
        currentSettings = newSettings;
        clipboard.setClearTimeoutSeconds(newSettings.clipboardClearSeconds());
    }
}
