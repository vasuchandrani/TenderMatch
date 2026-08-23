package com.tendermatch.dto.scoring;

import java.util.List;
import java.util.UUID;

public record ComparisonRowDto(
    UUID tenderSpecId,
    String requiredSpecName,
    String canonicalSpecName,
    String requiredValue,
    String requiredUnit,
    String comparisonOperator,
    Double weight,
    String weightCategory,
    List<ComparisonCellDto> vendorCells
) {}
