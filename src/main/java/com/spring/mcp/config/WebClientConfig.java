package com.spring.mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for WebClient used in external API calls.
 *
 * @author Spring MCP Server
 * @version 1.0
 * @since 2025-01-08
 */
@Configuration
public class WebClientConfig {

    /**
     * Create a WebClient.Builder bean for injection.
     *
     * @return WebClient.Builder instance
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
