package com.company.orderapproval.product.service;

import com.company.orderapproval.common.exception.ResourceNotFoundException;
import com.company.orderapproval.common.exception.BadRequestException;
import com.company.orderapproval.product.dto.BulkUploadJobResponse;
import com.company.orderapproval.product.entity.ProductBulkUploadCheckpoint;
import com.company.orderapproval.common.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.task.TaskExecutor;

@Service
@RequiredArgsConstructor
public class BulkUploadJobService {
    private final ProductService productService;
    private final TaskExecutor bulkUploadExecutor;
    private final ProductBulkUploadCheckpointRepository checkpoints;
    private final Map<UUID, Job> jobs = new ConcurrentHashMap<>();

    public BulkUploadJobResponse start(String customerSellCode, MultipartFile file, List<MultipartFile> images,
                                       UUID ownerId, UUID resumeToken) {
        try {
            if (file == null || file.isEmpty()) {
                throw new BadRequestException("CSV or Excel file is mandatory");
            }
            byte[] fileBytes = file.getBytes();
            String normalizedCustomerCode = customerSellCode.trim().toUpperCase(java.util.Locale.ROOT);
            String fileHash = HashUtil.sha256(fileBytes);
            ProductBulkUploadCheckpoint checkpoint = resumeToken == null
                    ? checkpoints.findByOwnerIdAndCustomerSellCodeAndFileHash(ownerId, normalizedCustomerCode, fileHash)
                            .orElseGet(ProductBulkUploadCheckpoint::new)
                    : checkpoints.findById(resumeToken)
                            .filter(saved -> saved.getOwnerId().equals(ownerId))
                            .filter(saved -> saved.getCustomerSellCode().equals(normalizedCustomerCode))
                            .orElseThrow(() -> new BadRequestException("Invalid bulk upload resume token"));
            if (checkpoint.getId() != null && "COMPLETED".equals(checkpoint.getStatus())) {
                throw new BadRequestException("This exact product file has already been uploaded successfully");
            }
            if (checkpoint.getId() != null && "PROCESSING".equals(checkpoint.getStatus())) {
                throw new BadRequestException("This product upload is already processing");
            }
            int resumeFrom = checkpoint.getId() != null && "FAILED".equals(checkpoint.getStatus())
                    ? checkpoint.getProcessedRows() : 0;
            checkpoint.setOwnerId(ownerId);
            checkpoint.setCustomerSellCode(normalizedCustomerCode);
            if (checkpoint.getId() == null) checkpoint.setFileHash(fileHash);
            checkpoint.setStatus("PROCESSING");
            checkpoint.setMessage(resumeFrom > 0 ? "Resuming upload from product " + (resumeFrom + 1) : "Upload started");
            checkpoint.setFailedRowNumber(null);
            checkpoint = checkpoints.save(checkpoint);
            List<BulkUploadImage> storedImages = images.stream()
                    .filter(image -> image != null && !image.isEmpty())
                    .map(image -> {
                        try {
                            return new BulkUploadImage(image.getOriginalFilename(), image.getContentType(), image.getBytes());
                        } catch (IOException ex) {
                            throw new IllegalStateException("Could not read uploaded image", ex);
                        }
                    }).toList();
            UUID id = UUID.randomUUID();
            Job job = new Job(id, ownerId, normalizedCustomerCode, file.getOriginalFilename(), file.getContentType(),
                    fileBytes, storedImages, checkpoint, resumeFrom);
            jobs.put(id, job);
            bulkUploadExecutor.execute(() -> processJob(job));
            return job.response();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read uploaded file", ex);
        }
    }

    public BulkUploadJobResponse get(UUID id, UUID ownerId) {
        Job job = jobs.get(id);
        if (job == null || !job.ownerId.equals(ownerId)) {
            throw new ResourceNotFoundException("Bulk upload job not found");
        }
        return job.response();
    }

    private void processJob(Job job) {
        try {
            job.processing();
            productService.processBulkUpload(job.customerSellCode, job.fileBytes, job.fileName, job.contentType,
                    job.images, job.resumeFrom, (total, processed) -> {
                        job.progress(total, processed);
                        persistProgress(job, total, processed);
                    });
            job.completed();
            job.checkpoint.setStatus("COMPLETED");
            job.checkpoint.setProcessedRows(job.total);
            job.checkpoint.setMessage("Product upload completed");
            checkpoints.save(job.checkpoint);
        } catch (Exception ex) {
            job.failed(ex.getMessage() == null ? "Bulk upload failed" : ex.getMessage());
            job.checkpoint.setStatus("FAILED");
            job.checkpoint.setProcessedRows(job.processed);
            job.checkpoint.setMessage(job.message);
            checkpoints.save(job.checkpoint);
        }
    }

    private void persistProgress(Job job, int total, int processed) {
        job.checkpoint.setTotalRows(total);
        job.checkpoint.setProcessedRows(processed);
        job.checkpoint.setStatus("PROCESSING");
        job.checkpoint.setMessage("Processed " + processed + " of " + total + " products");
        checkpoints.save(job.checkpoint);
    }

    private static final class Job {
        private final UUID id, ownerId;
        private final String customerSellCode, fileName, contentType;
        private final byte[] fileBytes;
        private final List<BulkUploadImage> images;
        private final ProductBulkUploadCheckpoint checkpoint;
        private final int resumeFrom;
        private String status = "QUEUED", message = "Upload queued";
        private int total, processed, failed;
        private Instant startedAt, completedAt;
        private long startedNanos;

        private Job(UUID id, UUID ownerId, String customerSellCode, String fileName, String contentType, byte[] fileBytes,
                    List<BulkUploadImage> images, ProductBulkUploadCheckpoint checkpoint, int resumeFrom) {
            this.id = id; this.ownerId = ownerId; this.customerSellCode = customerSellCode;
            this.fileName = fileName; this.contentType = contentType; this.fileBytes = fileBytes; this.images = images;
            this.checkpoint = checkpoint; this.resumeFrom = resumeFrom; this.processed = resumeFrom;
        }
        synchronized void processing() { status = "PROCESSING"; message = "Reading and processing products"; startedAt = Instant.now(); startedNanos = System.nanoTime(); }
        synchronized void progress(int total, int processed) { this.total = total; this.processed = processed; this.message = "Processed " + processed + " of " + total + " products"; }
        synchronized void completed() { status = "COMPLETED"; processed = total; message = "Product insertion completed"; completedAt = Instant.now(); }
        synchronized void failed(String error) { status = "FAILED"; failed = 1; message = error; completedAt = Instant.now(); }
        synchronized BulkUploadJobResponse response() {
            int percent = total == 0 ? 0 : Math.min(100, processed * 100 / total);
            Long remaining = null;
            if (status.equals("PROCESSING") && processed > 0 && total > processed) {
                long elapsed = Duration.ofNanos(System.nanoTime() - startedNanos).toSeconds();
                remaining = Math.max(1, (long) Math.ceil((double) (total - processed) * elapsed / processed));
            }
            return new BulkUploadJobResponse(id, checkpoint.getId(), status, total, processed, failed, resumeFrom > 0,
                    resumeFrom + 1, percent, remaining, message, startedAt, completedAt);
        }
    }
}
