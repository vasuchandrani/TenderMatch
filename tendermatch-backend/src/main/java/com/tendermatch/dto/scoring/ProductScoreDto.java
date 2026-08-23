package com.tendermatch.dto.scoring;

import java.util.UUID;

public record ProductScoreDto(
    UUID tenderProductId,
    String productName,
    Double score,
    int totalSpecs,
    int matched,
    int unmatched,
    int uncertain
) {}
