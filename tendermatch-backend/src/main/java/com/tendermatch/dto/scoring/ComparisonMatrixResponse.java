package com.tendermatch.dto.scoring;

import java.util.List;
import java.util.UUID;

public record ComparisonMatrixResponse(
    UUID tenderId,
    String tenderTitle,
    List<String> vendorNames,
    List<ComparisonRowDto> rows
) {}
