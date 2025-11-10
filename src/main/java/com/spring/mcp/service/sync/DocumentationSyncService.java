package com.spring.mcp.service.sync;

import com.spring.mcp.model.entity.*;
import com.spring.mcp.repository.*;
import com.spring.mcp.service.documentation.DocumentationFetchService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for syncing documentation content for Spring projects.
 * Fetches OVERVIEW content from spring.io/projects/{slug} and stores it as Markdown.
 *
 * @author Spring MCP Server
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationSyncService {

    private final SpringProjectRepository springProjectRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final DocumentationTypeRepository documentationTypeRepository;
    private final DocumentationLinkRepository documentationLinkRepository;
    private final DocumentationContentRepository documentationContentRepository;
    private final DocumentationFetchService documentationFetchService;

    /**
     * Syncs documentation for all Spring projects by fetching OVERVIEW content
     * from spring.io/projects/{slug} and converting to Markdown.
     *
     * @return SyncResult with statistics
     */
    @Transactional
    public SyncResult syncAllDocumentation() {
        log.info("Starting documentation sync for all Spring projects...");
        SyncResult result = new SyncResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // Get the OVERVIEW documentation type
            DocumentationType overviewType = documentationTypeRepository.findBySlug("overview")
                .orElseGet(() -> {
                    log.info("Creating OVERVIEW documentation type");
                    DocumentationType type = DocumentationType.builder()
                        .name("Overview")
                        .slug("overview")
                        .displayOrder(1)
                        .build();
                    return documentationTypeRepository.save(type);
                });

            // Get all active Spring projects
            List<SpringProject> projects = springProjectRepository.findAll().stream()
                .filter(SpringProject::getActive)
                .toList();

            log.info("Found {} active projects to sync documentation for", projects.size());

            for (SpringProject project : projects) {
                try {
                    syncProjectDocumentation(project, overviewType, result);
                } catch (Exception e) {
                    log.error("Error syncing documentation for project {}: {}",
                        project.getSlug(), e.getMessage(), e);
                    result.addError();
                }
            }

            result.setSuccess(result.getErrorsEncountered() == 0);
            result.setSummaryMessage(String.format(
                "Synced documentation for %d projects: %d links created, %d updated, %d errors",
                result.getProjectsProcessed(),
                result.getLinksCreated(),
                result.getLinksUpdated(),
                result.getErrorsEncountered()
            ));

        } catch (Exception e) {
            log.error("Error during documentation sync", e);
            result.setSuccess(false);
            result.setSummaryMessage("Documentation sync failed: " + e.getMessage());
            result.addError();
        } finally {
            result.setEndTime(LocalDateTime.now());
        }

        log.info("Documentation sync completed: {}", result.getSummaryMessage());
        return result;
    }

    /**
     * Syncs documentation for a single project
     */
    private void syncProjectDocumentation(SpringProject project, DocumentationType overviewType, SyncResult result) {
        log.debug("Syncing documentation for project: {}", project.getSlug());
        result.addProjectProcessed();

        try {
            // Fetch OVERVIEW content as Markdown
            String markdown = documentationFetchService.fetchProjectOverviewAsMarkdown(project.getSlug());

            if (markdown == null || markdown.isBlank()) {
                log.warn("No OVERVIEW content found for project: {}", project.getSlug());
                return;
            }

            // Get the latest version or create a generic documentation link
            ProjectVersion latestVersion = projectVersionRepository.findByProjectAndIsLatestTrue(project)
                .orElseGet(() -> projectVersionRepository.findByProject(project).stream()
                    .findFirst()
                    .orElse(null));

            if (latestVersion == null) {
                log.warn("No versions found for project: {}, skipping documentation", project.getSlug());
                return;
            }

            // Create or update documentation link
            String docUrl = "https://docs.spring.io/" + project.getSlug() + "/index.html";
            DocumentationLink link = documentationLinkRepository.findByUrl(docUrl)
                .orElseGet(() -> {
                    log.info("Creating documentation link for: {}", project.getSlug());
                    DocumentationLink newLink = DocumentationLink.builder()
                        .version(latestVersion)
                        .docType(overviewType)
                        .title(project.getName() + " - Documentation")
                        .url(docUrl)
                        .description("Documentation from docs.spring.io")
                        .isActive(true)
                        .build();
                    result.addLinkCreated();
                    return documentationLinkRepository.save(newLink);
                });

            // Calculate content hash
            String contentHash = documentationFetchService.calculateContentHash(markdown);

            // Check if content has changed
            if (contentHash.equals(link.getContentHash())) {
                log.debug("Documentation content unchanged for: {}", project.getSlug());
                link.setLastFetched(LocalDateTime.now());
                documentationLinkRepository.save(link);
                return;
            }

            // Update link
            link.setContentHash(contentHash);
            link.setLastFetched(LocalDateTime.now());
            link.setIsActive(true);
            documentationLinkRepository.save(link);

            // Create or update documentation content
            DocumentationContent content = documentationContentRepository.findByLink(link)
                .orElseGet(() -> {
                    DocumentationContent newContent = DocumentationContent.builder()
                        .link(link)
                        .build();
                    return newContent;
                });

            content.setContentType("text/markdown");
            content.setContent(markdown);
            content.setMetadata(java.util.Map.of(
                "source", "docs.spring.io",
                "projectSlug", project.getSlug(),
                "section", "documentation",
                "fetchedAt", LocalDateTime.now().toString(),
                "contentLength", markdown.length()
            ));

            documentationContentRepository.save(content);
            result.addLinkUpdated();

            log.info("Successfully synced documentation for: {} - Hash: {}, Size: {} chars",
                project.getSlug(), contentHash.substring(0, 8), markdown.length());

        } catch (Exception e) {
            log.error("Error syncing documentation for project {}: {}",
                project.getSlug(), e.getMessage(), e);
            result.addError();
        }
    }

    /**
     * Result object for documentation sync operations
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SyncResult {
        private boolean success;
        private String summaryMessage;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        @Builder.Default
        private int projectsProcessed = 0;
        @Builder.Default
        private int linksCreated = 0;
        @Builder.Default
        private int linksUpdated = 0;
        @Builder.Default
        private int errorsEncountered = 0;

        public void addProjectProcessed() {
            this.projectsProcessed++;
        }

        public void addLinkCreated() {
            this.linksCreated++;
        }

        public void addLinkUpdated() {
            this.linksUpdated++;
        }

        public void addError() {
            this.errorsEncountered++;
        }
    }
}
