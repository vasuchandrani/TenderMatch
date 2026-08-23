package com.tendermatch.dto.matching;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class MatchSpecPairAiDto {

    @JsonProperty("pair_id")
    private String pairId;

    private Map<String, Object> requirement;

    private Map<String, Object> vendor;

    public MatchSpecPairAiDto() {}

    public MatchSpecPairAiDto(String pairId, Map<String, Object> requirement, Map<String, Object> vendor) {
        this.pairId = pairId;
        this.requirement = requirement;
        this.vendor = vendor;
    }

    public String getPairId() { return pairId; }
    public void setPairId(String pairId) { this.pairId = pairId; }

    public Map<String, Object> getRequirement() { return requirement; }
    public void setRequirement(Map<String, Object> requirement) { this.requirement = requirement; }

    public Map<String, Object> getVendor() { return vendor; }
    public void setVendor(Map<String, Object> vendor) { this.vendor = vendor; }
}
