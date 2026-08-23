package com.tendermatch.dto.admin;

public record TrainingDataStatsResponse(
    long totalCorrectionLogs,
    long totalOverrides,
    long totalAutoConfirmed,
    long totalMatchResults,
    long totalTenderOutcomes,
    boolean confidenceModelReady,
    boolean operatorModelReady,
    boolean rankingModelReady
) {}
