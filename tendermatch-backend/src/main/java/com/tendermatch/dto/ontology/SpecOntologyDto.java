package com.tendermatch.dto.ontology;

import com.tendermatch.model.SpecOntology;

import java.time.ZonedDateTime;
import java.util.UUID;

public class SpecOntologyDto {
    private UUID id;
    private String canonicalName;
    private String displayName;
    private String category;
    private String defaultUnit;
    private String defaultOperator;
    private Integer usageCount;
    private Boolean humanVerified;
    private String[] synonyms;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public SpecOntologyDto() {}

    public SpecOntologyDto(UUID id, String canonicalName, String displayName, String category,
                           String defaultUnit, String defaultOperator, Integer usageCount,
                           Boolean humanVerified, String[] synonyms,
                           ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.id = id;
        this.canonicalName = canonicalName;
        this.displayName = displayName;
        this.category = category;
        this.defaultUnit = defaultUnit;
        this.defaultOperator = defaultOperator;
        this.usageCount = usageCount;
        this.humanVerified = humanVerified;
        this.synonyms = synonyms;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SpecOntologyDto fromEntity(SpecOntology entity) {
        if (entity == null) return null;
        return new SpecOntologyDto(
                entity.getId(),
                entity.getCanonicalName(),
                entity.getDisplayName(),
                entity.getCategory(),
                entity.getDefaultUnit(),
                entity.getDefaultOperator(),
                entity.getUsageCount(),
                entity.getHumanVerified(),
                entity.getSynonyms(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
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
}
