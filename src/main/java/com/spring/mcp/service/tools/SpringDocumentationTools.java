package com.spring.mcp.service.tools;

import com.spring.mcp.model.dto.DocumentationDto;
import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.repository.CodeExampleRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import com.spring.mcp.service.documentation.DocumentationService;
import com.spring.mcp.service.documentation.DocumentationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP Tools for Spring Documentation
 * Uses @Tool annotation for Spring AI MCP Server auto-discovery
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpringDocumentationTools {

    private final DocumentationService documentationService;
    private final DocumentationServiceImpl documentationServiceImpl;
    private final SpringProjectRepository projectRepository;
    private final ProjectVersionRepository versionRepository;
    private final CodeExampleRepository codeExampleRepository;

    /**
     * Search Spring documentation with full-text search
     */
    @Tool(description = """
        Search across all Spring documentation. Supports filtering by project, version, and documentation type.
        Returns relevant documentation links and snippets with relevance ranking.
        Parameters:
        - query (required): Search query string
        - project (optional): Project slug (e.g., 'spring-boot', 'spring-framework')
        - version (optional): Version string (e.g., '3.5.7')
        - docType (optional): Documentation type (e.g., 'reference', 'api')
        """)
    public Map<String, Object> searchSpringDocs(
            String query,
            String project,
            String version,
            String docType) {

        log.info("Tool: search_spring_docs - query={}, project={}, version={}, docType={}",
            query, project, version, docType);

        Instant startTime = Instant.now();

        try {
            // Validate query
            if (query == null || query.isBlank()) {
                return buildErrorResponse("Query parameter is required", null);
            }

            // Execute search
            List<DocumentationDto> results = documentationService.search(query, project, version, docType);
            long totalResults = documentationServiceImpl.countSearchResults(query, project, version, docType);

            // Calculate execution time
            long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

            // Build successful response
            return Map.of(
                "success", true,
                "query", query,
                "filters", buildFiltersMap(project, version, docType),
                "totalResults", totalResults,
                "returnedResults", results.size(),
                "executionTimeMs", executionTimeMs,
                "results", results.stream()
                    .map(this::mapDocumentationToResult)
                    .collect(Collectors.toList())
            );

        } catch (Exception e) {
            log.error("Error in search_spring_docs tool", e);
            return buildErrorResponse("Search failed: " + e.getMessage(),
                buildFiltersMap(project, version, docType));
        }
    }

    /**
     * Get available versions for a Spring project
     */
    @Tool(description = """
        List available versions for a Spring project. Shows latest stable, n-2 minor versions, and n+1 preview versions.
        Parameters:
        - project (required): Project slug (e.g., 'spring-boot', 'spring-framework', 'spring-data')
        """)
    public Map<String, Object> getSpringVersions(String project) {
        log.info("Tool: get_spring_versions - project={}", project);

        if (project == null || project.isBlank()) {
            return Map.of(
                "error", "Project parameter is required",
                "success", false
            );
        }

        return projectRepository.findBySlug(project)
            .map(p -> {
                var versions = versionRepository.findByProjectOrderByVersionDesc(p);
                return Map.of(
                    "project", p.getName(),
                    "slug", p.getSlug(),
                    "description", p.getDescription() != null ? p.getDescription() : "",
                    "versions", versions.stream()
                        .map(v -> Map.of(
                            "version", v.getVersion(),
                            "type", v.getState().name(),
                            "isLatest", v.getIsLatest(),
                            "isDefault", v.getIsDefault(),
                            "releaseDate", v.getReleaseDate() != null ? v.getReleaseDate().toString() : "unknown"
                        ))
                        .toList(),
                    "count", versions.size(),
                    "success", true
                );
            })
            .orElse(Map.of(
                "error", "Project not found: " + project,
                "success", false,
                "availableProjects", projectRepository.findByActiveTrue().stream()
                    .map(p -> p.getSlug())
                    .toList()
            ));
    }

    /**
     * List all available Spring projects
     */
    @Tool(description = """
        List all available Spring projects in the documentation system.
        Returns project names, slugs, descriptions, and homepage URLs.
        No parameters required.
        """)
    public Map<String, Object> listSpringProjects() {
        log.info("Tool: list_spring_projects");

        Instant startTime = Instant.now();

        try {
            var projects = projectRepository.findByActiveTrue();
            long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

            return Map.of(
                "success", true,
                "count", projects.size(),
                "executionTimeMs", executionTimeMs,
                "projects", projects.stream()
                    .map(p -> Map.of(
                        "name", p.getName(),
                        "slug", p.getSlug(),
                        "description", p.getDescription() != null ? p.getDescription() : "",
                        "homepage", p.getHomepageUrl() != null ? p.getHomepageUrl() : "",
                        "github", p.getGithubUrl() != null ? p.getGithubUrl() : ""
                    ))
                    .toList()
            );

        } catch (Exception e) {
            log.error("Error in list_spring_projects tool", e);
            return buildErrorResponse("Failed to list projects: " + e.getMessage(), null);
        }
    }

    /**
     * Get all documentation for a specific Spring project version
     */
    @Tool(description = """
        Get all documentation for a specific Spring project version.
        Returns all documentation links organized by type (reference, api, guides, etc.).
        Parameters:
        - project (required): Project slug (e.g., 'spring-boot', 'spring-framework')
        - version (required): Version string (e.g., '3.5.7', '6.2.1')
        """)
    public Map<String, Object> getDocumentationByVersion(String project, String version) {
        log.info("Tool: get_documentation_by_version - project={}, version={}", project, version);

        Instant startTime = Instant.now();

        try {
            // Validate parameters
            if (project == null || project.isBlank()) {
                return buildErrorResponse("Project parameter is required", null);
            }
            if (version == null || version.isBlank()) {
                return buildErrorResponse("Version parameter is required", null);
            }

            // Find project
            var springProject = projectRepository.findBySlug(project)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + project));

            // Find version
            ProjectVersion projectVersion = versionRepository.findByProjectAndVersion(springProject, version)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Version " + version + " not found for project " + project));

            // Get all documentation
            List<DocumentationDto> docs = documentationService.getByVersion(projectVersion.getId());

            // Group by doc type
            Map<String, List<Map<String, Object>>> groupedDocs = docs.stream()
                .collect(Collectors.groupingBy(
                    DocumentationDto::getDocType,
                    Collectors.mapping(this::mapDocumentationToResult, Collectors.toList())
                ));

            long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

            return Map.of(
                "success", true,
                "project", springProject.getName(),
                "projectSlug", project,
                "version", version,
                "versionType", projectVersion.getState().name(),
                "isLatest", projectVersion.getIsLatest(),
                "totalDocuments", docs.size(),
                "executionTimeMs", executionTimeMs,
                "documentationByType", groupedDocs,
                "allDocuments", docs.stream()
                    .map(this::mapDocumentationToResult)
                    .collect(Collectors.toList())
            );

        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters in get_documentation_by_version: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(),
                Map.of("project", project != null ? project : "null",
                    "version", version != null ? version : "null"));

        } catch (Exception e) {
            log.error("Error in get_documentation_by_version tool", e);
            return buildErrorResponse("Failed to get documentation: " + e.getMessage(),
                Map.of("project", project, "version", version));
        }
    }

    /**
     * Search for code examples with optional filters
     */
    @Tool(description = """
        Search for code examples with optional filters.
        Returns code snippets, descriptions, and metadata.
        Parameters:
        - query (optional): Search query for title/description
        - project (optional): Project slug (e.g., 'spring-boot')
        - version (optional): Version string (e.g., '3.5.7')
        - language (optional): Programming language (e.g., 'java', 'kotlin', 'groovy')
        - limit (optional): Maximum number of results (default: 10, max: 50)
        """)
    public Map<String, Object> getCodeExamples(
            String query,
            String project,
            String version,
            String language,
            Integer limit) {

        log.info("Tool: get_code_examples - query={}, project={}, version={}, language={}, limit={}",
            query, project, version, language, limit);

        Instant startTime = Instant.now();

        try {
            // Set default and max limit
            int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;

            List<CodeExample> examples;

            // Build query based on provided filters
            if (project != null && !project.isBlank() && version != null && !version.isBlank()) {
                // Filter by project and version
                var springProject = projectRepository.findBySlug(project)
                    .orElseThrow(() -> new IllegalArgumentException("Project not found: " + project));

                ProjectVersion projectVersion = versionRepository.findByProjectAndVersion(springProject, version)
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Version " + version + " not found for project " + project));

                if (language != null && !language.isBlank()) {
                    examples = codeExampleRepository.findByVersionAndLanguage(projectVersion, language);
                } else {
                    examples = codeExampleRepository.findByVersion(projectVersion);
                }

            } else if (query != null && !query.isBlank()) {
                // Search by query
                examples = codeExampleRepository.searchByTitle(query);
            } else {
                // Get all examples (limited)
                examples = codeExampleRepository.findAll();
            }

            // Apply text filter if query provided (for cases where we got results by version)
            if (query != null && !query.isBlank() && (project != null || version != null)) {
                String lowerQuery = query.toLowerCase();
                examples = examples.stream()
                    .filter(e -> e.getTitle().toLowerCase().contains(lowerQuery) ||
                        (e.getDescription() != null && e.getDescription().toLowerCase().contains(lowerQuery)))
                    .collect(Collectors.toList());
            }

            // Apply limit
            List<CodeExample> limitedExamples = examples.stream()
                .limit(effectiveLimit)
                .collect(Collectors.toList());

            long executionTimeMs = Duration.between(startTime, Instant.now()).toMillis();

            return Map.of(
                "success", true,
                "filters", buildCodeExampleFilters(query, project, version, language, effectiveLimit),
                "totalFound", examples.size(),
                "returnedResults", limitedExamples.size(),
                "executionTimeMs", executionTimeMs,
                "examples", limitedExamples.stream()
                    .map(this::mapCodeExampleToResult)
                    .collect(Collectors.toList())
            );

        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters in get_code_examples: {}", e.getMessage());
            return buildErrorResponse(e.getMessage(),
                buildCodeExampleFilters(query, project, version, language, limit));

        } catch (Exception e) {
            log.error("Error in get_code_examples tool", e);
            return buildErrorResponse("Failed to get code examples: " + e.getMessage(),
                buildCodeExampleFilters(query, project, version, language, limit));
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Build error response with consistent structure
     */
    private Map<String, Object> buildErrorResponse(String errorMessage, Map<String, Object> additionalInfo) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);

        if (additionalInfo != null) {
            response.putAll(additionalInfo);
        }

        return response;
    }

    /**
     * Build filters map for search results
     */
    private Map<String, Object> buildFiltersMap(String project, String version, String docType) {
        return Map.of(
            "project", project != null ? project : "all",
            "version", version != null ? version : "all",
            "docType", docType != null ? docType : "all"
        );
    }

    /**
     * Build filters map for code example results
     */
    private Map<String, Object> buildCodeExampleFilters(String query, String project,
                                                         String version, String language, Integer limit) {
        Map<String, Object> filters = new HashMap<>();
        filters.put("query", query != null ? query : "none");
        filters.put("project", project != null ? project : "all");
        filters.put("version", version != null ? version : "all");
        filters.put("language", language != null ? language : "all");
        filters.put("limit", limit != null ? limit : 10);
        return filters;
    }

    /**
     * Map DocumentationDto to result map
     */
    private Map<String, Object> mapDocumentationToResult(DocumentationDto dto) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", dto.getId());
        result.put("title", dto.getTitle());
        result.put("url", dto.getUrl());
        result.put("description", dto.getDescription() != null ? dto.getDescription() : "");
        result.put("project", dto.getProjectName());
        result.put("projectSlug", dto.getProjectSlug());
        result.put("version", dto.getVersion());
        result.put("docType", dto.getDocType());
        result.put("contentType", dto.getContentType() != null ? dto.getContentType() : "");

        // Add snippet if available
        if (dto.getSnippet() != null && !dto.getSnippet().isEmpty()) {
            result.put("snippet", dto.getSnippet());
        }

        // Add rank if available (for search results)
        if (dto.getRank() != null && dto.getRank() > 0) {
            result.put("rank", dto.getRank());
            result.put("relevance", calculateRelevanceLabel(dto.getRank()));
        }

        return result;
    }

    /**
     * Map CodeExample entity to result map
     */
    private Map<String, Object> mapCodeExampleToResult(CodeExample example) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", example.getId());
        result.put("title", example.getTitle());
        result.put("description", example.getDescription() != null ? example.getDescription() : "");
        result.put("codeSnippet", example.getCodeSnippet());
        result.put("language", example.getLanguage());
        result.put("category", example.getCategory() != null ? example.getCategory() : "");

        // Add tags if available
        if (example.getTags() != null && example.getTags().length > 0) {
            result.put("tags", Arrays.asList(example.getTags()));
        } else {
            result.put("tags", List.of());
        }

        result.put("sourceUrl", example.getSourceUrl() != null ? example.getSourceUrl() : "");

        // Add project and version info
        ProjectVersion version = example.getVersion();
        if (version != null) {
            result.put("project", version.getProject().getName());
            result.put("projectSlug", version.getProject().getSlug());
            result.put("version", version.getVersion());
        }

        return result;
    }

    /**
     * Calculate relevance label from rank score
     */
    private String calculateRelevanceLabel(double rank) {
        if (rank > 0.5) {
            return "high";
        } else if (rank > 0.2) {
            return "medium";
        } else {
            return "low";
        }
    }
}
