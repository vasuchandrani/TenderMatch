package com.tendermatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "model_registry", uniqueConstraints = {
        @UniqueConstraint(name = "uk_model_registry_name_version", columnNames = {"model_name", "version"})
})
public class ModelRegistryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "model_path", nullable = false, length = 1000)
    private String modelPath;

    @Column(name = "training_data_count", nullable = false)
    private Integer trainingDataCount;

    @Column(name = "trained_at", nullable = false)
    private ZonedDateTime trainedAt;

    @Column(name = "training_duration_seconds")
    private Integer trainingDurationSeconds;

    @Column(name = "eval_metrics", nullable = false, columnDefinition = "TEXT")
    private String evalMetrics;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "trained";

    @Column(name = "promoted_at")
    private ZonedDateTime promotedAt;

    @Column(name = "retired_at")
    private ZonedDateTime retiredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public ModelRegistryEntry() {}

    public ModelRegistryEntry(UUID id, String modelName, Integer version, String modelPath,
                              Integer trainingDataCount, ZonedDateTime trainedAt,
                              Integer trainingDurationSeconds, String evalMetrics, String status,
                              ZonedDateTime promotedAt, ZonedDateTime retiredAt, ZonedDateTime createdAt) {
        this.id = id;
        this.modelName = modelName;
        this.version = version;
        this.modelPath = modelPath;
        this.trainingDataCount = trainingDataCount;
        this.trainedAt = trainedAt;
        this.trainingDurationSeconds = trainingDurationSeconds;
        this.evalMetrics = evalMetrics;
        this.status = status;
        this.promotedAt = promotedAt;
        this.retiredAt = retiredAt;
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String modelName;
        private Integer version;
        private String modelPath;
        private Integer trainingDataCount;
        private ZonedDateTime trainedAt;
        private Integer trainingDurationSeconds;
        private String evalMetrics;
        private String status = "trained";
        private ZonedDateTime promotedAt;
        private ZonedDateTime retiredAt;
        private ZonedDateTime createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder modelName(String modelName) { this.modelName = modelName; return this; }
        public Builder version(Integer version) { this.version = version; return this; }
        public Builder modelPath(String modelPath) { this.modelPath = modelPath; return this; }
        public Builder trainingDataCount(Integer count) { this.trainingDataCount = count; return this; }
        public Builder trainedAt(ZonedDateTime trainedAt) { this.trainedAt = trainedAt; return this; }
        public Builder trainingDurationSeconds(Integer sec) { this.trainingDurationSeconds = sec; return this; }
        public Builder evalMetrics(String evalMetrics) { this.evalMetrics = evalMetrics; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder promotedAt(ZonedDateTime promotedAt) { this.promotedAt = promotedAt; return this; }
        public Builder retiredAt(ZonedDateTime retiredAt) { this.retiredAt = retiredAt; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ModelRegistryEntry build() {
            return new ModelRegistryEntry(id, modelName, version, modelPath, trainingDataCount,
                    trainedAt, trainingDurationSeconds, evalMetrics, status, promotedAt, retiredAt, createdAt);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }

    public Integer getTrainingDataCount() { return trainingDataCount; }
    public void setTrainingDataCount(Integer trainingDataCount) { this.trainingDataCount = trainingDataCount; }

    public ZonedDateTime getTrainedAt() { return trainedAt; }
    public void setTrainedAt(ZonedDateTime trainedAt) { this.trainedAt = trainedAt; }

    public Integer getTrainingDurationSeconds() { return trainingDurationSeconds; }
    public void setTrainingDurationSeconds(Integer trainingDurationSeconds) { this.trainingDurationSeconds = trainingDurationSeconds; }

    public String getEvalMetrics() { return evalMetrics; }
    public void setEvalMetrics(String evalMetrics) { this.evalMetrics = evalMetrics; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ZonedDateTime getPromotedAt() { return promotedAt; }
    public void setPromotedAt(ZonedDateTime promotedAt) { this.promotedAt = promotedAt; }

    public ZonedDateTime getRetiredAt() { return retiredAt; }
    public void setRetiredAt(ZonedDateTime retiredAt) { this.retiredAt = retiredAt; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
