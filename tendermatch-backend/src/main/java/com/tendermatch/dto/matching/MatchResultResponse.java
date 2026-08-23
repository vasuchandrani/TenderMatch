package com.tendermatch.dto.matching;

import com.tendermatch.dto.pipeline.ExtractedSpecDto;
import com.tendermatch.model.MatchResult;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MatchResultResponse {

    private UUID id;
    private UUID tenderId;
    private UUID vendorSubmissionId;
    private String vendorName;
    private UUID tenderProductId;
    private String tenderProductName;
    private ExtractedSpecDto requirementSpec;
    private ExtractedSpecDto vendorSpec;
    private String comparisonOperator;
    private String comparisonMethod;
    private Double crossEncoderScore;
    private String llmVerdict;
    private String llmReasoning;
    private String systemVerdict;
    private Double confidenceScore;
    private String details;
    private String humanVerdict;
    private ZonedDateTime reviewedAt;
    private ZonedDateTime createdAt;

    public MatchResultResponse() {}

    public static MatchResultResponse fromEntity(MatchResult mr) {
        if (mr == null) return null;
        MatchResultResponse dto = new MatchResultResponse();
        dto.setId(mr.getId());
        dto.setTenderId(mr.getTender() != null ? mr.getTender().getId() : null);
        dto.setVendorSubmissionId(mr.getVendorSubmission() != null ? mr.getVendorSubmission().getId() : null);
        dto.setVendorName(mr.getVendorSubmission() != null ? mr.getVendorSubmission().getVendorName() : null);
        dto.setTenderProductId(mr.getTenderProduct() != null ? mr.getTenderProduct().getId() : null);
        dto.setTenderProductName(mr.getTenderProduct() != null ? mr.getTenderProduct().getProductName() : null);

        if (mr.getTenderSpec() != null) {
            dto.setRequirementSpec(ExtractedSpecDto.fromTenderSpec(mr.getTenderSpec()));
        }
        if (mr.getVendorSpec() != null) {
            dto.setVendorSpec(ExtractedSpecDto.fromVendorSpec(mr.getVendorSpec()));
        }

        dto.setComparisonOperator(mr.getComparisonOperator());
        dto.setComparisonMethod(mr.getComparisonMethod());
        dto.setCrossEncoderScore(mr.getCrossEncoderScore());
        dto.setLlmVerdict(mr.getLlmVerdict());
        dto.setLlmReasoning(mr.getLlmReasoning());
        dto.setSystemVerdict(mr.getSystemVerdict());
        dto.setConfidenceScore(mr.getConfidenceScore());
        dto.setDetails(mr.getDetails());
        dto.setHumanVerdict(mr.getHumanVerdict());
        dto.setReviewedAt(mr.getReviewedAt());
        dto.setCreatedAt(mr.getCreatedAt());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenderId() { return tenderId; }
    public void setTenderId(UUID tenderId) { this.tenderId = tenderId; }

    public UUID getVendorSubmissionId() { return vendorSubmissionId; }
    public void setVendorSubmissionId(UUID vendorSubmissionId) { this.vendorSubmissionId = vendorSubmissionId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public UUID getTenderProductId() { return tenderProductId; }
    public void setTenderProductId(UUID tenderProductId) { this.tenderProductId = tenderProductId; }

    public String getTenderProductName() { return tenderProductName; }
    public void setTenderProductName(String tenderProductName) { this.tenderProductName = tenderProductName; }

    public ExtractedSpecDto getRequirementSpec() { return requirementSpec; }
    public void setRequirementSpec(ExtractedSpecDto requirementSpec) { this.requirementSpec = requirementSpec; }

    public ExtractedSpecDto getVendorSpec() { return vendorSpec; }
    public void setVendorSpec(ExtractedSpecDto vendorSpec) { this.vendorSpec = vendorSpec; }

    public String getComparisonOperator() { return comparisonOperator; }
    public void setComparisonOperator(String comparisonOperator) { this.comparisonOperator = comparisonOperator; }

    public String getComparisonMethod() { return comparisonMethod; }
    public void setComparisonMethod(String comparisonMethod) { this.comparisonMethod = comparisonMethod; }

    public Double getCrossEncoderScore() { return crossEncoderScore; }
    public void setCrossEncoderScore(Double crossEncoderScore) { this.crossEncoderScore = crossEncoderScore; }

    public String getLlmVerdict() { return llmVerdict; }
    public void setLlmVerdict(String llmVerdict) { this.llmVerdict = llmVerdict; }

    public String getLlmReasoning() { return llmReasoning; }
    public void setLlmReasoning(String llmReasoning) { this.llmReasoning = llmReasoning; }

    public String getSystemVerdict() { return systemVerdict; }
    public void setSystemVerdict(String systemVerdict) { this.systemVerdict = systemVerdict; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getHumanVerdict() { return humanVerdict; }
    public void setHumanVerdict(String humanVerdict) { this.humanVerdict = humanVerdict; }

    public ZonedDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(ZonedDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
