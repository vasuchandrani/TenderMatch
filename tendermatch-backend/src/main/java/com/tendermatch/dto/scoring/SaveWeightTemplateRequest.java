package com.tendermatch.dto.scoring;

import java.util.List;

public record SaveWeightTemplateRequest(
    String templateName,
    List<SpecWeightDto> weights
) {}
