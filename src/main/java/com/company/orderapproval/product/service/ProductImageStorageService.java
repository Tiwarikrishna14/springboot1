package com.company.orderapproval.product.service;

import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.config.ProductImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final ProductImageProperties properties;

    public String store(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        String originalFilename = cleanFilename(image.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Product image must be JPG, PNG, WEBP, or GIF");
        }

        String contentType = image.getContentType();
        if (contentType != null && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BadRequestException("Uploaded product image has an invalid content type");
        }

        Path uploadRoot = properties.uploadPath();
        try {
            Files.createDirectories(uploadRoot);
            String storedFilename = UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
            Path destination = uploadRoot.resolve(storedFilename).normalize();
            if (!destination.startsWith(uploadRoot)) {
                throw new BadRequestException("Invalid product image filename");
            }
            image.transferTo(destination);
            return properties.pathPrefix() + "/" + storedFilename;
        } catch (IOException ex) {
            throw new BadRequestException("Failed to store product image");
        }
    }

    public void delete(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        String normalizedPath = imagePath.trim().replace('\\', '/');
        if (!normalizedPath.startsWith(properties.pathPrefix() + "/")) {
            return;
        }

        Path uploadRoot = properties.uploadPath();
        Path imageFile = uploadRoot.resolve(cleanFilename(normalizedPath)).normalize();
        if (!imageFile.startsWith(uploadRoot)) {
            return;
        }

        try {
            Files.deleteIfExists(imageFile);
        } catch (IOException ignored) {
            // Product records should still update/delete if old image cleanup fails.
        }
    }

    public Map<String, MultipartFile> indexByOriginalFilename(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return Map.of();
        }

        Map<String, MultipartFile> indexedImages = new HashMap<>();
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }

            String filename = cleanFilename(image.getOriginalFilename());
            if (filename.isBlank()) {
                throw new BadRequestException("Uploaded product image filename is missing");
            }

            String key = filenameKey(filename);
            if (indexedImages.putIfAbsent(key, image) != null) {
                throw new BadRequestException("Duplicate uploaded product image filename: " + filename);
            }
        }
        return indexedImages;
    }

    public String filenameKey(String filenameOrPath) {
        return cleanFilename(filenameOrPath).toLowerCase(Locale.ROOT);
    }

    private String cleanFilename(String filenameOrPath) {
        if (filenameOrPath == null || filenameOrPath.isBlank()) {
            return "";
        }
        String normalized = filenameOrPath.trim().replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String filename = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        return StringUtils.cleanPath(filename);
    }
}
