package com.tendermatch.model;

import com.tendermatch.model.enums.SubmissionStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_submissions")
public class VendorSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id", nullable = false)
    private Tender tender;

    @Column(name = "vendor_name", length = 500)
    private String vendorName;

    @Column(name = "file_key", nullable = false, length = 1000)
    private String fileKey;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SubmissionStatus status = SubmissionStatus.UPLOADED;

    @Column(name = "processing_stage", length = 100)
    private String processingStage = "QUEUED";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "is_review_locked", nullable = false)
    private boolean isReviewLocked = false;

    @Column(name = "review_finalized_at")
    private ZonedDateTime reviewFinalizedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_finalized_by")
    private User reviewFinalizedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public VendorSubmission() {}

    public VendorSubmission(UUID id, Tender tender, String vendorName, String fileKey, String fileName,
                            Long fileSize, SubmissionStatus status, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.tender = tender;
        this.vendorName = vendorName;
        this.fileKey = fileKey;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status != null ? status : SubmissionStatus.UPLOADED;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Tender tender;
        private String vendorName;
        private String fileKey;
        private String fileName;
        private Long fileSize;
        private SubmissionStatus status = SubmissionStatus.UPLOADED;
        private String processingStage = "QUEUED";
        private String errorMessage;
        private ZonedDateTime createdAt;
        private ZonedDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tender(Tender tender) { this.tender = tender; return this; }
        public Builder vendorName(String vendorName) { this.vendorName = vendorName; return this; }
        public Builder fileKey(String fileKey) { this.fileKey = fileKey; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public Builder status(SubmissionStatus status) { this.status = status; return this; }
        public Builder processingStage(String processingStage) { this.processingStage = processingStage; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public VendorSubmission build() {
            VendorSubmission vs = new VendorSubmission(id, tender, vendorName, fileKey, fileName, fileSize, status, createdAt, updatedAt);
            vs.setProcessingStage(processingStage);
            vs.setErrorMessage(errorMessage);
            return vs;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tender getTender() { return tender; }
    public void setTender(Tender tender) { this.tender = tender; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }

    public String getProcessingStage() { return processingStage; }
    public void setProcessingStage(String processingStage) { this.processingStage = processingStage; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean isReviewLocked() { return isReviewLocked; }
    public void setReviewLocked(boolean reviewLocked) { isReviewLocked = reviewLocked; }

    public ZonedDateTime getReviewFinalizedAt() { return reviewFinalizedAt; }
    public void setReviewFinalizedAt(ZonedDateTime reviewFinalizedAt) { this.reviewFinalizedAt = reviewFinalizedAt; }

    public User getReviewFinalizedBy() { return reviewFinalizedBy; }
    public void setReviewFinalizedBy(User reviewFinalizedBy) { this.reviewFinalizedBy = reviewFinalizedBy; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
