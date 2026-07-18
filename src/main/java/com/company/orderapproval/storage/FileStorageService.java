package com.company.orderapproval.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String upload(MultipartFile file, String organizationId, String entityType, String entityId);

    Resource download(String fileId);

    void delete(String fileId);
}
