package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.Settings;
import com.spring.mcp.service.SettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for system settings.
 * Handles MCP server configuration and settings (Admin only).
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * Display settings page.
     *
     * @param model Spring MVC model to add attributes for the view
     * @return view name "settings/index"
     */
    @GetMapping
    public String showSettings(Model model) {
        log.debug("Showing settings page");

        // Set active page for sidebar navigation
        model.addAttribute("activePage", "settings");
        model.addAttribute("pageTitle", "Settings");

        // Load actual settings
        Settings settings = settingsService.getSettings();
        model.addAttribute("settings", settings);
        model.addAttribute("mcpServerStatus", "Running");
        model.addAttribute("mcpServerPort", 8080);
        model.addAttribute("databaseStatus", "Connected");

        return "settings/index";
    }

    /**
     * Update settings.
     *
     * @param enterpriseSubscriptionEnabled the enterprise subscription checkbox value
     * @param redirectAttributes for flash messages
     * @return redirect to settings page
     */
    @PostMapping
    public String updateSettings(
            @RequestParam(value = "enterpriseSubscriptionEnabled", defaultValue = "false") boolean enterpriseSubscriptionEnabled,
            RedirectAttributes redirectAttributes) {

        log.debug("Updating settings: enterpriseSubscriptionEnabled={}", enterpriseSubscriptionEnabled);

        try {
            settingsService.updateEnterpriseSubscription(enterpriseSubscriptionEnabled);
            redirectAttributes.addFlashAttribute("success",
                "Settings updated successfully. Enterprise Subscription is now " +
                (enterpriseSubscriptionEnabled ? "enabled" : "disabled") + ".");
            log.info("Settings updated successfully");
        } catch (Exception e) {
            log.error("Error updating settings", e);
            redirectAttributes.addFlashAttribute("error",
                "Failed to update settings: " + e.getMessage());
        }

        return "redirect:/settings";
    }
}
