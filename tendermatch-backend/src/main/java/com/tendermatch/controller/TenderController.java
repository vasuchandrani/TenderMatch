package com.tendermatch.controller;

import com.tendermatch.dto.common.ApiResponse;
import com.tendermatch.dto.tender.CreateTenderRequest;
import com.tendermatch.dto.tender.TenderResponse;
import com.tendermatch.dto.tender.VendorSubmissionResponse;
import com.tendermatch.service.TenderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenders")
public class TenderController {

    private final TenderService tenderService;

    public TenderController(TenderService tenderService) {
        this.tenderService = tenderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenderResponse>> createTender(
            @Valid @RequestBody CreateTenderRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TenderResponse response = tenderService.createTender(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Tender session created"));
    }

    @PostMapping(value = "/{id}/requirement", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<TenderResponse>> uploadRequirementPdf(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TenderResponse response = tenderService.uploadRequirementPdf(id, file, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response, "Requirement PDF uploaded successfully"));
    }

    @PostMapping(value = "/{id}/vendors", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<VendorSubmissionResponse>>> uploadVendorPdfs(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<VendorSubmissionResponse> response = tenderService.uploadVendorPdfs(id, files, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response, "Vendor PDFs uploaded successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenderResponse>> getTenderById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TenderResponse response = tenderService.getTenderById(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response, "Tender details retrieved"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TenderResponse>>> listTenders(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<TenderResponse> response = tenderService.listTenders(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Tenders listed successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTender(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        tenderService.deleteTender(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(null, "Tender deleted successfully"));
    }
}
