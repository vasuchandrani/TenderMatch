package com.tendermatch.dto.admin;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.Map;

public record ModelVersionDto(
    @JsonAlias({"model_name", "modelName"}) String modelName,
    @JsonAlias({"version"}) Integer version,
    @JsonAlias({"status"}) String status,
    @JsonAlias({"model_type", "modelType"}) String modelType,
    @JsonAlias({"primary_metric", "primaryMetric"}) String primaryMetric,
    @JsonAlias({"primary_metric_value", "primaryMetricValue"}) Double primaryMetricValue,
    @JsonAlias({"metrics"}) Map<String, Object> metrics,
    @JsonAlias({"training_data_count", "trainingDataCount"}) Integer trainingDataCount,
    @JsonAlias({"trained_at", "trainedAt"}) String trainedAt,
    @JsonAlias({"promoted_at", "promotedAt"}) String promotedAt,
    @JsonAlias({"retired_at", "retiredAt"}) String retiredAt,
    @JsonAlias({"model_path", "modelPath"}) String modelPath
) {}
