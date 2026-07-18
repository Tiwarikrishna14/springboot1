package com.company.orderapproval.storage;

import com.company.orderapproval.common.exception.BadRequestException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UnconfiguredFileStorageService implements FileStorageService {

    @Override
    public String upload(MultipartFile file, String organizationId, String entityType, String entityId) {
        throw notConfigured();
    }

    @Override
    public Resource download(String fileId) {
        throw notConfigured();
    }

    @Override
    public void delete(String fileId) {
        throw notConfigured();
    }

    private BadRequestException notConfigured() {
        return new BadRequestException("File storage is not configured yet");
    }
}
