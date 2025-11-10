package com.spring.mcp.controller.api;

import com.spring.mcp.repository.DocumentationContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for documentation operations
 */
@RestController
@RequestMapping("/api/documentation")
@RequiredArgsConstructor
@Slf4j
public class DocumentationApiController {

    private final DocumentationContentRepository documentationContentRepository;

    /**
     * Get documentation content by link ID
     *
     * @param id the documentation link ID
     * @return JSON response with markdown content
     */
    @GetMapping("/{id}/content")
    public ResponseEntity<Map<String, Object>> getDocumentationContent(@PathVariable Long id) {
        log.debug("Fetching content for documentation link ID: {}", id);

        return documentationContentRepository.findByLinkId(id)
            .map(content -> {
                log.info("Found content for link ID {}: {} characters", id,
                    content.getContent() != null ? content.getContent().length() : 0);

                Map<String, Object> response = new HashMap<>();
                response.put("content", content.getContent() != null ? content.getContent() : "");
                response.put("contentType", content.getContentType() != null ? content.getContentType() : "markdown");
                response.put("updatedAt", content.getUpdatedAt() != null ? content.getUpdatedAt().toString() : "");

                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                log.warn("No content found for documentation link ID: {}", id);

                Map<String, Object> response = new HashMap<>();
                response.put("content", "");
                response.put("contentType", "markdown");
                response.put("message", "No content available for this documentation link");

                return ResponseEntity.ok(response);
            });
    }
}
