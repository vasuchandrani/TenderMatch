package com.tendermatch.dto.matching;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchSpecsAiResponse {

    @JsonProperty("tender_id")
    private String tenderId;

    @JsonProperty("vendor_submission_id")
    private String vendorSubmissionId;

    private List<MatchResultItemAiDto> results = new ArrayList<>();

    public MatchSpecsAiResponse() {}

    public MatchSpecsAiResponse(String tenderId, String vendorSubmissionId, List<MatchResultItemAiDto> results) {
        this.tenderId = tenderId;
        this.vendorSubmissionId = vendorSubmissionId;
        this.results = results != null ? results : new ArrayList<>();
    }

    public String getTenderId() { return tenderId; }
    public void setTenderId(String tenderId) { this.tenderId = tenderId; }

    public String getVendorSubmissionId() { return vendorSubmissionId; }
    public void setVendorSubmissionId(String vendorSubmissionId) { this.vendorSubmissionId = vendorSubmissionId; }

    public List<MatchResultItemAiDto> getResults() { return results; }
    public void setResults(List<MatchResultItemAiDto> results) { this.results = results; }

    public static class MatchResultItemAiDto {
        @JsonProperty("pair_id")
        private String pairId;

        private String operator;
        private String method;
        private String verdict;
        private Double confidence;
        private String details;

        @JsonProperty("cross_encoder_score")
        private Double crossEncoderScore;

        @JsonProperty("llm_verdict")
        private String llmVerdict;

        @JsonProperty("llm_reasoning")
        private String llmReasoning;

        @JsonProperty("numeric_comparison")
        private Map<String, Object> numericComparison;

        @JsonProperty("confidence_features")
        private Map<String, Object> confidenceFeatures;

        public MatchResultItemAiDto() {}

        public MatchResultItemAiDto(String pairId, String operator, String method, String verdict,
                                    Double confidence, String details, Double crossEncoderScore,
                                    String llmVerdict, String llmReasoning, Map<String, Object> numericComparison,
                                    Map<String, Object> confidenceFeatures) {
            this.pairId = pairId;
            this.operator = operator;
            this.method = method;
            this.verdict = verdict;
            this.confidence = confidence;
            this.details = details;
            this.crossEncoderScore = crossEncoderScore;
            this.llmVerdict = llmVerdict;
            this.llmReasoning = llmReasoning;
            this.numericComparison = numericComparison;
            this.confidenceFeatures = confidenceFeatures;
        }

        public String getPairId() { return pairId; }
        public void setPairId(String pairId) { this.pairId = pairId; }

        public String getOperator() { return operator; }
        public void setOperator(String operator) { this.operator = operator; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getVerdict() { return verdict; }
        public void setVerdict(String verdict) { this.verdict = verdict; }

        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }

        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }

        public Double getCrossEncoderScore() { return crossEncoderScore; }
        public void setCrossEncoderScore(Double crossEncoderScore) { this.crossEncoderScore = crossEncoderScore; }

        public String getLlmVerdict() { return llmVerdict; }
        public void setLlmVerdict(String llmVerdict) { this.llmVerdict = llmVerdict; }

        public String getLlmReasoning() { return llmReasoning; }
        public void setLlmReasoning(String llmReasoning) { this.llmReasoning = llmReasoning; }

        public Map<String, Object> getNumericComparison() { return numericComparison; }
        public void setNumericComparison(Map<String, Object> numericComparison) { this.numericComparison = numericComparison; }

        public Map<String, Object> getConfidenceFeatures() { return confidenceFeatures; }
        public void setConfidenceFeatures(Map<String, Object> confidenceFeatures) { this.confidenceFeatures = confidenceFeatures; }
    }
}
