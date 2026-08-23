package com.tendermatch.dto.scoring;

import java.util.UUID;

public record ComparisonCellDto(
    UUID vendorSubmissionId,
    String vendorName,
    String offeredValue,
    String offeredUnit,
    String systemVerdict,
    String humanVerdict,
    Double confidenceScore,
    Double cellScore,
    Double weight
) {}
