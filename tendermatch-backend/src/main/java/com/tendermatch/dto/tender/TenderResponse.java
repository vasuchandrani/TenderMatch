package com.tendermatch.dto.tender;

import com.tendermatch.model.Tender;
import com.tendermatch.model.enums.TenderStatus;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TenderResponse {
    private UUID id;
    private String name;
    private String description;
    private TenderStatus status;
    private String requirementFileName;
    private Long requirementFileSize;
    private String requirementFileKey;
    private List<VendorSubmissionResponse> vendorSubmissions = new ArrayList<>();
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public TenderResponse() {}

    public TenderResponse(UUID id, String name, String description, TenderStatus status,
                          String requirementFileName, Long requirementFileSize, String requirementFileKey,
                          List<VendorSubmissionResponse> vendorSubmissions, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.requirementFileName = requirementFileName;
        this.requirementFileSize = requirementFileSize;
        this.requirementFileKey = requirementFileKey;
        this.vendorSubmissions = vendorSubmissions != null ? vendorSubmissions : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TenderResponse fromEntity(Tender entity) {
        List<VendorSubmissionResponse> subs = entity.getVendorSubmissions() != null
                ? entity.getVendorSubmissions().stream()
                .map(VendorSubmissionResponse::fromEntity)
                .collect(Collectors.toList())
                : new ArrayList<>();

        return new TenderResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getRequirementFileName(),
                entity.getRequirementFileSize(),
                entity.getRequirementFileKey(),
                subs,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String name;
        private String description;
        private TenderStatus status;
        private String requirementFileName;
        private Long requirementFileSize;
        private String requirementFileKey;
        private List<VendorSubmissionResponse> vendorSubmissions = new ArrayList<>();
        private ZonedDateTime createdAt;
        private ZonedDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder status(TenderStatus status) { this.status = status; return this; }
        public Builder requirementFileName(String name) { this.requirementFileName = name; return this; }
        public Builder requirementFileSize(Long size) { this.requirementFileSize = size; return this; }
        public Builder requirementFileKey(String key) { this.requirementFileKey = key; return this; }
        public Builder vendorSubmissions(List<VendorSubmissionResponse> list) { this.vendorSubmissions = list; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public TenderResponse build() {
            return new TenderResponse(id, name, description, status, requirementFileName, requirementFileSize, requirementFileKey, vendorSubmissions, createdAt, updatedAt);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TenderStatus getStatus() { return status; }
    public void setStatus(TenderStatus status) { this.status = status; }

    public String getRequirementFileName() { return requirementFileName; }
    public void setRequirementFileName(String requirementFileName) { this.requirementFileName = requirementFileName; }

    public Long getRequirementFileSize() { return requirementFileSize; }
    public void setRequirementFileSize(Long requirementFileSize) { this.requirementFileSize = requirementFileSize; }

    public String getRequirementFileKey() { return requirementFileKey; }
    public void setRequirementFileKey(String requirementFileKey) { this.requirementFileKey = requirementFileKey; }

    public List<VendorSubmissionResponse> getVendorSubmissions() { return vendorSubmissions; }
    public void setVendorSubmissions(List<VendorSubmissionResponse> vendorSubmissions) { this.vendorSubmissions = vendorSubmissions; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
