package com.tendermatch.dto.scoring;

public record SpecWeightDto(
    String canonicalSpecName,
    String weightCategory,
    Double weightValue
) {}
