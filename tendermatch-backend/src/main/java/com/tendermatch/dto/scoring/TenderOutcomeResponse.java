package com.tendermatch.dto.scoring;

import java.time.ZonedDateTime;
import java.util.UUID;

public record TenderOutcomeResponse(
    UUID id,
    UUID tenderId,
    UUID winningVendorId,
    String winningVendorName,
    String outcomeNotes,
    String recordedByName,
    ZonedDateTime recordedAt
) {}
