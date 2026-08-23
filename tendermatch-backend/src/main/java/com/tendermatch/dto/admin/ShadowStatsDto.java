package com.tendermatch.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public record ShadowStatsDto(
    @JsonAlias({"model_name", "modelName"}) String modelName,
    @JsonAlias({"total_predictions", "totalPredictions"}) long totalPredictions,
    @JsonAlias({"agreement_count", "agreementCount"}) long agreementCount,
    @JsonAlias({"disagreement_count", "disagreementCount"}) long disagreementCount,
    @JsonAlias({"agreement_rate", "agreementRate"}) Double agreementRate,
    @JsonAlias({"disagreements"}) List<DisagreementDto> disagreements
) {}
