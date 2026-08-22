package com.nagar.parishad.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Handled in SecurityConfig
    }

    @org.springframework.beans.factory.annotation.Value("${app.file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(
            org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {

        // Use "file:" prefix with the absolute path (already starts with /)
        // e.g. /Users/bhushan/.../uploads/ → file:/Users/bhushan/.../uploads/
        String path = uploadDir;
        if (!path.endsWith("/") && !path.endsWith("\\")) {
            path += "/";
        }

        // "file:" + "/Users/..." = "file:/Users/..." (correct on macOS/Linux)
        String resourceLocation = "file:" + path;

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}
