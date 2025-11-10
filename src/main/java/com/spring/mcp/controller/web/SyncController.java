package com.spring.mcp.controller.web;

import com.spring.mcp.service.sync.ComprehensiveSyncService;
import com.spring.mcp.service.sync.ProjectSyncService;
import com.spring.mcp.service.sync.SpringGenerationsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for synchronizing Spring projects and versions from external sources.
 * Admin-only access required.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/sync")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class SyncController {

    private final ProjectSyncService projectSyncService;
    private final SpringGenerationsSyncService generationsSyncService;
    private final ComprehensiveSyncService comprehensiveSyncService;

    /**
     * Show sync page with options.
     *
     * @param model Spring MVC model
     * @return view name "sync/index"
     */
    @GetMapping
    public String showSyncPage(Model model) {
        log.debug("Showing sync page");
        model.addAttribute("activePage", "sync");
        model.addAttribute("pageTitle", "Synchronize Projects & Versions");
        return "sync/index";
    }

    /**
     * Trigger manual sync of Spring Boot project and versions.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/spring-boot")
    public String syncSpringBoot(RedirectAttributes redirectAttributes) {
        log.info("Manual Spring Boot sync triggered");

        try {
            ProjectSyncService.SyncResult result = projectSyncService.syncSpringBoot();

            if (result.isSuccess()) {
                String message = String.format(
                    "Spring Boot sync completed successfully! Created %d versions (errors: %d)",
                    result.getVersionsCreated(),
                    result.getErrorsEncountered()
                );
                redirectAttributes.addFlashAttribute("success", message);
                log.info(message);
            } else {
                String errorMsg = "Spring Boot sync failed: " + result.getErrorMessage();
                redirectAttributes.addFlashAttribute("error", errorMsg);
                log.error(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Error during Spring Boot sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * Trigger manual sync of all Spring projects and versions from Spring Generations API.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/generations")
    public String syncGenerations(RedirectAttributes redirectAttributes) {
        log.info("Manual Spring Generations sync triggered");

        try {
            SpringGenerationsSyncService.SyncResult result = generationsSyncService.syncAllGenerations();

            if (result.isSuccess()) {
                String message = String.format(
                    "Spring Generations sync completed successfully! Created %d projects and %d versions (errors: %d)",
                    result.getProjectsCreated(),
                    result.getVersionsCreated(),
                    result.getErrorsEncountered()
                );
                redirectAttributes.addFlashAttribute("success", message);
                log.info(message);
            } else {
                String errorMsg = "Spring Generations sync failed: " + result.getErrorMessage();
                redirectAttributes.addFlashAttribute("error", errorMsg);
                log.error(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Error during Spring Generations sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }

    /**
     * Trigger comprehensive sync of ALL Spring projects and versions from ALL sources.
     * This is the master sync that ensures complete data coverage.
     *
     * @param redirectAttributes for flash messages
     * @return redirect to sync page
     */
    @PostMapping("/all")
    public String syncAll(RedirectAttributes redirectAttributes) {
        log.info("Manual COMPREHENSIVE sync triggered - syncing ALL projects and versions");

        try {
            ComprehensiveSyncService.ComprehensiveSyncResult result = comprehensiveSyncService.syncAll();

            if (result.isSuccess()) {
                String message = String.format(
                    "Comprehensive sync completed successfully! " +
                    "Total: %d projects, %d versions created, %d versions updated (errors: %d)",
                    result.getTotalProjectsCreated(),
                    result.getTotalVersionsCreated(),
                    result.getTotalVersionsUpdated(),
                    result.getTotalErrors()
                );
                redirectAttributes.addFlashAttribute("success", message);
                log.info(message);
            } else {
                String errorMsg = "Comprehensive sync completed with errors: " + result.getSummaryMessage();
                redirectAttributes.addFlashAttribute("error", errorMsg);
                log.error(errorMsg);
            }
        } catch (Exception e) {
            String errorMsg = "Error during comprehensive sync: " + e.getMessage();
            redirectAttributes.addFlashAttribute("error", errorMsg);
            log.error(errorMsg, e);
        }

        return "redirect:/sync";
    }
}
