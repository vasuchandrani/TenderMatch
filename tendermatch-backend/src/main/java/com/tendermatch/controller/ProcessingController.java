package com.tendermatch.controller;

import com.tendermatch.dto.common.ApiResponse;
import com.tendermatch.dto.pipeline.ExtractedProductDto;
import com.tendermatch.dto.pipeline.ExtractedSpecDto;
import com.tendermatch.dto.pipeline.ProcessingStatusResponse;
import com.tendermatch.service.ProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenders")
public class ProcessingController {

    private final ProcessingService processingService;

    public ProcessingController(ProcessingService processingService) {
        this.processingService = processingService;
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<ProcessingStatusResponse>> triggerProcess(
            @PathVariable("id") UUID tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        ProcessingStatusResponse statusResponse = processingService.triggerProcessing(tenderId, email);

        // Run processing asynchronously in background
        processingService.processTenderAsync(tenderId, email);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok(statusResponse, "Document extraction and processing initiated"));
    }

    @GetMapping("/{id}/processing-status")
    public ResponseEntity<ApiResponse<ProcessingStatusResponse>> getProcessingStatus(
            @PathVariable("id") UUID tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        ProcessingStatusResponse status = processingService.getProcessingStatus(tenderId, email);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<ApiResponse<List<ExtractedProductDto>>> getProducts(
            @PathVariable("id") UUID tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<ExtractedProductDto> products = processingService.getTenderProducts(tenderId, email);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/{id}/products/{productId}/specs")
    public ResponseEntity<ApiResponse<List<ExtractedSpecDto>>> getProductSpecs(
            @PathVariable("id") UUID tenderId,
            @PathVariable("productId") UUID productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<ExtractedSpecDto> specs = processingService.getTenderProductSpecs(tenderId, productId, email);
        return ResponseEntity.ok(ApiResponse.ok(specs));
    }

    @GetMapping("/{id}/vendor-products")
    public ResponseEntity<ApiResponse<List<ExtractedProductDto>>> getVendorProducts(
            @PathVariable("id") UUID tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<ExtractedProductDto> products = processingService.getAllVendorProducts(tenderId, email);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }

    @GetMapping("/{id}/vendors/{vendorId}/products")
    public ResponseEntity<ApiResponse<List<ExtractedProductDto>>> getVendorSubmissionProducts(
            @PathVariable("id") UUID tenderId,
            @PathVariable("vendorId") UUID vendorId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<ExtractedProductDto> products = processingService.getVendorSubmissionProducts(tenderId, vendorId, email);
        return ResponseEntity.ok(ApiResponse.ok(products));
    }
}
