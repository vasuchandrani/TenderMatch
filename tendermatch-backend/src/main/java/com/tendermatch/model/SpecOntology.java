package com.tendermatch.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "spec_ontology")
public class SpecOntology {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "canonical_name", nullable = false, unique = true, length = 500)
    private String canonicalName;

    @Column(name = "display_name", nullable = false, length = 500)
    private String displayName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "default_unit", length = 50)
    private String defaultUnit;

    @Column(name = "default_operator", length = 10)
    private String defaultOperator = ">=";

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "human_verified", nullable = false)
    private Boolean humanVerified = false;

    @Column(name = "synonyms")
    private String[] synonyms = new String[0];

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    public SpecOntology() {}

    public SpecOntology(UUID id, String canonicalName, String displayName, String category,
                        String defaultUnit, String defaultOperator, Integer usageCount,
                        Boolean humanVerified, String[] synonyms,
                        ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.canonicalName = canonicalName;
        this.displayName = displayName;
        this.category = category;
        this.defaultUnit = defaultUnit;
        this.defaultOperator = defaultOperator != null ? defaultOperator : ">=";
        this.usageCount = usageCount != null ? usageCount : 0;
        this.humanVerified = humanVerified != null ? humanVerified : false;
        this.synonyms = synonyms != null ? synonyms : new String[0];
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public SpecOntology(UUID id, String canonicalName, String displayName, String category,
                        String defaultUnit, String defaultOperator, Integer usageCount,
                        ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this(id, canonicalName, displayName, category, defaultUnit, defaultOperator, usageCount, false, new String[0], createdAt, updatedAt);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCanonicalName() { return canonicalName; }
    public void setCanonicalName(String canonicalName) { this.canonicalName = canonicalName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDefaultUnit() { return defaultUnit; }
    public void setDefaultUnit(String defaultUnit) { this.defaultUnit = defaultUnit; }

    public String getDefaultOperator() { return defaultOperator; }
    public void setDefaultOperator(String defaultOperator) { this.defaultOperator = defaultOperator; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    public Boolean getHumanVerified() { return humanVerified; }
    public void setHumanVerified(Boolean humanVerified) { this.humanVerified = humanVerified; }

    public String[] getSynonyms() { return synonyms; }
    public void setSynonyms(String[] synonyms) { this.synonyms = synonyms; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String canonicalName;
        private String displayName;
        private String category;
        private String defaultUnit;
        private String defaultOperator = ">=";
        private Integer usageCount = 0;
        private Boolean humanVerified = false;
        private String[] synonyms = new String[0];
        private ZonedDateTime createdAt;
        private ZonedDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder canonicalName(String canonicalName) { this.canonicalName = canonicalName; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder defaultUnit(String defaultUnit) { this.defaultUnit = defaultUnit; return this; }
        public Builder defaultOperator(String defaultOperator) { this.defaultOperator = defaultOperator; return this; }
        public Builder usageCount(Integer usageCount) { this.usageCount = usageCount; return this; }
        public Builder humanVerified(Boolean humanVerified) { this.humanVerified = humanVerified; return this; }
        public Builder synonyms(String[] synonyms) { this.synonyms = synonyms; return this; }
        public Builder createdAt(ZonedDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public SpecOntology build() {
            return new SpecOntology(id, canonicalName, displayName, category, defaultUnit, defaultOperator, usageCount, humanVerified, synonyms, createdAt, updatedAt);
        }
    }
}
