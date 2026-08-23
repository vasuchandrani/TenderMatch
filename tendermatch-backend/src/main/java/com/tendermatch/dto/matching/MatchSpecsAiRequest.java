package com.tendermatch.dto.matching;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class MatchSpecsAiRequest {

    @JsonProperty("tender_id")
    private String tenderId;

    @JsonProperty("vendor_submission_id")
    private String vendorSubmissionId;

    @JsonProperty("spec_pairs")
    private List<MatchSpecPairAiDto> specPairs = new ArrayList<>();

    public MatchSpecsAiRequest() {}

    public MatchSpecsAiRequest(String tenderId, String vendorSubmissionId, List<MatchSpecPairAiDto> specPairs) {
        this.tenderId = tenderId;
        this.vendorSubmissionId = vendorSubmissionId;
        this.specPairs = specPairs != null ? specPairs : new ArrayList<>();
    }

    public String getTenderId() { return tenderId; }
    public void setTenderId(String tenderId) { this.tenderId = tenderId; }

    public String getVendorSubmissionId() { return vendorSubmissionId; }
    public void setVendorSubmissionId(String vendorSubmissionId) { this.vendorSubmissionId = vendorSubmissionId; }

    public List<MatchSpecPairAiDto> getSpecPairs() { return specPairs; }
    public void setSpecPairs(List<MatchSpecPairAiDto> specPairs) { this.specPairs = specPairs; }
}
