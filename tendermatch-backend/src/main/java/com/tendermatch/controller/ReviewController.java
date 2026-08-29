package com.tendermatch.controller;

import com.tendermatch.dto.common.ApiResponse;
import com.tendermatch.dto.review.*;
import com.tendermatch.dto.scoring.TenderScoresResponse;
import com.tendermatch.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenders/{id}")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/review")
    public ResponseEntity<ApiResponse<List<ReviewVendorDto>>> getTenderReview(
            @PathVariable("id") UUID tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<ReviewVendorDto> data = reviewService.getTenderReviewData(tenderId, email);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/vendors/{vendorId}/review")
    public ResponseEntity<ApiResponse<ReviewVendorDto>> getVendorReview(
            @PathVariable("id") UUID tenderId,
            @PathVariable("vendorId") UUID vendorId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        ReviewVendorDto data = reviewService.getVendorReviewData(tenderId, vendorId, email);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/vendors/{vendorId}/review/progress")
    public ResponseEntity<ApiResponse<ReviewProgressResponse>> getVendorReviewProgress(
            @PathVariable("id") UUID tenderId,
            @PathVariable("vendorId") UUID vendorId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        ReviewProgressResponse progress = reviewService.getVendorReviewProgress(tenderId, vendorId, email);
        return ResponseEntity.ok(ApiResponse.ok(progress));
    }

    @PutMapping("/results/{resultId}/verdict")
    public ResponseEntity<ApiResponse<SubmitVerdictResponse>> submitVerdict(
            @PathVariable("id") UUID tenderId,
            @PathVariable("resultId") UUID resultId,
            @Valid @RequestBody SubmitVerdictRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        SubmitVerdictResponse response = reviewService.submitVerdict(tenderId, resultId, request, email);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/vendors/{vendorId}/bulk-confirm")
    public ResponseEntity<ApiResponse<ReviewProgressResponse>> bulkConfirm(
            @PathVariable("id") UUID tenderId,
            @PathVariable("vendorId") UUID vendorId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        ReviewProgressResponse progress = reviewService.bulkConfirmRemaining(tenderId, vendorId, email);
        return ResponseEntity.ok(ApiResponse.ok(progress));
    }

    @GetMapping("/vendors/{vendorId}/review-summary")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> getReviewSummary(
            @PathVariable("id") UUID tenderId,
            @PathVariable("vendorId") UUID vendorId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        ReviewSummaryResponse summary = reviewService.getReviewSummary(tenderId, vendorId, email);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @PostMapping("/vendors/{vendorId}/finalize")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> finalizeReview(
            @PathVariable("id") UUID tenderId,
            @PathVariable("vendorId") UUID vendorId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        ReviewSummaryResponse summary = reviewService.finalizeReview(tenderId, vendorId, email);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @PostMapping("/vendors/{vendorId}/reverify-reflection")
    public ResponseEntity<ApiResponse<ReviewSummaryResponse>> reverifyReflection(
            @PathVariable("id") UUID tenderId,
            @PathVariable("vendorId") UUID vendorId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        ReviewSummaryResponse summary = reviewService.reverifyWithAiReflection(tenderId, vendorId, email);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @PostMapping("/reverify-all-reflection")
    public ResponseEntity<ApiResponse<TenderScoresResponse>> reverifyAllReflection(
            @PathVariable("id") UUID tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        TenderScoresResponse scores = reviewService.reverifyAllWithAiReflection(tenderId, email);
        return ResponseEntity.ok(ApiResponse.ok(scores));
    }
}

