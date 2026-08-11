package com.tendermatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "shadow_predictions")
public class ShadowPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false)
    private Integer modelVersion;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "rule_result", nullable = false, columnDefinition = "TEXT")
    private String ruleResult;

    @Column(name = "ml_result", nullable = false, columnDefinition = "TEXT")
    private String mlResult;

    @Column(name = "agreed", nullable = false)
    private Boolean agreed;

    @Column(name = "human_verdict", length = 20)
    private String humanVerdict;

    @Column(name = "rule_correct")
    private Boolean ruleCorrect;

    @Column(name = "ml_correct")
    private Boolean mlCorrect;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public ShadowPrediction() {}

    public ShadowPrediction(UUID id, String modelName, Integer modelVersion, String inputHash,
                            String ruleResult, String mlResult, Boolean agreed, String humanVerdict,
                            Boolean ruleCorrect, Boolean mlCorrect, ZonedDateTime createdAt) {
        this.id = id;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.inputHash = inputHash;
        this.ruleResult = ruleResult;
        this.mlResult = mlResult;
        this.agreed = agreed;
        this.humanVerdict = humanVerdict;
        this.ruleCorrect = ruleCorrect;
        this.mlCorrect = mlCorrect;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String modelName;
        private Integer modelVersion;
        private String inputHash;
        private String ruleResult;
        private String mlResult;
        private Boolean agreed;
        private String humanVerdict;
        private Boolean ruleCorrect;
        private Boolean mlCorrect;
        private ZonedDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder modelName(String modelName) { this.modelName = modelName; return this; }
        public Builder modelVersion(Integer version) { this.modelVersion = version; return this; }
        public Builder inputHash(String hash) { this.inputHash = hash; return this; }
        public Builder ruleResult(String ruleResult) { this.ruleResult = ruleResult; return this; }
        public Builder mlResult(String mlResult) { this.mlResult = mlResult; return this; }
        public Builder agreed(Boolean agreed) { this.agreed = agreed; return this; }
        public Builder humanVerdict(String verdict) { this.humanVerdict = verdict; return this; }
        public Builder ruleCorrect(Boolean correct) { this.ruleCorrect = correct; return this; }
        public Builder mlCorrect(Boolean correct) { this.mlCorrect = correct; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ShadowPrediction build() {
            return new ShadowPrediction(id, modelName, modelVersion, inputHash, ruleResult,
                    mlResult, agreed, humanVerdict, ruleCorrect, mlCorrect, createdAt);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Integer getModelVersion() { return modelVersion; }
    public void setModelVersion(Integer modelVersion) { this.modelVersion = modelVersion; }

    public String getInputHash() { return inputHash; }
    public void setInputHash(String inputHash) { this.inputHash = inputHash; }

    public String getRuleResult() { return ruleResult; }
    public void setRuleResult(String ruleResult) { this.ruleResult = ruleResult; }

    public String getMlResult() { return mlResult; }
    public void setMlResult(String mlResult) { this.mlResult = mlResult; }

    public Boolean getAgreed() { return agreed; }
    public void setAgreed(Boolean agreed) { this.agreed = agreed; }

    public String getHumanVerdict() { return humanVerdict; }
    public void setHumanVerdict(String humanVerdict) { this.humanVerdict = humanVerdict; }

    public Boolean getRuleCorrect() { return ruleCorrect; }
    public void setRuleCorrect(Boolean ruleCorrect) { this.ruleCorrect = ruleCorrect; }

    public Boolean getMlCorrect() { return mlCorrect; }
    public void setMlCorrect(Boolean mlCorrect) { this.mlCorrect = mlCorrect; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
