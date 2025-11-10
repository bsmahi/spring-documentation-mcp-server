package com.spring.mcp.controller.advice;

import com.spring.mcp.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice that adds common model attributes to all views.
 * This includes system-wide settings like enterprise subscription status.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-10
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributesAdvice {

    private final SettingsService settingsService;

    /**
     * Adds the enterprise subscription status to all models.
     * This allows all templates to access the current support level.
     *
     * @return true if enterprise subscription is enabled, false otherwise
     */
    @ModelAttribute("enterpriseSubscriptionEnabled")
    public boolean addEnterpriseSubscriptionEnabled() {
        return settingsService.isEnterpriseSubscriptionEnabled();
    }
}
