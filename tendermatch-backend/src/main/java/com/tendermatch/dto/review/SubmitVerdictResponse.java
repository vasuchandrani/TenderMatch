package com.tendermatch.dto.review;

import java.time.ZonedDateTime;
import java.util.UUID;

public record SubmitVerdictResponse(
        UUID resultId,
        String systemVerdict,
        String humanVerdict,
        boolean isOverride,
        String userComment,
        UUID nextResultId,
        ReviewProgressResponse reviewProgress,
        ZonedDateTime reviewedAt
) {}
