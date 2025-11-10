package com.spring.mcp.controller.web;

import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringBootVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringBootVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for managing Spring projects.
 * Handles CRUD operations for Spring ecosystem projects.
 *
 * <p>This controller provides endpoints for:
 * <ul>
 *   <li>Listing all Spring projects</li>
 *   <li>Viewing individual project details</li>
 *   <li>Creating new projects (ADMIN only)</li>
 *   <li>Updating existing projects (ADMIN only)</li>
 *   <li>Deleting projects (ADMIN only)</li>
 * </ul>
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectsController {

    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final SpringBootVersionRepository springBootVersionRepository;
    private final SettingsService settingsService;

    /**
     * List all Spring projects.
     *
     * <p>Displays a paginated list of all projects in the system,
     * including both active and inactive projects. Projects are
     * ordered by name for easy navigation.
     *
     * <p>Optionally filters projects by Spring Boot version compatibility.
     * When a Spring Boot version ID is provided via the {@code springBootVersionId}
     * parameter, only projects that have at least one version compatible with
     * the selected Spring Boot version are shown.
     *
     * @param springBootVersionId optional Spring Boot version ID for filtering
     * @param model Spring MVC model to add attributes for the view
     * @return view name "projects/list" which renders the project list template
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String listProjects(
            @RequestParam(required = false) Long springBootVersionId,
            Model model) {
        log.debug("Listing projects with Spring Boot version filter: {}", springBootVersionId);

        // Fetch all Spring Boot versions for the dropdown
        List<SpringBootVersion> springBootVersions = springBootVersionRepository.findAllOrderByVersionDesc();
        model.addAttribute("springBootVersions", springBootVersions);

        // Fetch projects - filtered if a Spring Boot version is selected
        List<SpringProject> projects;
        SpringBootVersion selectedVersion = null;

        if (springBootVersionId != null) {
            selectedVersion = springBootVersionRepository.findById(springBootVersionId).orElse(null);
            if (selectedVersion != null) {
                log.debug("Filtering by Spring Boot version: {}", selectedVersion.getVersion());
                projects = springProjectRepository.findAllCompatibleWithSpringBootVersion(springBootVersionId);
                model.addAttribute("selectedSpringBootVersionId", springBootVersionId);
                model.addAttribute("selectedSpringBootVersion", selectedVersion);

                // Create a map of filtered versions for each project
                java.util.Map<Long, List<ProjectVersion>> projectVersionsMap = new java.util.HashMap<>();
                for (SpringProject project : projects) {
                    List<ProjectVersion> filteredVersions = projectVersionRepository
                        .findByProjectIdAndSpringBootVersionId(project.getId(), springBootVersionId)
                        .stream()
                        .filter(ProjectVersion::getVisible)
                        .toList();
                    projectVersionsMap.put(project.getId(), filteredVersions);
                }
                model.addAttribute("projectVersionsMap", projectVersionsMap);
            } else {
                log.warn("Spring Boot version with id {} not found, showing all projects", springBootVersionId);
                projects = springProjectRepository.findAll();
            }
        } else {
            projects = springProjectRepository.findAll();
        }

        model.addAttribute("projects", projects);
        model.addAttribute("pageTitle", "Spring Projects");
        model.addAttribute("activePage", "projects");

        log.info("Retrieved {} projects (filtered: {})", projects.size(), springBootVersionId != null);
        return "projects/list";
    }

    /**
     * Display a single project's details.
     *
     * <p>Shows comprehensive information about a specific project including:
     * <ul>
     *   <li>Project metadata (name, slug, description)</li>
     *   <li>Associated versions</li>
     *   <li>Homepage and GitHub URLs</li>
     *   <li>Active status</li>
     * </ul>
     *
     * <p>Optionally filters project versions by Spring Boot version compatibility.
     * When a Spring Boot version ID is provided, only versions compatible with
     * the selected Spring Boot version are shown.
     *
     * @param id the project ID
     * @param springBootVersionId optional Spring Boot version ID for filtering
     * @param model Spring MVC model to add attributes for the view
     * @param redirectAttributes redirect attributes for flash messages
     * @return view name "projects/detail" or redirect to list if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String viewProject(
            @PathVariable Long id,
            @RequestParam(required = false) Long springBootVersionId,
            Model model,
            RedirectAttributes redirectAttributes) {
        log.debug("Viewing project with id: {} and Spring Boot version filter: {}", id, springBootVersionId);

        return springProjectRepository.findById(id)
            .map(project -> {
                model.addAttribute("project", project);
                model.addAttribute("pageTitle", project.getName());
                model.addAttribute("activePage", "projects");

                // Fetch all Spring Boot versions for the dropdown
                List<SpringBootVersion> springBootVersions = springBootVersionRepository.findAllOrderByVersionDesc();
                model.addAttribute("springBootVersions", springBootVersions);

                // Fetch versions - filtered if a Spring Boot version is selected
                List<ProjectVersion> versions;
                SpringBootVersion selectedVersion = null;

                if (springBootVersionId != null) {
                    selectedVersion = springBootVersionRepository.findById(springBootVersionId).orElse(null);
                    if (selectedVersion != null) {
                        log.debug("Filtering versions by Spring Boot version: {}", selectedVersion.getVersion());
                        versions = projectVersionRepository.findByProjectIdAndSpringBootVersionId(
                                project.getId(), springBootVersionId);
                        model.addAttribute("selectedSpringBootVersionId", springBootVersionId);
                        model.addAttribute("selectedSpringBootVersion", selectedVersion);
                    } else {
                        log.warn("Spring Boot version with id {} not found, showing all versions", springBootVersionId);
                        versions = projectVersionRepository.findByProjectId(project.getId());
                    }
                } else {
                    versions = projectVersionRepository.findByProjectId(project.getId());
                }

                // Filter to show only visible versions (status != null)
                final List<ProjectVersion> visibleVersions =
                        versions.stream().filter(ProjectVersion::getVisible).toList();

                // Get enterprise subscription setting
                boolean enterpriseSubscriptionEnabled = settingsService.isEnterpriseSubscriptionEnabled();

                model.addAttribute("versions", visibleVersions);
                model.addAttribute("enterpriseSubscriptionEnabled", enterpriseSubscriptionEnabled);
                log.info("Retrieved {} visible versions for project {} (filtered: {})",
                        visibleVersions.size(), project.getName(), springBootVersionId != null);

                return "projects/detail";
            })
            .orElseGet(() -> {
                log.warn("Project with id {} not found", id);
                redirectAttributes.addFlashAttribute("error", "Project not found");
                return "redirect:/projects";
            });
    }

}
