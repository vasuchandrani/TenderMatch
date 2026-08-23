package com.tendermatch.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SubmitVerdictRequest(
        @NotBlank(message = "humanVerdict is required")
        @Pattern(regexp = "^(Match|Unmatch|AI Can't Predict)$", message = "humanVerdict must be 'Match', 'Unmatch', or 'AI Can't Predict'")
        String humanVerdict,

        String comment
) {}
