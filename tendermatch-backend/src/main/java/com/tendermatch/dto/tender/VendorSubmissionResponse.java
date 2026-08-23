package com.tendermatch.dto.tender;

import com.tendermatch.model.VendorSubmission;
import com.tendermatch.model.enums.SubmissionStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

public class VendorSubmissionResponse {
    private UUID id;
    private UUID tenderId;
    private String vendorName;
    private String fileName;
    private Long fileSize;
    private SubmissionStatus status;
    private ZonedDateTime createdAt;

    public VendorSubmissionResponse() {}

    public VendorSubmissionResponse(UUID id, UUID tenderId, String vendorName, String fileName, Long fileSize, SubmissionStatus status, ZonedDateTime createdAt) {
        this.id = id;
        this.tenderId = tenderId;
        this.vendorName = vendorName;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static VendorSubmissionResponse fromEntity(VendorSubmission entity) {
        return new VendorSubmissionResponse(
                entity.getId(),
                entity.getTender() != null ? entity.getTender().getId() : null,
                entity.getVendorName(),
                entity.getFileName(),
                entity.getFileSize(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID tenderId;
        private String vendorName;
        private String fileName;
        private Long fileSize;
        private SubmissionStatus status;
        private ZonedDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tenderId(UUID tenderId) { this.tenderId = tenderId; return this; }
        public Builder vendorName(String vendorName) { this.vendorName = vendorName; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public Builder status(SubmissionStatus status) { this.status = status; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }

        public VendorSubmissionResponse build() {
            return new VendorSubmissionResponse(id, tenderId, vendorName, fileName, fileSize, status, createdAt);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenderId() { return tenderId; }
    public void setTenderId(UUID tenderId) { this.tenderId = tenderId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
