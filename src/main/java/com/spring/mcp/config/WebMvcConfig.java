package com.spring.mcp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Map root to dashboard
        registry.addViewController("/").setViewName("redirect:/dashboard");

        // Login page
        registry.addViewController("/login").setViewName("login");
    }
}
