package com.tendermatch.dto.scoring;

import java.util.UUID;

public record RecordOutcomeRequest(
    UUID winningVendorId,
    String outcomeNotes
) {}
