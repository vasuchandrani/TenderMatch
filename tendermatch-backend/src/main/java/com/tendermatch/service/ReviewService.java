package com.tendermatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tendermatch.dto.review.*;
import com.tendermatch.dto.scoring.TenderScoresResponse;
import com.tendermatch.model.*;
import com.tendermatch.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TenderRepository tenderRepository;
    private final VendorSubmissionRepository vendorSubmissionRepository;
    private final MatchResultRepository matchResultRepository;
    private final CorrectionLogRepository correctionLogRepository;
    private final UserRepository userRepository;
    private final com.tendermatch.service.ai.AiServiceClient aiServiceClient;
    private final ScoringService scoringService;
    private final ScoringWeightRepository scoringWeightRepository;

    public ReviewService(TenderRepository tenderRepository,
                         VendorSubmissionRepository vendorSubmissionRepository,
                         MatchResultRepository matchResultRepository,
                         CorrectionLogRepository correctionLogRepository,
                         UserRepository userRepository,
                         com.tendermatch.service.ai.AiServiceClient aiServiceClient,
                         ScoringService scoringService,
                         ScoringWeightRepository scoringWeightRepository) {
        this.tenderRepository = tenderRepository;
        this.vendorSubmissionRepository = vendorSubmissionRepository;
        this.matchResultRepository = matchResultRepository;
        this.correctionLogRepository = correctionLogRepository;
        this.userRepository = userRepository;
        this.aiServiceClient = aiServiceClient;
        this.scoringService = scoringService;
        this.scoringWeightRepository = scoringWeightRepository;
    }


    @Transactional(readOnly = true)
    public List<ReviewVendorDto> getTenderReviewData(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        List<VendorSubmission> vendors = vendorSubmissionRepository.findByTender(tender);

        return vendors.stream()
                .map(v -> getVendorReviewData(tenderId, v.getId(), userEmail))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReviewVendorDto getVendorReviewData(UUID tenderId, UUID vendorId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        VendorSubmission vendor = vendorSubmissionRepository.findByIdAndTender(vendorId, tender)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor submission not found"));

        List<MatchResult> results = matchResultRepository.findByTenderAndVendorSubmission(tender, vendor);

        Map<UUID, List<MatchResult>> byProduct = results.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getTenderProduct().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ReviewProductDto> products = new ArrayList<>();
        for (Map.Entry<UUID, List<MatchResult>> entry : byProduct.entrySet()) {
            List<MatchResult> productResults = entry.getValue();
            MatchResult first = productResults.get(0);
            TenderProduct tenderProduct = first.getTenderProduct();
            VendorProduct vendorProduct = first.getVendorProduct();

            List<ReviewSpecPairDto> specPairs = productResults.stream()
                    .map(this::mapToSpecPairDto)
                    .collect(Collectors.toList());

            products.add(new ReviewProductDto(
                    tenderProduct.getId(),
                    tenderProduct.getProductName(),
                    vendorProduct != null ? vendorProduct.getId() : null,
                    vendorProduct != null ? vendorProduct.getProductName() : null,
                    specPairs
            ));
        }

        ReviewProgressResponse progress = calculateProgress(tenderId, vendorId, results, vendor.isReviewLocked());

        return new ReviewVendorDto(
                vendor.getId(),
                vendor.getVendorName() != null ? vendor.getVendorName() : vendor.getFileName(),
                vendor.getFileName(),
                vendor.isReviewLocked(),
                products,
                progress
        );
    }

    @Transactional(readOnly = true)
    public ReviewProgressResponse getVendorReviewProgress(UUID tenderId, UUID vendorId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        VendorSubmission vendor = vendorSubmissionRepository.findByIdAndTender(vendorId, tender)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor submission not found"));

        List<MatchResult> results = matchResultRepository.findByTenderAndVendorSubmission(tender, vendor);
        return calculateProgress(tenderId, vendorId, results, vendor.isReviewLocked());
    }

    public SubmitVerdictResponse submitVerdict(UUID tenderId, UUID resultId, SubmitVerdictRequest request, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);

        MatchResult result = matchResultRepository.findById(resultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match result not found"));

        if (!result.getTender().getId().equals(tender.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Match result does not belong to tender");
        }

        VendorSubmission vendor = result.getVendorSubmission();
        if (vendor.isReviewLocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review is finalized and locked for this vendor");
        }

        boolean isOverride = !result.getSystemVerdict().equalsIgnoreCase(request.humanVerdict());

        // Update match result
        result.setHumanVerdict(request.humanVerdict());
        result.setReviewedAt(ZonedDateTime.now());
        result.setReviewedBy(user);
        matchResultRepository.save(result);

        // Build snapshot context for active learning
        String contextJson = buildContextSnapshot(result);

        // Save correction log
        CorrectionLog correctionLog = CorrectionLog.builder()
                .matchResult(result)
                .tender(result.getTender())
                .user(user)
                .systemVerdict(result.getSystemVerdict())
                .humanVerdict(request.humanVerdict())
                .isOverride(isOverride)
                .isAutoConfirmed(false)
                .context(contextJson)
                .userComment(request.comment())
                .build();
        correctionLogRepository.save(correctionLog);

        // Calculate progress and find next unreviewed result
        List<MatchResult> vendorResults = matchResultRepository.findByTenderAndVendorSubmission(tender, vendor);
        ReviewProgressResponse progress = calculateProgress(tenderId, vendor.getId(), vendorResults, vendor.isReviewLocked());

        UUID nextResultId = vendorResults.stream()
                .filter(r -> r.getHumanVerdict() == null && !r.getId().equals(resultId))
                .map(MatchResult::getId)
                .findFirst()
                .orElse(null);

        return new SubmitVerdictResponse(
                result.getId(),
                result.getSystemVerdict(),
                result.getHumanVerdict(),
                isOverride,
                request.comment(),
                nextResultId,
                progress,
                result.getReviewedAt()
        );
    }

    public ReviewProgressResponse bulkConfirmRemaining(UUID tenderId, UUID vendorId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        VendorSubmission vendor = vendorSubmissionRepository.findByIdAndTender(vendorId, tender)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor submission not found"));

        if (vendor.isReviewLocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Review is finalized and locked for this vendor");
        }

        List<MatchResult> results = matchResultRepository.findByTenderAndVendorSubmission(tender, vendor);
        ZonedDateTime now = ZonedDateTime.now();

        for (MatchResult result : results) {
            if (result.getHumanVerdict() == null) {
                result.setHumanVerdict(result.getSystemVerdict());
                result.setReviewedAt(now);
                result.setReviewedBy(user);
                matchResultRepository.save(result);

                String contextJson = buildContextSnapshot(result);
                CorrectionLog log = CorrectionLog.builder()
                        .matchResult(result)
                        .tender(result.getTender())
                        .user(user)
                        .systemVerdict(result.getSystemVerdict())
                        .humanVerdict(result.getSystemVerdict())
                        .isOverride(false)
                        .isAutoConfirmed(true)
                        .context(contextJson)
                        .userComment("Bulk confirmed as system verdict")
                        .build();
                correctionLogRepository.save(log);
            }
        }

        return calculateProgress(tenderId, vendorId, results, vendor.isReviewLocked());
    }

    public ReviewSummaryResponse finalizeReview(UUID tenderId, UUID vendorId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        VendorSubmission vendor = vendorSubmissionRepository.findByIdAndTender(vendorId, tender)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor submission not found"));

        vendor.setReviewLocked(true);
        vendor.setReviewFinalizedAt(ZonedDateTime.now());
        vendor.setReviewFinalizedBy(user);
        vendorSubmissionRepository.save(vendor);

        // Deterministically recompute overall scores and leaderboard ranking
        scoringService.calculateScores(tenderId);

        return getReviewSummary(tenderId, vendorId, userEmail);
    }

    public ReviewSummaryResponse reverifyWithAiReflection(UUID tenderId, UUID vendorId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        VendorSubmission vendor = vendorSubmissionRepository.findByIdAndTender(vendorId, tender)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor submission not found"));

        List<MatchResult> results = matchResultRepository.findByTenderAndVendorSubmission(tender, vendor);

        List<Map<String, Object>> reviewedSpecs = new ArrayList<>();
        for (MatchResult r : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId().toString());
            item.put("raw_spec_name", r.getTenderSpec().getRawSpecName());
            item.put("system_verdict", r.getSystemVerdict());
            item.put("human_verdict", r.getHumanVerdict() != null ? r.getHumanVerdict() : r.getSystemVerdict());
            item.put("required_value", r.getTenderSpec().getSpecValue());
            item.put("required_text", r.getTenderSpec().getRawText());
            item.put("offered_value", r.getVendorSpec() != null ? r.getVendorSpec().getSpecValue() : "");
            item.put("offered_text", r.getVendorSpec() != null ? r.getVendorSpec().getRawText() : "");
            item.put("confidence_score", r.getConfidenceScore());
            reviewedSpecs.add(item);
        }

        Map<String, Object> aiReq = new HashMap<>();
        aiReq.put("tender_id", tenderId.toString());
        aiReq.put("vendor_submission_id", vendorId.toString());
        aiReq.put("reviewed_specs", reviewedSpecs);

        try {
            Map<String, Object> aiResp = aiServiceClient.reverifyWithReflection(aiReq);
            List<Map<String, Object>> certifiedList = (List<Map<String, Object>>) aiResp.get("certified_results");
            if (certifiedList != null) {
                Map<UUID, Map<String, Object>> certMap = new HashMap<>();
                for (Map<String, Object> c : certifiedList) {
                    if (c.get("id") != null) {
                        certMap.put(UUID.fromString((String) c.get("id")), c);
                    }
                }

                for (MatchResult r : results) {
                    Map<String, Object> cert = certMap.get(r.getId());
                    if (cert != null) {
                        String certVerdict = (String) cert.get("certified_verdict");
                        String auditNote = (String) cert.get("reflection_audit_note");
                        if (certVerdict != null) {
                            r.setHumanVerdict(certVerdict);
                        }
                        if (auditNote != null) {
                            r.setDetails("[AI Reflection Certified] " + auditNote);
                        }
                        matchResultRepository.save(r);
                    }
                }
            }

            // Persist LLM cognitive criticality weights for the tender
            Object critWeightsObj = aiResp.get("criticality_weights");
            if (critWeightsObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> critWeights = (Map<String, Object>) critWeightsObj;
                for (Map.Entry<String, Object> entry : critWeights.entrySet()) {
                    String specName = entry.getKey();
                    if (entry.getValue() instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> details = (Map<String, Object>) entry.getValue();
                        String category = (String) details.getOrDefault("category", "medium");
                        Double weightVal = 1.0;
                        if (details.get("weight") instanceof Number) {
                            weightVal = ((Number) details.get("weight")).doubleValue();
                        }

                        ScoringWeight sw = scoringWeightRepository
                                .findByTenderIdAndCanonicalSpecName(tenderId, specName)
                                .orElse(ScoringWeight.builder()
                                        .tender(tender)
                                        .user(user)
                                        .canonicalSpecName(specName)
                                        .build());
                        sw.setWeightCategory(category.toLowerCase());
                        sw.setWeightValue(weightVal);
                        scoringWeightRepository.save(sw);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Pass 2 reflection call failed, applying human verdicts directly: {}", e.getMessage());
        }

        // Lock vendor review and recalculate leaderboard scores
        vendor.setReviewLocked(true);
        vendor.setReviewFinalizedAt(ZonedDateTime.now());
        vendor.setReviewFinalizedBy(user);
        vendorSubmissionRepository.save(vendor);

        scoringService.calculateScores(tenderId);

        return getReviewSummary(tenderId, vendorId, userEmail);
    }

    @Transactional
    public TenderScoresResponse reverifyAllWithAiReflection(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        List<VendorSubmission> vendors = tender.getVendorSubmissions();
        if (vendors != null) {
            for (VendorSubmission vendor : vendors) {
                List<MatchResult> results = matchResultRepository.findByTenderAndVendorSubmission(tender, vendor);
                boolean hasReviews = results.stream().anyMatch(r -> r.getHumanVerdict() != null);
                if (hasReviews && !vendor.isReviewLocked()) {
                    try {
                        reverifyWithAiReflection(tenderId, vendor.getId(), userEmail);
                    } catch (Exception e) {
                        log.warn("Failed reverifying reflection for vendor {}: {}", vendor.getId(), e.getMessage());
                    }
                }
            }
        }
        return scoringService.calculateScores(tenderId);
    }


    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(UUID tenderId, UUID vendorId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        VendorSubmission vendor = vendorSubmissionRepository.findByIdAndTender(vendorId, tender)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor submission not found"));

        List<MatchResult> results = matchResultRepository.findByTenderAndVendorSubmission(tender, vendor);

        long matchCount = results.stream().filter(r -> "Match".equalsIgnoreCase(effectiveVerdict(r))).count();
        long unmatchCount = results.stream().filter(r -> "Unmatch".equalsIgnoreCase(effectiveVerdict(r))).count();
        long aiErrCount = results.stream().filter(r -> "AI Err".equalsIgnoreCase(effectiveVerdict(r)) || "AI Can't Predict".equalsIgnoreCase(effectiveVerdict(r))).count();
        long overrideCount = results.stream().filter(this::isResultOverridden).count();

        Map<String, List<MatchResult>> byProductName = results.stream()
                .collect(Collectors.groupingBy(r -> r.getTenderProduct().getProductName(), LinkedHashMap::new, Collectors.toList()));

        List<ReviewSummaryResponse.ProductSummaryDto> productSummaries = new ArrayList<>();
        for (Map.Entry<String, List<MatchResult>> entry : byProductName.entrySet()) {
            List<MatchResult> pResults = entry.getValue();
            long pMatch = pResults.stream().filter(r -> "Match".equalsIgnoreCase(effectiveVerdict(r))).count();
            long pUnmatch = pResults.stream().filter(r -> "Unmatch".equalsIgnoreCase(effectiveVerdict(r))).count();
            long pAiErr = pResults.stream().filter(r -> "AI Err".equalsIgnoreCase(effectiveVerdict(r)) || "AI Can't Predict".equalsIgnoreCase(effectiveVerdict(r))).count();
            long pOverride = pResults.stream().filter(this::isResultOverridden).count();

            List<ReviewSummaryResponse.SpecVerdictSummaryDto> specSummaries = pResults.stream()
                    .map(r -> new ReviewSummaryResponse.SpecVerdictSummaryDto(
                            r.getTenderSpec().getRawSpecName(),
                            r.getSystemVerdict(),
                            r.getHumanVerdict(),
                            isResultOverridden(r),
                            r.getDetails()
                    ))
                    .collect(Collectors.toList());

            productSummaries.add(new ReviewSummaryResponse.ProductSummaryDto(
                    entry.getKey(), pMatch, pUnmatch, pAiErr, pOverride, specSummaries
            ));
        }

        return new ReviewSummaryResponse(
                tenderId,
                vendorId,
                vendor.getVendorName() != null ? vendor.getVendorName() : vendor.getFileName(),
                vendor.isReviewLocked(),
                matchCount,
                unmatchCount,
                aiErrCount,
                results.size(),
                overrideCount,
                productSummaries
        );
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Tender getTender(UUID tenderId, User user) {
        return tenderRepository.findByIdAndUserAndDeletedAtIsNull(tenderId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found"));
    }

    private ReviewSpecPairDto mapToSpecPairDto(MatchResult r) {
        TenderSpec req = r.getTenderSpec();
        VendorSpec off = r.getVendorSpec();
        boolean isReviewed = r.getHumanVerdict() != null;
        boolean isOverride = isResultOverridden(r);

        String userComment = "";
        List<CorrectionLog> logs = correctionLogRepository.findByMatchResultIdOrderByCreatedAtDesc(r.getId());
        if (!logs.isEmpty() && logs.get(0).getUserComment() != null) {
            userComment = logs.get(0).getUserComment();
        }

        String aiReasoning = r.getLlmReasoning() != null && !r.getLlmReasoning().isBlank()
                ? r.getLlmReasoning()
                : (r.getDetails() != null ? r.getDetails() : "");

        return new ReviewSpecPairDto(
                r.getId(),
                req.getId(),
                req.getRawSpecName(),
                req.getCanonicalSpecName(),
                req.getSpecValue(),
                req.getUnit(),
                req.getRawText(),

                off != null ? off.getId() : null,
                off != null ? off.getRawSpecName() : "Not Offered",
                off != null ? off.getSpecValue() : null,
                off != null ? off.getUnit() : null,
                off != null ? off.getRawText() : "No specification provided in vendor bid",

                r.getSystemVerdict(),
                r.getConfidenceScore(),
                r.getComparisonMethod(),
                r.getComparisonOperator(),
                aiReasoning,

                r.getHumanVerdict(),
                isReviewed,
                isOverride,
                userComment
        );
    }

    private ReviewProgressResponse calculateProgress(UUID tenderId, UUID vendorId, List<MatchResult> results, boolean isLocked) {
        long total = results.size();
        long reviewed = results.stream().filter(r -> r.getHumanVerdict() != null).count();
        long overrides = results.stream().filter(this::isResultOverridden).count();

        double percentage = 0.0;
        if (total > 0) {
            percentage = BigDecimal.valueOf((double) reviewed / total * 100.0)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new ReviewProgressResponse(tenderId, vendorId, reviewed, total, percentage, overrides, isLocked);
    }

    private boolean isResultOverridden(MatchResult r) {
        return r.getHumanVerdict() != null && !r.getHumanVerdict().equalsIgnoreCase(r.getSystemVerdict());
    }

    private String effectiveVerdict(MatchResult r) {
        return r.getHumanVerdict() != null ? r.getHumanVerdict() : r.getSystemVerdict();
    }

    private String buildContextSnapshot(MatchResult r) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            TenderSpec ts = r.getTenderSpec();
            VendorSpec vs = r.getVendorSpec();

            Map<String, Object> reqMap = new LinkedHashMap<>();
            reqMap.put("raw_spec_name", ts.getRawSpecName());
            reqMap.put("canonical_key", ts.getCanonicalSpecName());
            reqMap.put("value", ts.getSpecValue());
            reqMap.put("unit", ts.getUnit());
            reqMap.put("raw_text", ts.getRawText());
            map.put("tender_spec", reqMap);

            if (vs != null) {
                Map<String, Object> offMap = new LinkedHashMap<>();
                offMap.put("raw_spec_name", vs.getRawSpecName());
                offMap.put("canonical_key", vs.getCanonicalSpecName());
                offMap.put("value", vs.getSpecValue());
                offMap.put("unit", vs.getUnit());
                offMap.put("raw_text", vs.getRawText());
                map.put("vendor_spec", offMap);
            } else {
                map.put("vendor_spec", null);
            }

            Map<String, Object> matchInfo = new LinkedHashMap<>();
            matchInfo.put("operator", r.getComparisonOperator());
            matchInfo.put("method", r.getComparisonMethod());
            matchInfo.put("confidence_score", r.getConfidenceScore());
            matchInfo.put("system_verdict", r.getSystemVerdict());
            matchInfo.put("details", r.getDetails());
            map.put("matching", matchInfo);

            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize context snapshot for result {}", r.getId(), e);
            return "{}";
        }
    }
}
