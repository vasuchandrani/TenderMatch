package com.tendermatch.model;

import com.tendermatch.model.enums.TenderStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tenders")
public class Tender {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TenderStatus status = TenderStatus.CREATED;

    @Column(name = "processing_stage", length = 100)
    private String processingStage = "IDLE";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "requirement_file_key", length = 1000)
    private String requirementFileKey;

    @Column(name = "requirement_file_name", length = 500)
    private String requirementFileName;

    @Column(name = "requirement_file_size")
    private Long requirementFileSize;

    @OneToMany(mappedBy = "tender", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorSubmission> vendorSubmissions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    public Tender() {}

    public Tender(UUID id, User user, String name, String description, TenderStatus status,
                  String requirementFileKey, String requirementFileName, Long requirementFileSize,
                  List<VendorSubmission> vendorSubmissions, ZonedDateTime createdAt, ZonedDateTime updatedAt, ZonedDateTime deletedAt) {
        this.id = id;
        this.user = user;
        this.name = name;
        this.description = description;
        this.status = status != null ? status : TenderStatus.CREATED;
        this.requirementFileKey = requirementFileKey;
        this.requirementFileName = requirementFileName;
        this.requirementFileSize = requirementFileSize;
        this.vendorSubmissions = vendorSubmissions != null ? vendorSubmissions : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private User user;
        private String name;
        private String description;
        private TenderStatus status = TenderStatus.CREATED;
        private String processingStage = "IDLE";
        private String errorMessage;
        private String requirementFileKey;
        private String requirementFileName;
        private Long requirementFileSize;
        private List<VendorSubmission> vendorSubmissions = new ArrayList<>();
        private ZonedDateTime createdAt;
        private ZonedDateTime updatedAt;
        private ZonedDateTime deletedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder status(TenderStatus status) { this.status = status; return this; }
        public Builder processingStage(String processingStage) { this.processingStage = processingStage; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder requirementFileKey(String key) { this.requirementFileKey = key; return this; }
        public Builder requirementFileName(String name) { this.requirementFileName = name; return this; }
        public Builder requirementFileSize(Long size) { this.requirementFileSize = size; return this; }
        public Builder vendorSubmissions(List<VendorSubmission> list) { this.vendorSubmissions = list; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder deletedAt(ZonedDateTime deletedAt) { this.deletedAt = deletedAt; return this; }

        public Tender build() {
            Tender t = new Tender(id, user, name, description, status, requirementFileKey, requirementFileName, requirementFileSize, vendorSubmissions, createdAt, updatedAt, deletedAt);
            t.setProcessingStage(processingStage);
            t.setErrorMessage(errorMessage);
            return t;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TenderStatus getStatus() { return status; }
    public void setStatus(TenderStatus status) { this.status = status; }

    public String getProcessingStage() { return processingStage; }
    public void setProcessingStage(String processingStage) { this.processingStage = processingStage; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getRequirementFileKey() { return requirementFileKey; }
    public void setRequirementFileKey(String requirementFileKey) { this.requirementFileKey = requirementFileKey; }

    public String getRequirementFileName() { return requirementFileName; }
    public void setRequirementFileName(String requirementFileName) { this.requirementFileName = requirementFileName; }

    public Long getRequirementFileSize() { return requirementFileSize; }
    public void setRequirementFileSize(Long requirementFileSize) { this.requirementFileSize = requirementFileSize; }

    public List<VendorSubmission> getVendorSubmissions() { return vendorSubmissions; }
    public void setVendorSubmissions(List<VendorSubmission> vendorSubmissions) { this.vendorSubmissions = vendorSubmissions; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }

    public ZonedDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(ZonedDateTime deletedAt) { this.deletedAt = deletedAt; }
}
