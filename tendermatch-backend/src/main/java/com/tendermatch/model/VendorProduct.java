package com.tendermatch.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vendor_products")
public class VendorProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_submission_id", nullable = false)
    @JsonIgnore
    private VendorSubmission vendorSubmission;

    @Column(name = "product_name", nullable = false, length = 500)
    private String productName;

    @Column(name = "product_category", length = 100)
    private String productCategory = "electronics";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_tender_product_id")
    private TenderProduct matchedTenderProduct;

    @Column(name = "match_confidence")
    private Double matchConfidence;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @OneToMany(mappedBy = "vendorProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VendorSpec> specifications = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public VendorProduct() {}

    public VendorProduct(UUID id, VendorSubmission vendorSubmission, String productName, String productCategory,
                         TenderProduct matchedTenderProduct, Double matchConfidence, Integer displayOrder,
                         String rawText, List<VendorSpec> specifications, ZonedDateTime createdAt) {
        this.id = id;
        this.vendorSubmission = vendorSubmission;
        this.productName = productName;
        this.productCategory = productCategory != null ? productCategory : "electronics";
        this.matchedTenderProduct = matchedTenderProduct;
        this.matchConfidence = matchConfidence;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.rawText = rawText;
        this.specifications = specifications != null ? specifications : new ArrayList<>();
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public VendorSubmission getVendorSubmission() { return vendorSubmission; }
    public void setVendorSubmission(VendorSubmission vendorSubmission) { this.vendorSubmission = vendorSubmission; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }

    public TenderProduct getMatchedTenderProduct() { return matchedTenderProduct; }
    public void setMatchedTenderProduct(TenderProduct matchedTenderProduct) { this.matchedTenderProduct = matchedTenderProduct; }

    public Double getMatchConfidence() { return matchConfidence; }
    public void setMatchConfidence(Double matchConfidence) { this.matchConfidence = matchConfidence; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public List<VendorSpec> getSpecifications() { return specifications; }
    public void setSpecifications(List<VendorSpec> specifications) { this.specifications = specifications; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
