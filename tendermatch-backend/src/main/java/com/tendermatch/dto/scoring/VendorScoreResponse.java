package com.tendermatch.dto.scoring;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record VendorScoreResponse(
    UUID vendorScoreId,
    UUID vendorSubmissionId,
    String vendorName,
    String fileName,
    Double overallScore,
    Integer rank,
    int totalSpecs,
    int matched,
    int unmatched,
    int uncertain,
    boolean isReviewLocked,
    List<ProductScoreDto> productScores,
    String scoringMethod,
    ZonedDateTime scoredAt
) {}
