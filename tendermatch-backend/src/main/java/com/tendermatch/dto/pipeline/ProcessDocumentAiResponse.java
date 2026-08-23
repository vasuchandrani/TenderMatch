package com.tendermatch.dto.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class ProcessDocumentAiResponse {

    private String status;
    private String message;

    @JsonProperty("minio_file_key")
    private String minioFileKey;

    @JsonProperty("document_type")
    private String documentType;

    private List<ExtractedProductDto> products = new ArrayList<>();

    public ProcessDocumentAiResponse() {}

    public ProcessDocumentAiResponse(String status, String message, String minioFileKey, String documentType, List<ExtractedProductDto> products) {
        this.status = status;
        this.message = message;
        this.minioFileKey = minioFileKey;
        this.documentType = documentType;
        this.products = products != null ? products : new ArrayList<>();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getMinioFileKey() { return minioFileKey; }
    public void setMinioFileKey(String minioFileKey) { this.minioFileKey = minioFileKey; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public List<ExtractedProductDto> getProducts() { return products; }
    public void setProducts(List<ExtractedProductDto> products) { this.products = products; }
}
