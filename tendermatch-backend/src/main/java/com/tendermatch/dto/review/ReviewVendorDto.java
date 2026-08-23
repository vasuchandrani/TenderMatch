package com.tendermatch.dto.review;

import java.util.List;
import java.util.UUID;

public record ReviewVendorDto(
        UUID vendorId,
        String vendorName,
        String fileName,
        boolean isReviewLocked,
        List<ReviewProductDto> products,
        ReviewProgressResponse progress
) {}
