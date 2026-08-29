package com.tendermatch.controller;

import com.tendermatch.dto.common.ApiResponse;
import com.tendermatch.dto.scoring.*;
import com.tendermatch.service.ExportService;
import com.tendermatch.service.ScoringService;
import com.tendermatch.service.WeightService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ScoreController {

    private final ScoringService scoringService;
    private final WeightService weightService;
    private final ExportService exportService;

    public ScoreController(ScoringService scoringService,
                           WeightService weightService,
                           ExportService exportService) {
        this.scoringService = scoringService;
        this.weightService = weightService;
        this.exportService = exportService;
    }

    @PostMapping("/api/tenders/{id}/calculate-scores")
    public ResponseEntity<ApiResponse<TenderScoresResponse>> calculateScores(
            @PathVariable("id") UUID tenderId) {
        TenderScoresResponse response = scoringService.calculateScores(tenderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/tenders/{id}/scores")
    public ResponseEntity<ApiResponse<TenderScoresResponse>> getTenderScores(
            @PathVariable("id") UUID tenderId) {
        TenderScoresResponse response = scoringService.getTenderScores(tenderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/tenders/{id}/scores/{vendorId}")
    public ResponseEntity<ApiResponse<VendorScoreResponse>> getVendorScore(
            @PathVariable("id") UUID tenderId,
            @PathVariable("vendorId") UUID vendorId) {
        VendorScoreResponse response = scoringService.getVendorScore(tenderId, vendorId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/tenders/{id}/comparison-matrix")
    public ResponseEntity<ApiResponse<ComparisonMatrixResponse>> getComparisonMatrix(
            @PathVariable("id") UUID tenderId) {
        ComparisonMatrixResponse response = scoringService.getComparisonMatrix(tenderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/tenders/{id}/weights")
    public ResponseEntity<ApiResponse<List<SpecWeightDto>>> getWeights(
            @PathVariable("id") UUID tenderId) {
        List<SpecWeightDto> weights = weightService.getWeightsForTender(tenderId);
        return ResponseEntity.ok(ApiResponse.ok(weights));
    }

    @PutMapping("/api/tenders/{id}/weights")
    public ResponseEntity<ApiResponse<List<SpecWeightDto>>> updateWeights(
            @PathVariable("id") UUID tenderId,
            @Valid @RequestBody UpdateWeightsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        List<SpecWeightDto> updated = weightService.updateWeights(tenderId, email, request.weights());
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @PostMapping("/api/tenders/{id}/weights/template")
    public ResponseEntity<ApiResponse<WeightTemplateDto>> saveTemplate(
            @PathVariable("id") UUID tenderId,
            @Valid @RequestBody SaveWeightTemplateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        WeightTemplateDto saved = weightService.saveTemplate(email, request.templateName(), request.weights());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved, "Template saved successfully"));
    }

    @GetMapping("/api/weights/templates")
    public ResponseEntity<ApiResponse<List<WeightTemplateDto>>> listTemplates() {
        List<WeightTemplateDto> templates = weightService.listTemplates();
        return ResponseEntity.ok(ApiResponse.ok(templates));
    }

    @PostMapping("/api/tenders/{id}/weights/from-template/{templateName}")
    public ResponseEntity<ApiResponse<List<SpecWeightDto>>> applyTemplate(
            @PathVariable("id") UUID tenderId,
            @PathVariable("templateName") String templateName,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        List<SpecWeightDto> applied = weightService.applyTemplate(tenderId, email, templateName);
        return ResponseEntity.ok(ApiResponse.ok(applied, "Template applied successfully"));
    }

    @GetMapping("/api/tenders/{id}/export/csv")
    public ResponseEntity<String> exportCsv(@PathVariable("id") UUID tenderId) {
        String csv = exportService.exportCsv(tenderId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tender-evaluation-" + tenderId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/api/tenders/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable("id") UUID tenderId) {
        byte[] pdfBytes = exportService.exportPdf(tenderId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tender-evaluation-" + tenderId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/api/tenders/{id}/outcome")
    public ResponseEntity<ApiResponse<TenderOutcomeResponse>> recordOutcome(
            @PathVariable("id") UUID tenderId,
            @RequestBody RecordOutcomeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        TenderOutcomeResponse res = scoringService.recordOutcome(tenderId, email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(res, "Outcome recorded successfully"));
    }

    @GetMapping("/api/tenders/{id}/outcome")
    public ResponseEntity<ApiResponse<TenderOutcomeResponse>> getOutcome(
            @PathVariable("id") UUID tenderId) {
        TenderOutcomeResponse res = scoringService.getOutcome(tenderId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
