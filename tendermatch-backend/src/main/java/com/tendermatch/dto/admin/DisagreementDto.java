package com.tendermatch.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;

public record DisagreementDto(
    @JsonAlias({"input_hash", "inputHash"}) String inputHash,
    @JsonAlias({"rule_result", "ruleResult"}) String ruleResult,
    @JsonAlias({"ml_result", "mlResult"}) String mlResult,
    @JsonAlias({"input_sample", "inputSample"}) String inputSample
) {}
