package com.tendermatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_scores")
public class VendorScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id", nullable = false)
    private Tender tender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_submission_id", nullable = false)
    private VendorSubmission vendorSubmission;

    @Column(name = "overall_score", nullable = false)
    private Double overallScore;

    @Column(name = "rank")
    private Integer rank;

    @Column(name = "total_specs", nullable = false)
    private Integer totalSpecs;

    @Column(name = "matched_count", nullable = false)
    private Integer matchedCount;

    @Column(name = "unmatched_count", nullable = false)
    private Integer unmatchedCount;

    @Column(name = "uncertain_count", nullable = false)
    private Integer uncertainCount;

    @Column(name = "product_scores", columnDefinition = "TEXT")
    private String productScores;

    @Column(name = "score_breakdown", columnDefinition = "TEXT")
    private String scoreBreakdown;

    @Column(name = "scoring_method", nullable = false, length = 50)
    private String scoringMethod = "weighted_average";

    @Column(name = "weights_snapshot", columnDefinition = "TEXT")
    private String weightsSnapshot;

    @CreationTimestamp
    @Column(name = "scored_at", nullable = false)
    private ZonedDateTime scoredAt;

    public VendorScore() {}

    public VendorScore(UUID id, Tender tender, VendorSubmission vendorSubmission, Double overallScore,
                       Integer rank, Integer totalSpecs, Integer matchedCount, Integer unmatchedCount,
                       Integer uncertainCount, String productScores, String scoreBreakdown,
                       String scoringMethod, String weightsSnapshot, ZonedDateTime scoredAt) {
        this.id = id;
        this.tender = tender;
        this.vendorSubmission = vendorSubmission;
        this.overallScore = overallScore;
        this.rank = rank;
        this.totalSpecs = totalSpecs;
        this.matchedCount = matchedCount;
        this.unmatchedCount = unmatchedCount;
        this.uncertainCount = uncertainCount;
        this.productScores = productScores;
        this.scoreBreakdown = scoreBreakdown;
        this.scoringMethod = scoringMethod;
        this.weightsSnapshot = weightsSnapshot;
        this.scoredAt = scoredAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Tender tender;
        private VendorSubmission vendorSubmission;
        private Double overallScore;
        private Integer rank;
        private Integer totalSpecs;
        private Integer matchedCount;
        private Integer unmatchedCount;
        private Integer uncertainCount;
        private String productScores;
        private String scoreBreakdown;
        private String scoringMethod = "weighted_average";
        private String weightsSnapshot;
        private ZonedDateTime scoredAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tender(Tender tender) { this.tender = tender; return this; }
        public Builder vendorSubmission(VendorSubmission vendorSubmission) { this.vendorSubmission = vendorSubmission; return this; }
        public Builder overallScore(Double overallScore) { this.overallScore = overallScore; return this; }
        public Builder rank(Integer rank) { this.rank = rank; return this; }
        public Builder totalSpecs(Integer totalSpecs) { this.totalSpecs = totalSpecs; return this; }
        public Builder matchedCount(Integer matchedCount) { this.matchedCount = matchedCount; return this; }
        public Builder unmatchedCount(Integer unmatchedCount) { this.unmatchedCount = unmatchedCount; return this; }
        public Builder uncertainCount(Integer uncertainCount) { this.uncertainCount = uncertainCount; return this; }
        public Builder productScores(String productScores) { this.productScores = productScores; return this; }
        public Builder scoreBreakdown(String scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; return this; }
        public Builder scoringMethod(String scoringMethod) { this.scoringMethod = scoringMethod; return this; }
        public Builder weightsSnapshot(String weightsSnapshot) { this.weightsSnapshot = weightsSnapshot; return this; }
        public Builder scoredAt(ZonedDateTime scoredAt) { this.scoredAt = scoredAt; return this; }

        public VendorScore build() {
            return new VendorScore(id, tender, vendorSubmission, overallScore, rank, totalSpecs,
                    matchedCount, unmatchedCount, uncertainCount, productScores, scoreBreakdown,
                    scoringMethod, weightsSnapshot, scoredAt);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tender getTender() { return tender; }
    public void setTender(Tender tender) { this.tender = tender; }

    public VendorSubmission getVendorSubmission() { return vendorSubmission; }
    public void setVendorSubmission(VendorSubmission vendorSubmission) { this.vendorSubmission = vendorSubmission; }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public Integer getTotalSpecs() { return totalSpecs; }
    public void setTotalSpecs(Integer totalSpecs) { this.totalSpecs = totalSpecs; }

    public Integer getMatchedCount() { return matchedCount; }
    public void setMatchedCount(Integer matchedCount) { this.matchedCount = matchedCount; }

    public Integer getUnmatchedCount() { return unmatchedCount; }
    public void setUnmatchedCount(Integer unmatchedCount) { this.unmatchedCount = unmatchedCount; }

    public Integer getUncertainCount() { return uncertainCount; }
    public void setUncertainCount(Integer uncertainCount) { this.uncertainCount = uncertainCount; }

    public String getProductScores() { return productScores; }
    public void setProductScores(String productScores) { this.productScores = productScores; }

    public String getScoreBreakdown() { return scoreBreakdown; }
    public void setScoreBreakdown(String scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }

    public String getScoringMethod() { return scoringMethod; }
    public void setScoringMethod(String scoringMethod) { this.scoringMethod = scoringMethod; }

    public String getWeightsSnapshot() { return weightsSnapshot; }
    public void setWeightsSnapshot(String weightsSnapshot) { this.weightsSnapshot = weightsSnapshot; }

    public ZonedDateTime getScoredAt() { return scoredAt; }
    public void setScoredAt(ZonedDateTime scoredAt) { this.scoredAt = scoredAt; }
}
