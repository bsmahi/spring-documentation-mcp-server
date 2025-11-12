package com.spring.mcp.service.sync;

import com.spring.mcp.model.entity.CodeExample;
import com.spring.mcp.model.entity.ProjectVersion;
import com.spring.mcp.model.entity.SpringProject;
import com.spring.mcp.repository.CodeExampleRepository;
import com.spring.mcp.repository.ProjectVersionRepository;
import com.spring.mcp.repository.SpringProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlPage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service to fetch code examples from Spring Guides (spring.io/guides).
 *
 * Spring Guides provide comprehensive tutorials with code examples that are
 * more valuable than isolated snippets from project pages.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpringGuideFetchService {

    private final CodeExampleRepository codeExampleRepository;
    private final ProjectVersionRepository projectVersionRepository;
    private final SpringProjectRepository springProjectRepository;

    private static final String GUIDES_BASE_URL = "https://spring.io/guides";

    /**
     * Curated list of popular Spring Guides.
     * These are the most commonly used guides that provide the best code examples.
     */
    private static final List<SpringGuide> POPULAR_GUIDES = Arrays.asList(
        // Getting Started Guides
        new SpringGuide("gs/rest-service", "Building a RESTful Web Service", "spring-boot", "Getting Started"),
        new SpringGuide("gs/spring-boot", "Building an Application with Spring Boot", "spring-boot", "Getting Started"),
        new SpringGuide("gs/rest-service-cors", "Enabling Cross Origin Requests", "spring-boot", "Getting Started"),
        new SpringGuide("gs/accessing-data-jpa", "Accessing Data with JPA", "spring-data", "Getting Started"),
        new SpringGuide("gs/accessing-data-rest", "Accessing JPA Data with REST", "spring-data", "Getting Started"),
        new SpringGuide("gs/accessing-data-mongodb", "Accessing Data with MongoDB", "spring-data", "Getting Started"),
        new SpringGuide("gs/accessing-data-mysql", "Accessing Data with MySQL", "spring-data", "Getting Started"),
        new SpringGuide("gs/securing-web", "Securing a Web Application", "spring-security", "Getting Started"),
        new SpringGuide("gs/spring-boot-docker", "Spring Boot with Docker", "spring-boot", "Getting Started"),
        new SpringGuide("gs/testing-web", "Testing the Web Layer", "spring-boot", "Getting Started"),
        new SpringGuide("gs/consuming-rest", "Consuming a RESTful Web Service", "spring-boot", "Getting Started"),
        new SpringGuide("gs/scheduling-tasks", "Scheduling Tasks", "spring-boot", "Getting Started"),
        new SpringGuide("gs/validating-form-input", "Validating Form Input", "spring-boot", "Getting Started"),
        new SpringGuide("gs/uploading-files", "Uploading Files", "spring-boot", "Getting Started"),
        new SpringGuide("gs/messaging-redis", "Messaging with Redis", "spring-data", "Getting Started"),
        new SpringGuide("gs/messaging-rabbitmq", "Messaging with RabbitMQ", "spring-amqp", "Getting Started"),
        new SpringGuide("gs/batch-processing", "Creating a Batch Service", "spring-batch", "Getting Started"),
        new SpringGuide("gs/async-method", "Creating Asynchronous Methods", "spring-boot", "Getting Started"),
        new SpringGuide("gs/caching", "Caching Data", "spring-boot", "Getting Started"),
        new SpringGuide("gs/relational-data-access", "Accessing Relational Data with JDBC", "spring-boot", "Getting Started"),

        // Topical Guides
        new SpringGuide("topical/spring-boot-docker", "Spring Boot with Docker", "spring-boot", "Topical"),
        new SpringGuide("topical/spring-security-architecture", "Spring Security Architecture", "spring-security", "Topical")
    );

    /**
     * Sync code examples from all popular Spring Guides.
     *
     * @return number of code examples synchronized
     */
    public int syncSpringGuides() {
        log.info("Starting Spring Guides code examples sync");
        int totalExamples = 0;

        for (SpringGuide guide : POPULAR_GUIDES) {
            try {
                int examplesFromGuide = syncGuide(guide);
                totalExamples += examplesFromGuide;
                log.debug("Synced {} examples from guide: {}", examplesFromGuide, guide.title);
            } catch (Exception e) {
                log.error("Error syncing guide: {} - {}", guide.title, e.getMessage(), e);
            }
        }

        log.info("Spring Guides sync complete. Total examples: {}", totalExamples);
        return totalExamples;
    }

    /**
     * Sync code examples from a specific guide.
     *
     * @param guide the guide to sync
     * @return number of examples extracted
     */
    private int syncGuide(SpringGuide guide) {
        String guideUrl = GUIDES_BASE_URL + "/" + guide.urlPath;
        log.debug("Fetching guide: {} from {}", guide.title, guideUrl);

        try (WebClient webClient = createWebClient()) {
            HtmlPage page = webClient.getPage(guideUrl);
            webClient.waitForBackgroundJavaScript(15000);

            // Extract code blocks
            List<ExtractedCodeExample> examples = extractCodeExamples(page, guide);

            if (examples.isEmpty()) {
                log.warn("No code examples found in guide: {}", guide.title);
                return 0;
            }

            // Find the appropriate version for this guide's project
            Optional<ProjectVersion> projectVersionOpt = findLatestVersionForProject(guide.projectSlug);

            if (projectVersionOpt.isEmpty()) {
                log.warn("No project version found for: {}", guide.projectSlug);
                return 0;
            }

            ProjectVersion projectVersion = projectVersionOpt.get();

            // Save examples to database (check for duplicates first)
            int savedCount = 0;
            int skippedCount = 0;
            for (ExtractedCodeExample example : examples) {
                String exampleTitle = guide.title + " - Example " + example.index;

                // Check if this example already exists
                if (codeExampleRepository.existsByVersionAndTitle(projectVersion, exampleTitle)) {
                    log.debug("Example already exists, skipping: {}", exampleTitle);
                    skippedCount++;
                    continue;
                }

                CodeExample codeExample = CodeExample.builder()
                    .title(exampleTitle)
                    .description(example.context != null ? example.context : guide.title)
                    .codeSnippet(example.code)
                    .language(example.language != null ? example.language : "java")
                    .category(guide.category)
                    .sourceUrl(guideUrl)
                    .version(projectVersion)
                    .tags(new String[]{guide.category, "spring-guide", guide.urlPath.split("/")[0]}) // e.g., "gs", "topical"
                    .build();

                try {
                    codeExampleRepository.save(codeExample);
                    savedCount++;
                } catch (Exception e) {
                    log.error("Error saving code example: {}", example.index, e);
                }
            }

            log.info("Saved {} code examples from guide: {}", savedCount, guide.title);
            return savedCount;

        } catch (Exception e) {
            log.error("Error fetching guide: {} - {}", guide.title, e.getMessage());
            throw new RuntimeException("Failed to fetch guide: " + guide.title, e);
        }
    }

    /**
     * Extract code examples from a guide page.
     *
     * @param page the HTML page
     * @param guide the guide metadata
     * @return list of extracted code examples
     */
    private List<ExtractedCodeExample> extractCodeExamples(HtmlPage page, SpringGuide guide) {
        List<ExtractedCodeExample> examples = new ArrayList<>();

        // Try to find code blocks
        DomNodeList<DomNode> codeBlocks = page.querySelectorAll("pre code");

        if (codeBlocks.isEmpty()) {
            // Fallback to just pre tags
            codeBlocks = page.querySelectorAll("pre");
        }

        // Get all section headers for context
        DomNodeList<DomNode> headers = page.querySelectorAll("h2, h3");

        int index = 1;
        for (DomNode codeBlock : codeBlocks) {
            String code = codeBlock.asNormalizedText();

            // Skip very short snippets (likely not real code examples)
            if (code.length() < 50) {
                continue;
            }

            ExtractedCodeExample example = new ExtractedCodeExample();
            example.index = index++;
            example.code = code;

            // Try to detect language from class attribute
            if (codeBlock instanceof DomElement) {
                DomElement elem = (DomElement) codeBlock;
                String className = elem.getAttribute("class");
                if (className != null && !className.isEmpty()) {
                    example.language = detectLanguage(className);
                }
            }

            // Try to find context from nearest preceding header
            example.context = findNearestContext(codeBlock, headers);

            examples.add(example);
        }

        return examples;
    }

    /**
     * Find the nearest context (section header) for a code block.
     *
     * @param codeBlock the code block element
     * @param headers all headers on the page
     * @return context text or null
     */
    private String findNearestContext(DomNode codeBlock, DomNodeList<DomNode> headers) {
        // This is a simple heuristic: use the first non-empty header text
        // A more sophisticated approach would calculate DOM distance
        for (DomNode header : headers) {
            String headerText = header.asNormalizedText();
            if (!headerText.isEmpty() && headerText.length() < 200) {
                return headerText;
            }
        }
        return null;
    }

    /**
     * Detect programming language from CSS class name.
     *
     * @param className the CSS class name
     * @return detected language or "java" as default
     */
    private String detectLanguage(String className) {
        className = className.toLowerCase();
        if (className.contains("java")) return "java";
        if (className.contains("kotlin")) return "kotlin";
        if (className.contains("groovy")) return "groovy";
        if (className.contains("xml")) return "xml";
        if (className.contains("json")) return "json";
        if (className.contains("yaml") || className.contains("yml")) return "yaml";
        if (className.contains("shell") || className.contains("bash")) return "bash";
        if (className.contains("sql")) return "sql";
        if (className.contains("properties")) return "properties";
        return "java"; // Default to Java for Spring guides
    }

    /**
     * Find the latest version for a project.
     *
     * @param projectSlug the project slug
     * @return optional project version
     */
    private Optional<ProjectVersion> findLatestVersionForProject(String projectSlug) {
        // First find the project by slug
        Optional<SpringProject> projectOpt = springProjectRepository.findBySlug(projectSlug);

        if (projectOpt.isEmpty()) {
            log.warn("No project found with slug: {}", projectSlug);
            return Optional.empty();
        }

        SpringProject project = projectOpt.get();

        // Try to find the version marked as latest
        Optional<ProjectVersion> latestVersion = projectVersionRepository.findByProjectAndIsLatestTrue(project);

        // If no latest version found, get the most recently created one
        if (latestVersion.isEmpty()) {
            latestVersion = projectVersionRepository.findFirstByProjectOrderByCreatedAtDesc(project);
        }

        return latestVersion;
    }

    /**
     * Create and configure a WebClient for fetching guides.
     *
     * @return configured WebClient
     */
    private WebClient createWebClient() {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setTimeout(30000);
        return webClient;
    }

    /**
     * DTO for Spring Guide metadata.
     */
    private static class SpringGuide {
        String urlPath;       // e.g., "gs/rest-service"
        String title;
        String projectSlug;   // e.g., "spring-boot"
        String category;      // e.g., "Getting Started", "Topical"

        SpringGuide(String urlPath, String title, String projectSlug, String category) {
            this.urlPath = urlPath;
            this.title = title;
            this.projectSlug = projectSlug;
            this.category = category;
        }
    }

    /**
     * DTO for extracted code examples.
     */
    private static class ExtractedCodeExample {
        int index;
        String code;
        String language;
        String context;
    }
}
