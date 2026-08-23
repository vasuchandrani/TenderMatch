package com.tendermatch.dto.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class ProcessDocumentAiRequest {

    @JsonProperty("minio_file_key")
    private String minioFileKey;

    @JsonProperty("document_type_hint")
    private String documentTypeHint;

    @JsonProperty("tender_id")
    private String tenderId;

    @JsonProperty("submission_id")
    private String submissionId;

    @JsonProperty("file_bytes_base64")
    private String fileBytesBase64;

    @JsonProperty("requirement_products")
    private List<Map<String, Object>> requirementProducts;

    public ProcessDocumentAiRequest() {}

    public ProcessDocumentAiRequest(String minioFileKey, String documentTypeHint, String tenderId,
                                    String submissionId, String fileBytesBase64,
                                    List<Map<String, Object>> requirementProducts) {
        this.minioFileKey = minioFileKey;
        this.documentTypeHint = documentTypeHint;
        this.tenderId = tenderId;
        this.submissionId = submissionId;
        this.fileBytesBase64 = fileBytesBase64;
        this.requirementProducts = requirementProducts;
    }

    public String getMinioFileKey() { return minioFileKey; }
    public void setMinioFileKey(String minioFileKey) { this.minioFileKey = minioFileKey; }

    public String getDocumentTypeHint() { return documentTypeHint; }
    public void setDocumentTypeHint(String documentTypeHint) { this.documentTypeHint = documentTypeHint; }

    public String getTenderId() { return tenderId; }
    public void setTenderId(String tenderId) { this.tenderId = tenderId; }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }

    public String getFileBytesBase64() { return fileBytesBase64; }
    public void setFileBytesBase64(String fileBytesBase64) { this.fileBytesBase64 = fileBytesBase64; }

    public List<Map<String, Object>> getRequirementProducts() { return requirementProducts; }
    public void setRequirementProducts(List<Map<String, Object>> requirementProducts) { this.requirementProducts = requirementProducts; }
}
