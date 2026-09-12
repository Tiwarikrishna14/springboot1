package com.company.orderapproval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties(prefix = "app.product-images")
public record ProductImageProperties(String uploadDir, String pathPrefix) {

    public ProductImageProperties {
        if (uploadDir == null || uploadDir.isBlank()) {
            uploadDir = "uploads/products";
        }
        if (pathPrefix == null || pathPrefix.isBlank()) {
            pathPrefix = "/uploads/products";
        }
        uploadDir = uploadDir.trim();
        pathPrefix = normalizePathPrefix(pathPrefix);
    }

    public Path uploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private static String normalizePathPrefix(String value) {
        String normalized = value.trim().replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
