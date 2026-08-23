package com.tendermatch.dto.scoring;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record TenderScoresResponse(
    UUID tenderId,
    String tenderTitle,
    List<VendorScoreResponse> scores,
    String scoringMethod,
    boolean allVendorsFinalized,
    ZonedDateTime scoredAt
) {}
