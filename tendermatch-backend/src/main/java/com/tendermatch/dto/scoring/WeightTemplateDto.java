package com.tendermatch.dto.scoring;

import java.util.List;

public record WeightTemplateDto(
    String templateName,
    List<SpecWeightDto> weights
) {}
