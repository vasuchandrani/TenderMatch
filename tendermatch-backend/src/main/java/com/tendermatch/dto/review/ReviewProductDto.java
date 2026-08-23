package com.tendermatch.dto.review;

import java.util.List;
import java.util.UUID;

public record ReviewProductDto(
        UUID tenderProductId,
        String tenderProductName,
        UUID vendorProductId,
        String vendorProductName,
        List<ReviewSpecPairDto> specPairs
) {}
