package com.tendermatch.dto.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tendermatch.model.TenderProduct;
import com.tendermatch.model.VendorProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ExtractedProductDto {

    private UUID id;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("product_category")
    private String productCategory;

    @JsonProperty("matched_tender_product_id")
    private String matchedTenderProductId;

    @JsonProperty("match_confidence")
    private Double matchConfidence;

    @JsonProperty("raw_text")
    private String rawText;

    @JsonProperty("display_order")
    private Integer displayOrder;

    @JsonProperty("vendor_name")
    private String vendorName;

    @JsonProperty("vendor_file_name")
    private String vendorFileName;

    @JsonProperty("submission_id")
    private UUID submissionId;

    private List<ExtractedSpecDto> specifications = new ArrayList<>();

    public ExtractedProductDto() {}

    public ExtractedProductDto(UUID id, String productName, String productCategory,
                               String matchedTenderProductId, Double matchConfidence,
                               String rawText, Integer displayOrder, List<ExtractedSpecDto> specifications) {
        this.id = id;
        this.productName = productName;
        this.productCategory = productCategory;
        this.matchedTenderProductId = matchedTenderProductId;
        this.matchConfidence = matchConfidence;
        this.rawText = rawText;
        this.displayOrder = displayOrder;
        this.specifications = specifications != null ? specifications : new ArrayList<>();
    }

    public static ExtractedProductDto fromTenderProduct(TenderProduct product) {
        if (product == null) return null;
        List<ExtractedSpecDto> specs = product.getSpecifications() != null
                ? product.getSpecifications().stream().map(ExtractedSpecDto::fromTenderSpec).collect(Collectors.toList())
                : new ArrayList<>();

        return new ExtractedProductDto(
                product.getId(),
                product.getProductName(),
                product.getProductCategory(),
                null,
                null,
                product.getRawText(),
                product.getDisplayOrder(),
                specs
        );
    }

    public static ExtractedProductDto fromVendorProduct(VendorProduct product) {
        if (product == null) return null;
        List<ExtractedSpecDto> specs = product.getSpecifications() != null
                ? product.getSpecifications().stream().map(ExtractedSpecDto::fromVendorSpec).collect(Collectors.toList())
                : new ArrayList<>();

        ExtractedProductDto dto = new ExtractedProductDto(
                product.getId(),
                product.getProductName(),
                product.getProductCategory(),
                product.getMatchedTenderProduct() != null ? product.getMatchedTenderProduct().getId().toString() : null,
                product.getMatchConfidence(),
                product.getRawText(),
                product.getDisplayOrder(),
                specs
        );
        if (product.getVendorSubmission() != null) {
            dto.setVendorName(product.getVendorSubmission().getVendorName());
            dto.setVendorFileName(product.getVendorSubmission().getFileName());
            dto.setSubmissionId(product.getVendorSubmission().getId());
        }
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }

    public String getMatchedTenderProductId() { return matchedTenderProductId; }
    public void setMatchedTenderProductId(String matchedTenderProductId) { this.matchedTenderProductId = matchedTenderProductId; }

    public Double getMatchConfidence() { return matchConfidence; }
    public void setMatchConfidence(Double matchConfidence) { this.matchConfidence = matchConfidence; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getVendorFileName() { return vendorFileName; }
    public void setVendorFileName(String vendorFileName) { this.vendorFileName = vendorFileName; }

    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }

    public List<ExtractedSpecDto> getSpecifications() { return specifications; }
    public void setSpecifications(List<ExtractedSpecDto> specifications) { this.specifications = specifications; }
}
