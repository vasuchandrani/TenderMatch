package com.tendermatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "scoring_weights")
public class ScoringWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id")
    private Tender tender;

    @Column(name = "template_name")
    private String templateName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "canonical_spec_name", nullable = false, length = 500)
    private String canonicalSpecName;

    @Column(name = "weight_category", nullable = false, length = 50)
    private String weightCategory = "medium";

    @Column(name = "weight_value", nullable = false)
    private Double weightValue = 1.0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public ScoringWeight() {}

    public ScoringWeight(UUID id, Tender tender, String templateName, User user,
                         String canonicalSpecName, String weightCategory, Double weightValue,
                         ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.tender = tender;
        this.templateName = templateName;
        this.user = user;
        this.canonicalSpecName = canonicalSpecName;
        this.weightCategory = weightCategory;
        this.weightValue = weightValue;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private Tender tender;
        private String templateName;
        private User user;
        private String canonicalSpecName;
        private String weightCategory = "medium";
        private Double weightValue = 1.0;
        private ZonedDateTime createdAt;
        private ZonedDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder tender(Tender tender) { this.tender = tender; return this; }
        public Builder templateName(String templateName) { this.templateName = templateName; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder canonicalSpecName(String canonicalSpecName) { this.canonicalSpecName = canonicalSpecName; return this; }
        public Builder weightCategory(String weightCategory) { this.weightCategory = weightCategory; return this; }
        public Builder weightValue(Double weightValue) { this.weightValue = weightValue; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ScoringWeight build() {
            return new ScoringWeight(id, tender, templateName, user, canonicalSpecName,
                    weightCategory, weightValue, createdAt, updatedAt);
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tender getTender() { return tender; }
    public void setTender(Tender tender) { this.tender = tender; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getCanonicalSpecName() { return canonicalSpecName; }
    public void setCanonicalSpecName(String canonicalSpecName) { this.canonicalSpecName = canonicalSpecName; }

    public String getWeightCategory() { return weightCategory; }
    public void setWeightCategory(String weightCategory) { this.weightCategory = weightCategory; }

    public Double getWeightValue() { return weightValue; }
    public void setWeightValue(Double weightValue) { this.weightValue = weightValue; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
