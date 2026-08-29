package com.tendermatch.controller;

import com.tendermatch.dto.admin.*;
import com.tendermatch.dto.common.ApiResponse;
import com.tendermatch.service.ModelAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class ModelAdminController {

    private final ModelAdminService modelAdminService;

    public ModelAdminController(ModelAdminService modelAdminService) {
        this.modelAdminService = modelAdminService;
    }

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<ModelSummaryDto>>> listModels() {
        List<ModelSummaryDto> models = modelAdminService.listModels();
        return ResponseEntity.ok(ApiResponse.ok(models));
    }

    @GetMapping("/models/{modelName}/versions")
    public ResponseEntity<ApiResponse<List<ModelVersionDto>>> getModelVersions(
            @PathVariable("modelName") String modelName) {
        List<ModelVersionDto> versions = modelAdminService.getModelVersions(modelName);
        return ResponseEntity.ok(ApiResponse.ok(versions));
    }

    @GetMapping("/training-data-stats")
    public ResponseEntity<ApiResponse<TrainingDataStatsResponse>> getTrainingDataStats() {
        TrainingDataStatsResponse stats = modelAdminService.getTrainingDataStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @PostMapping("/models/{modelName}/{version}/promote")
    public ResponseEntity<ApiResponse<ModelVersionDto>> promoteModel(
            @PathVariable("modelName") String modelName,
            @PathVariable("version") Integer version) {
        ModelVersionDto promoted = modelAdminService.promoteModel(modelName, version);
        return ResponseEntity.ok(ApiResponse.ok(promoted, "Model promoted successfully"));
    }

    @PostMapping("/models/{modelName}/{version}/rollback")
    public ResponseEntity<ApiResponse<ModelVersionDto>> rollbackModel(
            @PathVariable("modelName") String modelName,
            @PathVariable("version") Integer version) {
        ModelVersionDto rolledBack = modelAdminService.rollbackModel(modelName, version);
        return ResponseEntity.ok(ApiResponse.ok(rolledBack, "Model rolled back successfully"));
    }

    @GetMapping("/models/{modelName}/shadow-stats")
    public ResponseEntity<ApiResponse<ShadowStatsDto>> getShadowStats(
            @PathVariable("modelName") String modelName) {
        ShadowStatsDto stats = modelAdminService.getShadowStats(modelName);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/retrain-check")
    public ResponseEntity<ApiResponse<List<RetrainCheckDto>>> checkRetrain() {
        List<RetrainCheckDto> checks = modelAdminService.checkRetrain();
        return ResponseEntity.ok(ApiResponse.ok(checks));
    }

    @PostMapping("/retrain/{modelName}")
    public ResponseEntity<ApiResponse<ModelVersionDto>> triggerRetrain(
            @PathVariable("modelName") String modelName) {
        ModelVersionDto result = modelAdminService.triggerRetrain(modelName);
        return ResponseEntity.ok(ApiResponse.ok(result, "Model retraining completed successfully"));
    }
}
