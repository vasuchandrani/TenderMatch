package com.tendermatch.dto.review;

import java.util.UUID;

public record ReviewProgressResponse(
        UUID tenderId,
        UUID vendorId,
        long reviewedCount,
        long totalCount,
        double percentage,
        long overrideCount,
        boolean isLocked
) {}
