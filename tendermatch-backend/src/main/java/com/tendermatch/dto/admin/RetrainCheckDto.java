package com.tendermatch.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;

public record RetrainCheckDto(
    @JsonAlias({"model_name", "modelName"}) String modelName,
    @JsonAlias({"retrain_needed", "retrainNeeded"}) boolean retrainNeeded,
    @JsonAlias({"reason"}) String reason,
    @JsonAlias({"total_data_count", "totalDataCount"}) long totalDataCount,
    @JsonAlias({"new_data_count", "newDataCount"}) long newDataCount
) {}
