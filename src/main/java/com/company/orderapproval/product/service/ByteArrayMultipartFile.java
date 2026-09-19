package com.company.orderapproval.product.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

final class ByteArrayMultipartFile implements MultipartFile {
    private final String name, originalFilename, contentType;
    private final byte[] bytes;

    ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
        this.name = name; this.originalFilename = originalFilename; this.contentType = contentType; this.bytes = bytes;
    }
    public String getName() { return name; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public boolean isEmpty() { return bytes.length == 0; }
    public long getSize() { return bytes.length; }
    public byte[] getBytes() { return bytes; }
    public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
    public void transferTo(java.io.File dest) throws IOException { java.nio.file.Files.write(dest.toPath(), bytes); }
    public void transferTo(java.nio.file.Path dest) throws IOException { java.nio.file.Files.write(dest, bytes); }
}
