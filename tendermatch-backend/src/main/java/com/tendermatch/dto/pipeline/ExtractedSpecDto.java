package com.tendermatch.dto.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tendermatch.model.TenderSpec;
import com.tendermatch.model.VendorSpec;

import java.util.UUID;

public class ExtractedSpecDto {

    private UUID id;

    @JsonProperty("raw_spec_name")
    private String rawSpecName;

    @JsonProperty("canonical_spec_name")
    private String canonicalSpecName;

    @JsonProperty("spec_value")
    private String specValue;

    @JsonProperty("numeric_value")
    private Double numericValue;

    @JsonProperty("min_value")
    private Double minValue;

    @JsonProperty("max_value")
    private Double maxValue;

    private String unit;

    @JsonProperty("is_numeric")
    private Boolean isNumeric;

    @JsonProperty("raw_text")
    private String rawText;

    @JsonProperty("extraction_confidence")
    private String extractionConfidence;

    @JsonProperty("extraction_source")
    private String extractionSource;

    @JsonProperty("normalization_confidence")
    private String normalizationConfidence;

    @JsonProperty("matched_tender_spec_id")
    private String matchedTenderSpecId;

    @JsonProperty("display_order")
    private Integer displayOrder;

    public ExtractedSpecDto() {}

    public ExtractedSpecDto(UUID id, String rawSpecName, String canonicalSpecName, String specValue,
                            Double numericValue, Double minValue, Double maxValue, String unit,
                            Boolean isNumeric, String rawText, String extractionConfidence,
                            String extractionSource, String normalizationConfidence,
                            String matchedTenderSpecId, Integer displayOrder) {
        this.id = id;
        this.rawSpecName = rawSpecName;
        this.canonicalSpecName = canonicalSpecName;
        this.specValue = specValue;
        this.numericValue = numericValue;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.unit = unit;
        this.isNumeric = isNumeric;
        this.rawText = rawText;
        this.extractionConfidence = extractionConfidence;
        this.extractionSource = extractionSource;
        this.normalizationConfidence = normalizationConfidence;
        this.matchedTenderSpecId = matchedTenderSpecId;
        this.displayOrder = displayOrder;
    }

    public static ExtractedSpecDto fromTenderSpec(TenderSpec spec) {
        if (spec == null) return null;
        return new ExtractedSpecDto(
                spec.getId(),
                spec.getRawSpecName(),
                spec.getCanonicalSpecName(),
                spec.getSpecValue(),
                spec.getNumericValue(),
                spec.getMinValue(),
                spec.getMaxValue(),
                spec.getUnit(),
                spec.getIsNumeric(),
                spec.getRawText(),
                spec.getExtractionConfidence(),
                spec.getExtractionSource(),
                spec.getNormalizationConfidence(),
                null,
                spec.getDisplayOrder()
        );
    }

    public static ExtractedSpecDto fromVendorSpec(VendorSpec spec) {
        if (spec == null) return null;
        return new ExtractedSpecDto(
                spec.getId(),
                spec.getRawSpecName(),
                spec.getCanonicalSpecName(),
                spec.getSpecValue(),
                spec.getNumericValue(),
                spec.getMinValue(),
                spec.getMaxValue(),
                spec.getUnit(),
                spec.getIsNumeric(),
                spec.getRawText(),
                spec.getExtractionConfidence(),
                spec.getExtractionSource(),
                spec.getNormalizationConfidence(),
                spec.getMatchedTenderSpec() != null ? spec.getMatchedTenderSpec().getId().toString() : null,
                spec.getDisplayOrder()
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public String getMatchedTenderSpecId() { return matchedTenderSpecId; }
    public void setMatchedTenderSpecId(String matchedTenderSpecId) { this.matchedTenderSpecId = matchedTenderSpecId; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
