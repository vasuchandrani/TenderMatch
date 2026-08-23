package com.tendermatch.dto.review;

import java.util.UUID;

public record ReviewSpecPairDto(
        UUID resultId,
        UUID tenderSpecId,
        String requiredSpecName,
        String canonicalName,
        String requiredValue,
        String requiredUnit,
        String requiredRawText,

        UUID vendorSpecId,
        String offeredSpecName,
        String offeredValue,
        String offeredUnit,
        String offeredRawText,

        String systemVerdict,
        double confidenceScore,
        String comparisonMethod,
        String comparisonOperator,
        String comparisonDetails,

        String humanVerdict,
        boolean isReviewed,
        boolean isOverride,
        String userComment
) {}
