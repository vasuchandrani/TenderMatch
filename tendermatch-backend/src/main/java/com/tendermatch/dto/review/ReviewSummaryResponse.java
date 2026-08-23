package com.tendermatch.dto.review;

import java.util.List;
import java.util.UUID;

public record ReviewSummaryResponse(
        UUID tenderId,
        UUID vendorId,
        String vendorName,
        boolean isLocked,
        long matchCount,
        long unmatchCount,
        long aiErrCount,
        long totalSpecs,
        long overrideCount,
        List<ProductSummaryDto> productSummaries
) {
    public record ProductSummaryDto(
            String productName,
            long matchCount,
            long unmatchCount,
            long aiErrCount,
            long overrideCount,
            List<SpecVerdictSummaryDto> specs
    ) {}

    public record SpecVerdictSummaryDto(
            String specName,
            String systemVerdict,
            String humanVerdict,
            boolean isOverride,
            String comment
    ) {}
}
