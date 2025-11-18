package com.spring.mcp.config;

import com.spring.mcp.service.tools.SpringDocumentationTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server Configuration
 * Registers MCP tools for Spring AI MCP Server auto-discovery
 *
 * This configuration follows the official Spring AI MCP Server pattern:
 * - Uses MethodToolCallbackProvider for @Tool annotated methods
 * - Registers tool objects for auto-discovery
 * - Enables automatic tool registration with the MCP server
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html">Spring AI MCP Server Documentation</a>
 */
@Configuration
public class McpConfig {

    /**
     * Register the Spring Documentation Tools for MCP server
     *
     * The MethodToolCallbackProvider scans the provided tool objects for @Tool annotated methods
     * and automatically registers them with the MCP server. All methods annotated with @Tool
     * in the SpringDocumentationTools class will be exposed as MCP tools.
     *
     * @param springDocumentationTools the Spring Documentation tools service
     * @return ToolCallbackProvider configured with Spring Documentation tools
     */
    @Bean
    public ToolCallbackProvider toolCallbackProvider(SpringDocumentationTools springDocumentationTools) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(springDocumentationTools)
            .build();
    }
}
