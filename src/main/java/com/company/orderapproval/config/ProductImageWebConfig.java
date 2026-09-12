package com.company.orderapproval.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ProductImageWebConfig implements WebMvcConfigurer {

    private final ProductImageProperties productImageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = productImageProperties.uploadPath().toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }

        registry.addResourceHandler(productImageProperties.pathPrefix() + "/**")
                .addResourceLocations(resourceLocation);
    }
}
