package com.spring.mcp.service;

import com.spring.mcp.model.entity.Settings;
import com.spring.mcp.repository.SettingsRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing application settings.
 * Handles the singleton Settings entity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final SettingsRepository settingsRepository;

    /**
     * Get the current settings.
     * If no settings exist, creates default settings.
     *
     * @return the current settings
     */
    @Transactional(readOnly = true)
    public Settings getSettings() {
        return settingsRepository.findFirstBy()
            .orElseGet(this::createDefaultSettings);
    }

    /**
     * Get the enterprise subscription status.
     *
     * @return true if enterprise subscription is enabled, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isEnterpriseSubscriptionEnabled() {
        return getSettings().getEnterpriseSubscriptionEnabled();
    }

    /**
     * Update the enterprise subscription status.
     *
     * @param enabled the new enterprise subscription status
     * @return the updated settings
     */
    @Transactional
    public Settings updateEnterpriseSubscription(boolean enabled) {
        Settings settings = getSettings();
        settings.setEnterpriseSubscriptionEnabled(enabled);
        Settings updated = settingsRepository.save(settings);
        log.info("Enterprise subscription updated to: {}", enabled);
        return updated;
    }

    /**
     * Update all settings.
     *
     * @param settings the updated settings
     * @return the saved settings
     */
    @Transactional
    public Settings updateSettings(Settings settings) {
        Settings updated = settingsRepository.save(settings);
        log.info("Settings updated: {}", updated);
        return updated;
    }

    /**
     * Create default settings.
     * This is called if no settings exist in the database.
     *
     * @return the created default settings
     */
    @Transactional
    protected Settings createDefaultSettings() {
        log.warn("No settings found, creating default settings");
        Settings defaultSettings = Settings.builder()
            .enterpriseSubscriptionEnabled(false)
            .build();
        return settingsRepository.save(defaultSettings);
    }

    /**
     * Initialize settings on application startup.
     * Ensures settings exist in the database.
     */
    @PostConstruct
    public void initializeSettings() {
        try {
            Settings settings = getSettings();
            log.info("Settings initialized: Enterprise Subscription Enabled = {}",
                settings.getEnterpriseSubscriptionEnabled());
        } catch (Exception e) {
            log.error("Failed to initialize settings", e);
        }
    }
}
