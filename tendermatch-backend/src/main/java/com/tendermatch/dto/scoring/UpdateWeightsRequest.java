package com.tendermatch.dto.scoring;

import java.util.List;

public record UpdateWeightsRequest(
    List<SpecWeightDto> weights
) {}
