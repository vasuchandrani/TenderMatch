package com.tendermatch.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_results")
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id", nullable = false)
    @JsonIgnore
    private Tender tender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_submission_id", nullable = false)
    @JsonIgnore
    private VendorSubmission vendorSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_spec_id", nullable = false)
    private TenderSpec tenderSpec;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_spec_id")
    private VendorSpec vendorSpec;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_product_id", nullable = false)
    @JsonIgnore
    private TenderProduct tenderProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id")
    @JsonIgnore
    private VendorProduct vendorProduct;

    @Column(name = "comparison_operator", length = 20)
    private String comparisonOperator;

    @Column(name = "comparison_method", length = 50)
    private String comparisonMethod;

    @Column(name = "cross_encoder_score")
    private Double crossEncoderScore;

    @Column(name = "llm_verdict", length = 20)
    private String llmVerdict;

    @Column(name = "llm_reasoning", columnDefinition = "TEXT")
    private String llmReasoning;

    @Column(name = "numeric_comparison", columnDefinition = "TEXT")
    private String numericComparison;

    @Column(name = "system_verdict", nullable = false, length = 20)
    private String systemVerdict; // Match, Unmatch, AI Err

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @Column(name = "confidence_features", columnDefinition = "TEXT")
    private String confidenceFeatures;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "human_verdict", length = 20)
    private String humanVerdict;

    @Column(name = "reviewed_at")
    private ZonedDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    @JsonIgnore
    private User reviewedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public MatchResult() {}

    public MatchResult(UUID id, Tender tender, VendorSubmission vendorSubmission,
                       TenderSpec tenderSpec, VendorSpec vendorSpec,
                       TenderProduct tenderProduct, VendorProduct vendorProduct,
                       String comparisonOperator, String comparisonMethod,
                       Double crossEncoderScore, String llmVerdict, String llmReasoning,
                       String numericComparison, String systemVerdict, Double confidenceScore,
                       String confidenceFeatures, String details, String humanVerdict,
                       ZonedDateTime reviewedAt, User reviewedBy, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.tender = tender;
        this.vendorSubmission = vendorSubmission;
        this.tenderSpec = tenderSpec;
        this.vendorSpec = vendorSpec;
        this.tenderProduct = tenderProduct;
        this.vendorProduct = vendorProduct;
        this.comparisonOperator = comparisonOperator;
        this.comparisonMethod = comparisonMethod;
        this.crossEncoderScore = crossEncoderScore;
        this.llmVerdict = llmVerdict;
        this.llmReasoning = llmReasoning;
        this.numericComparison = numericComparison;
        this.systemVerdict = systemVerdict;
        this.confidenceScore = confidenceScore;
        this.confidenceFeatures = confidenceFeatures;
        this.details = details;
        this.humanVerdict = humanVerdict;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Tender tender;
        private VendorSubmission vendorSubmission;
        private TenderSpec tenderSpec;
        private VendorSpec vendorSpec;
        private TenderProduct tenderProduct;
        private VendorProduct vendorProduct;
        private String comparisonOperator;
        private String comparisonMethod;
        private Double crossEncoderScore;
        private String llmVerdict;
        private String llmReasoning;
        private String numericComparison;
        private String systemVerdict;
        private Double confidenceScore;
        private String confidenceFeatures;
        private String details;
        private String humanVerdict;
        private ZonedDateTime reviewedAt;
        private User reviewedBy;
        private ZonedDateTime createdAt;
        private ZonedDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tender(Tender tender) { this.tender = tender; return this; }
        public Builder vendorSubmission(VendorSubmission vendorSubmission) { this.vendorSubmission = vendorSubmission; return this; }
        public Builder tenderSpec(TenderSpec tenderSpec) { this.tenderSpec = tenderSpec; return this; }
        public Builder vendorSpec(VendorSpec vendorSpec) { this.vendorSpec = vendorSpec; return this; }
        public Builder tenderProduct(TenderProduct tenderProduct) { this.tenderProduct = tenderProduct; return this; }
        public Builder vendorProduct(VendorProduct vendorProduct) { this.vendorProduct = vendorProduct; return this; }
        public Builder comparisonOperator(String comparisonOperator) { this.comparisonOperator = comparisonOperator; return this; }
        public Builder comparisonMethod(String comparisonMethod) { this.comparisonMethod = comparisonMethod; return this; }
        public Builder crossEncoderScore(Double crossEncoderScore) { this.crossEncoderScore = crossEncoderScore; return this; }
        public Builder llmVerdict(String llmVerdict) { this.llmVerdict = llmVerdict; return this; }
        public Builder llmReasoning(String llmReasoning) { this.llmReasoning = llmReasoning; return this; }
        public Builder numericComparison(String numericComparison) { this.numericComparison = numericComparison; return this; }
        public Builder systemVerdict(String systemVerdict) { this.systemVerdict = systemVerdict; return this; }
        public Builder confidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; return this; }
        public Builder confidenceFeatures(String confidenceFeatures) { this.confidenceFeatures = confidenceFeatures; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder humanVerdict(String humanVerdict) { this.humanVerdict = humanVerdict; return this; }
        public Builder reviewedAt(ZonedDateTime reviewedAt) { this.reviewedAt = reviewedAt; return this; }
        public Builder reviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public MatchResult build() {
            return new MatchResult(id, tender, vendorSubmission, tenderSpec, vendorSpec,
                    tenderProduct, vendorProduct, comparisonOperator, comparisonMethod,
                    crossEncoderScore, llmVerdict, llmReasoning, numericComparison,
                    systemVerdict, confidenceScore, confidenceFeatures, details,
                    humanVerdict, reviewedAt, reviewedBy, createdAt, updatedAt);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tender getTender() { return tender; }
    public void setTender(Tender tender) { this.tender = tender; }

    public VendorSubmission getVendorSubmission() { return vendorSubmission; }
    public void setVendorSubmission(VendorSubmission vendorSubmission) { this.vendorSubmission = vendorSubmission; }

    public TenderSpec getTenderSpec() { return tenderSpec; }
    public void setTenderSpec(TenderSpec tenderSpec) { this.tenderSpec = tenderSpec; }

    public VendorSpec getVendorSpec() { return vendorSpec; }
    public void setVendorSpec(VendorSpec vendorSpec) { this.vendorSpec = vendorSpec; }

    public TenderProduct getTenderProduct() { return tenderProduct; }
    public void setTenderProduct(TenderProduct tenderProduct) { this.tenderProduct = tenderProduct; }

    public VendorProduct getVendorProduct() { return vendorProduct; }
    public void setVendorProduct(VendorProduct vendorProduct) { this.vendorProduct = vendorProduct; }

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

    public String getNumericComparison() { return numericComparison; }
    public void setNumericComparison(String numericComparison) { this.numericComparison = numericComparison; }

    public String getSystemVerdict() { return systemVerdict; }
    public void setSystemVerdict(String systemVerdict) { this.systemVerdict = systemVerdict; }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }

    public String getConfidenceFeatures() { return confidenceFeatures; }
    public void setConfidenceFeatures(String confidenceFeatures) { this.confidenceFeatures = confidenceFeatures; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getHumanVerdict() { return humanVerdict; }
    public void setHumanVerdict(String humanVerdict) { this.humanVerdict = humanVerdict; }

    public ZonedDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(ZonedDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
