package com.spring.mcp.service.sync;

import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive sync service that orchestrates all project and version synchronization.
 * Ensures ALL Spring projects and their versions are loaded and updated from multiple sources.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveSyncService {

    private final SpringGenerationsSyncService generationsSyncService;
    private final ProjectSyncService projectSyncService;
    private final SpringProjectPageCrawlerService crawlerService;
    private final SpringBootVersionSyncService springBootVersionSyncService;
    private final ProjectRelationshipSyncService projectRelationshipSyncService;
    private final DocumentationSyncService documentationSyncService;
    private final SpringProjectRepository springProjectRepository;

    /**
     * Synchronize ALL Spring projects and versions from all available sources.
     * This is the master sync method that should be called to ensure complete data.
     *
     * @return ComprehensiveSyncResult with aggregated statistics
     */
    @Transactional
    public ComprehensiveSyncResult syncAll() {
        log.info("=".repeat(80));
        log.info("COMPREHENSIVE SYNC STARTED - Syncing ALL Spring Projects and Versions");
        log.info("=".repeat(80));

        ComprehensiveSyncResult result = new ComprehensiveSyncResult();
        result.setStartTime(LocalDateTime.now());

        try {
            // PHASE 0: Sync Spring Boot versions to spring_boot_versions table (PRIMARY TABLE)
            log.info("Phase 0/5: Syncing Spring Boot versions to spring_boot_versions table...");
            SpringBootVersionSyncService.SyncResult bootVersionsResult = springBootVersionSyncService.syncSpringBootVersions();
            result.setBootVersionsResult(bootVersionsResult);
            result.addVersionsCreated(bootVersionsResult.getVersionsCreated());
            result.addVersionsUpdated(bootVersionsResult.getVersionsUpdated());
            result.addErrorsEncountered(bootVersionsResult.getErrorsEncountered());

            if (bootVersionsResult.isSuccess()) {
                log.info("✓ Phase 0 completed: {} Spring Boot versions created, {} updated",
                    bootVersionsResult.getVersionsCreated(),
                    bootVersionsResult.getVersionsUpdated());
            } else {
                log.warn("✗ Phase 0 completed with errors: {}", bootVersionsResult.getErrorMessage());
            }

            // PHASE 1: Sync from Spring Generations API (Spring Boot with support dates)
            log.info("Phase 1/5: Syncing from Spring Generations API...");
            SpringGenerationsSyncService.SyncResult generationsResult = generationsSyncService.syncAllGenerations();
            result.setGenerationsResult(generationsResult);
            result.addProjectsCreated(generationsResult.getProjectsCreated());
            result.addVersionsCreated(generationsResult.getVersionsCreated());
            result.addErrorsEncountered(generationsResult.getErrorsEncountered());

            if (generationsResult.isSuccess()) {
                log.info("✓ Phase 1 completed: {} projects, {} versions created",
                    generationsResult.getProjectsCreated(),
                    generationsResult.getVersionsCreated());
            } else {
                log.warn("✗ Phase 1 completed with errors: {}", generationsResult.getErrorMessage());
            }

            // PHASE 2: Sync from Spring Initializr (additional Spring Boot versions)
            log.info("Phase 2/5: Syncing from Spring Initializr API...");
            ProjectSyncService.SyncResult initializrResult = projectSyncService.syncSpringBoot();
            result.setInitializrResult(initializrResult);
            result.addProjectsCreated(initializrResult.getProjectsCreated());
            result.addVersionsCreated(initializrResult.getVersionsCreated());
            result.addErrorsEncountered(initializrResult.getErrorsEncountered());

            if (initializrResult.isSuccess()) {
                log.info("✓ Phase 2 completed: {} projects, {} versions created",
                    initializrResult.getProjectsCreated(),
                    initializrResult.getVersionsCreated());
            } else {
                log.warn("✗ Phase 2 completed with errors: {}", initializrResult.getErrorMessage());
            }

            // PHASE 3: Crawl project pages to enrich version data
            log.info("Phase 3/5: Crawling Spring project pages for documentation links and support dates...");
            CrawlerResult crawlerResult = crawlAllProjects();
            result.setCrawlerResult(crawlerResult);
            result.addVersionsUpdated(crawlerResult.getTotalVersionsUpdated());
            result.addErrorsEncountered(crawlerResult.getTotalErrors());

            if (crawlerResult.isSuccess()) {
                log.info("✓ Phase 3 completed: {} projects crawled, {} versions updated",
                    crawlerResult.getProjectsCrawled(),
                    crawlerResult.getTotalVersionsUpdated());
            } else {
                log.warn("✗ Phase 3 completed with errors: {} failures out of {} projects",
                    crawlerResult.getTotalErrors(),
                    crawlerResult.getProjectsCrawled());
            }

            // PHASE 4: Sync project relationships (parent/child hierarchies)
            log.info("Phase 4/6: Syncing project relationships (parent/child hierarchies)...");
            ProjectRelationshipSyncService.SyncResult relationshipsResult = projectRelationshipSyncService.syncProjectRelationships();
            result.setRelationshipsResult(relationshipsResult);
            result.addRelationshipsCreated(relationshipsResult.getRelationshipsCreated());
            result.addErrorsEncountered(relationshipsResult.getErrorsEncountered());

            if (relationshipsResult.isSuccess()) {
                log.info("✓ Phase 4 completed: {} relationships created, {} skipped",
                    relationshipsResult.getRelationshipsCreated(),
                    relationshipsResult.getRelationshipsSkipped());
            } else {
                log.warn("✗ Phase 4 completed with errors: {}", relationshipsResult.getErrorMessage());
            }

            // PHASE 5: Sync documentation content (fetch OVERVIEW from spring.io and convert to Markdown)
            log.info("Phase 5/6: Syncing documentation content (OVERVIEW from spring.io)...");
            DocumentationSyncService.SyncResult documentationResult = documentationSyncService.syncAllDocumentation();
            result.setDocumentationResult(documentationResult);
            result.addDocumentationLinksCreated(documentationResult.getLinksCreated());
            result.addDocumentationLinksUpdated(documentationResult.getLinksUpdated());
            result.addErrorsEncountered(documentationResult.getErrorsEncountered());

            if (documentationResult.isSuccess()) {
                log.info("✓ Phase 5 completed: {} documentation links created, {} updated",
                    documentationResult.getLinksCreated(),
                    documentationResult.getLinksUpdated());
            } else {
                log.warn("✗ Phase 5 completed with errors: {}", documentationResult.getErrorMessage());
            }

            // Determine overall success
            result.setSuccess(bootVersionsResult.isSuccess() &&
                            generationsResult.isSuccess() &&
                            initializrResult.isSuccess() &&
                            crawlerResult.isSuccess() &&
                            relationshipsResult.isSuccess() &&
                            documentationResult.isSuccess());

            // Build summary message
            StringBuilder summary = new StringBuilder();
            if (result.isSuccess()) {
                summary.append("All phases completed successfully!");
            } else {
                summary.append("Completed with some errors.");
            }
            result.setSummaryMessage(summary.toString());

        } catch (Exception e) {
            log.error("Error during comprehensive sync", e);
            result.setSuccess(false);
            result.setSummaryMessage("Comprehensive sync failed: " + e.getMessage());
            result.addErrorsEncountered(1);
        } finally {
            result.setEndTime(LocalDateTime.now());

            log.info("=".repeat(80));
            log.info("COMPREHENSIVE SYNC COMPLETED");
            log.info("Duration: {} seconds",
                java.time.Duration.between(result.getStartTime(), result.getEndTime()).getSeconds());
            log.info("Total Projects Created: {}", result.getTotalProjectsCreated());
            log.info("Total Versions Created: {}", result.getTotalVersionsCreated());
            log.info("Total Versions Updated: {}", result.getTotalVersionsUpdated());
            log.info("Total Errors: {}", result.getTotalErrors());
            log.info("Status: {}", result.isSuccess() ? "SUCCESS" : "FAILED");
            log.info("=".repeat(80));
        }

        return result;
    }

    /**
     * Crawl all Spring project pages to extract documentation links and support dates
     *
     * @return CrawlerResult with aggregated crawl statistics
     */
    private CrawlerResult crawlAllProjects() {
        CrawlerResult result = new CrawlerResult();
        List<SpringProject> projects = springProjectRepository.findAll();

        log.debug("Found {} projects to crawl", projects.size());

        for (SpringProject project : projects) {
            try {
                log.debug("Crawling project: {}", project.getSlug());
                SpringProjectPageCrawlerService.CrawlResult crawlResult =
                    crawlerService.crawlProject(project.getSlug());

                result.addProjectCrawled();
                result.addVersionsParsed(crawlResult.getVersionsParsed());
                result.addVersionsUpdated(crawlResult.getVersionsUpdated());

                if (!crawlResult.isSuccess()) {
                    result.addError();
                    log.warn("Crawl failed for project {}: {}",
                        project.getSlug(), crawlResult.getErrorMessage());
                }

            } catch (Exception e) {
                result.addError();
                log.error("Error crawling project: {}", project.getSlug(), e);
            }
        }

        // Determine success (at least 50% of projects crawled successfully)
        result.setSuccess(result.getTotalErrors() <= (result.getProjectsCrawled() / 2));

        return result;
    }

    /**
     * Result object for crawler operation.
     */
    @Data
    @NoArgsConstructor
    public static class CrawlerResult {
        private boolean success;
        private int projectsCrawled = 0;
        private int totalVersionsParsed = 0;
        private int totalVersionsUpdated = 0;
        private int totalErrors = 0;

        public void addProjectCrawled() {
            this.projectsCrawled++;
        }

        public void addVersionsParsed(int count) {
            this.totalVersionsParsed += count;
        }

        public void addVersionsUpdated(int count) {
            this.totalVersionsUpdated += count;
        }

        public void addError() {
            this.totalErrors++;
        }
    }

    /**
     * Result object for comprehensive sync operation.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComprehensiveSyncResult {
        private boolean success;
        private String summaryMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // Aggregated statistics
        private int totalProjectsCreated;
        private int totalVersionsCreated;
        private int totalVersionsUpdated;
        private int totalRelationshipsCreated;
        private int totalDocumentationLinksCreated;
        private int totalDocumentationLinksUpdated;
        private int totalErrors;

        // Individual phase results
        private SpringBootVersionSyncService.SyncResult bootVersionsResult;
        private SpringGenerationsSyncService.SyncResult generationsResult;
        private ProjectSyncService.SyncResult initializrResult;
        private CrawlerResult crawlerResult;
        private ProjectRelationshipSyncService.SyncResult relationshipsResult;
        private DocumentationSyncService.SyncResult documentationResult;

        // Helper methods to aggregate statistics
        public void addProjectsCreated(int count) {
            this.totalProjectsCreated += count;
        }

        public void addVersionsCreated(int count) {
            this.totalVersionsCreated += count;
        }

        public void addVersionsUpdated(int count) {
            this.totalVersionsUpdated += count;
        }

        public void addRelationshipsCreated(int count) {
            this.totalRelationshipsCreated += count;
        }

        public void addDocumentationLinksCreated(int count) {
            this.totalDocumentationLinksCreated += count;
        }

        public void addDocumentationLinksUpdated(int count) {
            this.totalDocumentationLinksUpdated += count;
        }

        public void addErrorsEncountered(int count) {
            this.totalErrors += count;
        }
    }
}
