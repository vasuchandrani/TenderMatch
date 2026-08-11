package com.tendermatch.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tender_products")
public class TenderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tender_id", nullable = false)
    @JsonIgnore
    private Tender tender;

    @Column(name = "product_name", nullable = false, length = 500)
    private String productName;

    @Column(name = "product_category", length = 100)
    private String productCategory = "electronics";

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @OneToMany(mappedBy = "tenderProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TenderSpec> specifications = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public TenderProduct() {}

    public TenderProduct(UUID id, Tender tender, String productName, String productCategory,
                         Integer displayOrder, String rawText, List<TenderSpec> specifications, ZonedDateTime createdAt) {
        this.id = id;
        this.tender = tender;
        this.productName = productName;
        this.productCategory = productCategory != null ? productCategory : "electronics";
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.rawText = rawText;
        this.specifications = specifications != null ? specifications : new ArrayList<>();
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Tender getTender() { return tender; }
    public void setTender(Tender tender) { this.tender = tender; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductCategory() { return productCategory; }
    public void setProductCategory(String productCategory) { this.productCategory = productCategory; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public List<TenderSpec> getSpecifications() { return specifications; }
    public void setSpecifications(List<TenderSpec> specifications) { this.specifications = specifications; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
