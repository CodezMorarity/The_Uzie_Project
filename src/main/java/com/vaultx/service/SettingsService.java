package com.vaultx.service;

import com.vaultx.model.AppSettings;
import com.vaultx.repository.SettingsRepository;

public final class SettingsService {
    private final SettingsRepository repository;

    public SettingsService(SettingsRepository repository) {
        this.repository = repository;
    }

    public AppSettings load() { return repository.load(); }

    public void save(AppSettings settings) { repository.save(settings); }
}
