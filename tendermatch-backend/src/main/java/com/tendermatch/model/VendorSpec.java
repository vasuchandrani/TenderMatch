package com.tendermatch.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_specs")
public class VendorSpec {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id", nullable = false)
    @JsonIgnore
    private VendorProduct vendorProduct;

    @Column(name = "raw_spec_name", nullable = false, length = 500)
    private String rawSpecName;

    @Column(name = "canonical_spec_name", length = 500)
    private String canonicalSpecName;

    @Column(name = "spec_value", nullable = false, columnDefinition = "TEXT")
    private String specValue;

    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "is_numeric", nullable = false)
    private Boolean isNumeric = false;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "extraction_confidence", length = 20)
    private String extractionConfidence = "high";

    @Column(name = "extraction_source", length = 20)
    private String extractionSource = "llm";

    @Column(name = "normalization_confidence", length = 20)
    private String normalizationConfidence = "high";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_tender_spec_id")
    private TenderSpec matchedTenderSpec;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public VendorSpec() {}

    public VendorSpec(UUID id, VendorProduct vendorProduct, String rawSpecName, String canonicalSpecName,
                      String specValue, Double numericValue, Double minValue, Double maxValue,
                      String unit, Boolean isNumeric, String rawText, String extractionConfidence,
                      String extractionSource, String normalizationConfidence, TenderSpec matchedTenderSpec,
                      Integer displayOrder, ZonedDateTime createdAt) {
        this.id = id;
        this.vendorProduct = vendorProduct;
        this.rawSpecName = rawSpecName;
        this.canonicalSpecName = canonicalSpecName;
        this.specValue = specValue;
        this.numericValue = numericValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unit = unit;
        this.isNumeric = isNumeric != null ? isNumeric : false;
        this.rawText = rawText;
        this.extractionConfidence = extractionConfidence != null ? extractionConfidence : "high";
        this.extractionSource = extractionSource != null ? extractionSource : "llm";
        this.normalizationConfidence = normalizationConfidence != null ? normalizationConfidence : "high";
        this.matchedTenderSpec = matchedTenderSpec;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public VendorProduct getVendorProduct() { return vendorProduct; }
    public void setVendorProduct(VendorProduct vendorProduct) { this.vendorProduct = vendorProduct; }

    public String getRawSpecName() { return rawSpecName; }
    public void setRawSpecName(String rawSpecName) { this.rawSpecName = rawSpecName; }

    public String getCanonicalSpecName() { return canonicalSpecName; }
    public void setCanonicalSpecName(String canonicalSpecName) { this.canonicalSpecName = canonicalSpecName; }

    public String getSpecValue() { return specValue; }
    public void setSpecValue(String specValue) { this.specValue = specValue; }

    public Double getNumericValue() { return numericValue; }
    public void setNumericValue(Double numericValue) { this.numericValue = numericValue; }

    public Double getMinValue() { return minValue; }
    public void setMinValue(Double minValue) { this.minValue = minValue; }

    public Double getMaxValue() { return maxValue; }
    public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Boolean getIsNumeric() { return isNumeric; }
    public void setIsNumeric(Boolean isNumeric) { this.isNumeric = isNumeric; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getExtractionConfidence() { return extractionConfidence; }
    public void setExtractionConfidence(String extractionConfidence) { this.extractionConfidence = extractionConfidence; }

    public String getExtractionSource() { return extractionSource; }
    public void setExtractionSource(String extractionSource) { this.extractionSource = extractionSource; }

    public String getNormalizationConfidence() { return normalizationConfidence; }
    public void setNormalizationConfidence(String normalizationConfidence) { this.normalizationConfidence = normalizationConfidence; }

    public TenderSpec getMatchedTenderSpec() { return matchedTenderSpec; }
    public void setMatchedTenderSpec(TenderSpec matchedTenderSpec) { this.matchedTenderSpec = matchedTenderSpec; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
