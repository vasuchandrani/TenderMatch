package com.tendermatch.controller;

import com.tendermatch.dto.common.ApiResponse;
import com.tendermatch.dto.matching.MatchResultResponse;
import com.tendermatch.dto.matching.VendorMatchSummaryDto;
import com.tendermatch.service.MatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenders")
public class MatchController {

    private final MatchingService matchingService;

    public MatchController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/{id}/match")
    public ResponseEntity<ApiResponse<String>> triggerMatch(
            @PathVariable("id") UUID tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        matchingService.triggerMatching(tenderId, email);

        // Run matching asynchronously
        matchingService.matchTenderAsync(tenderId, email);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Matching process initiated successfully"));
    }

    @GetMapping("/{id}/match-status")
    public ResponseEntity<ApiResponse<List<VendorMatchSummaryDto>>> getMatchStatus(
            @PathVariable("id") UUID tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<VendorMatchSummaryDto> summary = matchingService.getMatchSummary(tenderId, email);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<ApiResponse<List<MatchResultResponse>>> getResults(
            @PathVariable("id") UUID tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<MatchResultResponse> results = matchingService.getMatchResults(tenderId, email);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/{id}/vendors/{vendorId}/results")
    public ResponseEntity<ApiResponse<List<MatchResultResponse>>> getVendorResults(
            @PathVariable("id") UUID tenderId,
            @PathVariable("vendorId") UUID vendorId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<MatchResultResponse> results = matchingService.getVendorResults(tenderId, vendorId, email);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/{id}/results/summary")
    public ResponseEntity<ApiResponse<List<VendorMatchSummaryDto>>> getResultsSummary(
            @PathVariable("id") UUID tenderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<VendorMatchSummaryDto> summary = matchingService.getMatchSummary(tenderId, email);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
