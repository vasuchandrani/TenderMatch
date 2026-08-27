package com.tendermatch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tendermatch.dto.scoring.*;
import com.tendermatch.model.*;
import com.tendermatch.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    private final TenderRepository tenderRepository;
    private final VendorSubmissionRepository vendorSubmissionRepository;
    private final MatchResultRepository matchResultRepository;
    private final ScoringWeightRepository scoringWeightRepository;
    private final VendorScoreRepository vendorScoreRepository;
    private final TenderOutcomeRepository tenderOutcomeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ScoringService(TenderRepository tenderRepository,
                          VendorSubmissionRepository vendorSubmissionRepository,
                          MatchResultRepository matchResultRepository,
                          ScoringWeightRepository scoringWeightRepository,
                          VendorScoreRepository vendorScoreRepository,
                          TenderOutcomeRepository tenderOutcomeRepository,
                          UserRepository userRepository,
                          ObjectMapper objectMapper) {
        this.tenderRepository = tenderRepository;
        this.vendorSubmissionRepository = vendorSubmissionRepository;
        this.matchResultRepository = matchResultRepository;
        this.scoringWeightRepository = scoringWeightRepository;
        this.vendorScoreRepository = vendorScoreRepository;
        this.tenderOutcomeRepository = tenderOutcomeRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TenderOutcomeResponse recordOutcome(UUID tenderId, String userEmail, RecordOutcomeRequest request) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found: " + tenderId));
        User user = userEmail != null ? userRepository.findByEmail(userEmail).orElse(null) : null;

        VendorSubmission winner = null;
        if (request.winningVendorId() != null) {
            winner = vendorSubmissionRepository.findById(request.winningVendorId()).orElse(null);
        }

        tenderOutcomeRepository.deleteByTenderId(tenderId);

        TenderOutcome outcome = TenderOutcome.builder()
                .tender(tender)
                .winningVendor(winner)
                .outcomeNotes(request.outcomeNotes())
                .recordedBy(user)
                .recordedAt(ZonedDateTime.now())
                .build();

        tenderOutcomeRepository.save(outcome);

        return new TenderOutcomeResponse(
                outcome.getId(),
                tenderId,
                winner != null ? winner.getId() : null,
                winner != null ? winner.getVendorName() : null,
                outcome.getOutcomeNotes(),
                user != null ? user.getName() : null,
                outcome.getRecordedAt()
        );
    }

    @Transactional(readOnly = true)
    public TenderOutcomeResponse getOutcome(UUID tenderId) {
        return tenderOutcomeRepository.findByTenderId(tenderId)
                .map(o -> new TenderOutcomeResponse(
                        o.getId(),
                        tenderId,
                        o.getWinningVendor() != null ? o.getWinningVendor().getId() : null,
                        o.getWinningVendor() != null ? o.getWinningVendor().getVendorName() : null,
                        o.getOutcomeNotes(),
                        o.getRecordedBy() != null ? o.getRecordedBy().getName() : null,
                        o.getRecordedAt()
                ))
                .orElse(null);
    }

    @Transactional
    public TenderScoresResponse calculateScores(UUID tenderId) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found: " + tenderId));

        List<VendorSubmission> vendors = vendorSubmissionRepository.findByTender(tender);
        if (vendors.isEmpty()) {
            return new TenderScoresResponse(tenderId, tender.getName(), Collections.emptyList(),
                    "weighted_average", true, ZonedDateTime.now());
        }

        // Clean previous scores
        vendorScoreRepository.deleteByTenderId(tenderId);

        // Fetch custom weights lookup
        List<ScoringWeight> weightsList = scoringWeightRepository.findByTenderId(tenderId);
        Map<String, Double> weightsMap = weightsList.stream()
                .collect(Collectors.toMap(
                        ScoringWeight::getCanonicalSpecName,
                        ScoringWeight::getWeightValue,
                        (existing, replacement) -> replacement
                ));

        boolean allFinalized = vendors.stream().allMatch(VendorSubmission::isReviewLocked);

        // Calculate raw score data for each vendor
        List<VendorScoringData> vendorDataList = new ArrayList<>();

        for (VendorSubmission vendor : vendors) {
            List<MatchResult> results = matchResultRepository.findByTenderAndVendorSubmission(tender, vendor);
            VendorScoringData data = calculateSingleVendor(vendor, results, weightsMap);
            vendorDataList.add(data);
        }

        // Sort descending by score
        vendorDataList.sort((a, b) -> Double.compare(b.overallScore, a.overallScore));

        // Assign ranks with tie handling
        List<VendorScoreResponse> scoreResponses = new ArrayList<>();
        int currentRank = 1;
        for (int i = 0; i < vendorDataList.size(); i++) {
            VendorScoringData current = vendorDataList.get(i);
            if (i > 0) {
                VendorScoringData prev = vendorDataList.get(i - 1);
                if (Double.compare(current.overallScore, prev.overallScore) != 0) {
                    currentRank = i + 1;
                }
            }
            current.rank = currentRank;

            // Persist or Update VendorScore (clean upsert)
            VendorScore entity = vendorScoreRepository
                    .findByTenderIdAndVendorSubmissionId(tenderId, current.vendor.getId())
                    .orElse(VendorScore.builder().tender(tender).vendorSubmission(current.vendor).build());

            entity.setOverallScore(current.overallScore);
            entity.setRank(current.rank);
            entity.setTotalSpecs(current.totalSpecs);
            entity.setMatchedCount(current.matched);
            entity.setUnmatchedCount(current.unmatched);
            entity.setUncertainCount(current.uncertain);
            entity.setProductScores(toJson(current.productScores));
            entity.setScoreBreakdown(toJson(current.breakdowns));
            entity.setScoringMethod("weighted_average");
            entity.setWeightsSnapshot(toJson(weightsMap));
            entity.setScoredAt(ZonedDateTime.now());

            entity = vendorScoreRepository.save(entity);


            scoreResponses.add(new VendorScoreResponse(
                    entity.getId(),
                    current.vendor.getId(),
                    current.vendor.getVendorName(),
                    current.vendor.getFileName(),
                    current.overallScore,
                    current.rank,
                    current.totalSpecs,
                    current.matched,
                    current.unmatched,
                    current.uncertain,
                    current.vendor.isReviewLocked(),
                    current.productScores,
                    "weighted_average",
                    entity.getScoredAt()
            ));
        }

        return new TenderScoresResponse(
                tenderId,
                tender.getName(),
                scoreResponses,
                "weighted_average",
                allFinalized,
                ZonedDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public TenderScoresResponse getTenderScores(UUID tenderId) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found: " + tenderId));

        List<VendorScore> savedScores = vendorScoreRepository.findByTenderIdOrderByRankAsc(tenderId);
        if (savedScores.isEmpty()) {
            return calculateScores(tenderId);
        }

        List<VendorSubmission> vendors = vendorSubmissionRepository.findByTender(tender);
        boolean allFinalized = vendors.stream().allMatch(VendorSubmission::isReviewLocked);

        List<VendorScoreResponse> scoreResponses = savedScores.stream().map(vs -> {
            List<ProductScoreDto> productScores = parseProductScores(vs.getProductScores());
            return new VendorScoreResponse(
                    vs.getId(),
                    vs.getVendorSubmission().getId(),
                    vs.getVendorSubmission().getVendorName(),
                    vs.getVendorSubmission().getFileName(),
                    vs.getOverallScore(),
                    vs.getRank(),
                    vs.getTotalSpecs(),
                    vs.getMatchedCount(),
                    vs.getUnmatchedCount(),
                    vs.getUncertainCount(),
                    vs.getVendorSubmission().isReviewLocked(),
                    productScores,
                    vs.getScoringMethod(),
                    vs.getScoredAt()
            );
        }).collect(Collectors.toList());

        return new TenderScoresResponse(
                tenderId,
                tender.getName(),
                scoreResponses,
                savedScores.get(0).getScoringMethod(),
                allFinalized,
                savedScores.get(0).getScoredAt()
        );
    }

    @Transactional(readOnly = true)
    public VendorScoreResponse getVendorScore(UUID tenderId, UUID vendorSubmissionId) {
        VendorScore vs = vendorScoreRepository.findByTenderIdAndVendorSubmissionId(tenderId, vendorSubmissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Score not found for vendor: " + vendorSubmissionId));

        List<ProductScoreDto> productScores = parseProductScores(vs.getProductScores());
        return new VendorScoreResponse(
                vs.getId(),
                vs.getVendorSubmission().getId(),
                vs.getVendorSubmission().getVendorName(),
                vs.getVendorSubmission().getFileName(),
                vs.getOverallScore(),
                vs.getRank(),
                vs.getTotalSpecs(),
                vs.getMatchedCount(),
                vs.getUnmatchedCount(),
                vs.getUncertainCount(),
                vs.getVendorSubmission().isReviewLocked(),
                productScores,
                vs.getScoringMethod(),
                vs.getScoredAt()
        );
    }

    @Transactional(readOnly = true)
    public ComparisonMatrixResponse getComparisonMatrix(UUID tenderId) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found: " + tenderId));

        List<VendorSubmission> vendors = vendorSubmissionRepository.findByTender(tender);
        List<String> vendorNames = vendors.stream().map(VendorSubmission::getVendorName).collect(Collectors.toList());

        List<MatchResult> allResults = matchResultRepository.findByTender(tender);
        List<ScoringWeight> weightsList = scoringWeightRepository.findByTenderId(tenderId);
        Map<String, ScoringWeight> weightMap = weightsList.stream()
                .collect(Collectors.toMap(ScoringWeight::getCanonicalSpecName, w -> w, (a, b) -> b));

        // Group by tender requirement spec ID
        Map<UUID, List<MatchResult>> byTenderSpec = allResults.stream()
                .filter(r -> r.getTenderSpec() != null)
                .collect(Collectors.groupingBy(r -> r.getTenderSpec().getId()));

        List<ComparisonRowDto> rows = new ArrayList<>();

        for (Map.Entry<UUID, List<MatchResult>> entry : byTenderSpec.entrySet()) {
            List<MatchResult> specResults = entry.getValue();
            MatchResult sample = specResults.get(0);
            TenderSpec reqSpec = sample.getTenderSpec();

            String canonicalName = reqSpec.getCanonicalSpecName() != null ? reqSpec.getCanonicalSpecName() : reqSpec.getRawSpecName();
            double weight = resolveSpecWeight(canonicalName, weightsList.stream()
                    .collect(Collectors.toMap(ScoringWeight::getCanonicalSpecName, ScoringWeight::getWeightValue, (a, b) -> b)));
            String category = resolveSpecCategory(canonicalName, weightMap);

            // Map each vendor's cell
            Map<UUID, MatchResult> vendorResultMap = specResults.stream()
                    .collect(Collectors.toMap(r -> r.getVendorSubmission().getId(), r -> r, (a, b) -> b));

            List<ComparisonCellDto> cells = new ArrayList<>();
            for (VendorSubmission v : vendors) {
                MatchResult vr = vendorResultMap.get(v.getId());
                if (vr != null) {
                    String effVerdict = vr.getHumanVerdict() != null ? vr.getHumanVerdict() : vr.getSystemVerdict();
                    double cellScore = verdictToScore(effVerdict);
                    String offVal = vr.getVendorSpec() != null ? vr.getVendorSpec().getSpecValue() : "-";
                    String offUnit = vr.getVendorSpec() != null ? vr.getVendorSpec().getUnit() : "";

                    cells.add(new ComparisonCellDto(
                            v.getId(),
                            v.getVendorName(),
                            offVal,
                            offUnit,
                            vr.getSystemVerdict(),
                            vr.getHumanVerdict(),
                            vr.getConfidenceScore(),
                            cellScore,
                            weight
                    ));
                } else {
                    cells.add(new ComparisonCellDto(
                            v.getId(),
                            v.getVendorName(),
                            "-",
                            "",
                            "Unmatch",
                            null,
                            1.0,
                            0.0,
                            weight
                    ));
                }
            }

            rows.add(new ComparisonRowDto(
                    reqSpec.getId(),
                    reqSpec.getRawSpecName(),
                    canonicalName,
                    reqSpec.getSpecValue(),
                    reqSpec.getUnit(),
                    sample.getComparisonOperator(),
                    weight,
                    category,
                    cells
            ));
        }

        return new ComparisonMatrixResponse(tenderId, tender.getName(), vendorNames, rows);
    }

    private VendorScoringData calculateSingleVendor(VendorSubmission vendor, List<MatchResult> results,
                                                    Map<String, Double> weightsMap) {
        double totalWeightedScore = 0.0;
        double totalWeight = 0.0;
        int matched = 0;
        int unmatched = 0;
        int uncertain = 0;

        Map<UUID, ProductAccumulator> productMap = new LinkedHashMap<>();
        List<Map<String, Object>> breakdowns = new ArrayList<>();

        for (MatchResult res : results) {
            String canonicalName = res.getTenderSpec() != null && res.getTenderSpec().getCanonicalSpecName() != null
                    ? res.getTenderSpec().getCanonicalSpecName()
                    : (res.getTenderSpec() != null ? res.getTenderSpec().getRawSpecName() : "unknown");

            double weight = resolveSpecWeight(canonicalName, weightsMap);
            String verdict = res.getHumanVerdict() != null ? res.getHumanVerdict() : res.getSystemVerdict();
            double score = verdictToScore(verdict);

            if ("Match".equalsIgnoreCase(verdict)) matched++;
            else if ("Unmatch".equalsIgnoreCase(verdict)) unmatched++;
            else uncertain++;

            totalWeightedScore += (weight * score);
            totalWeight += weight;

            // Product tracking
            if (res.getTenderProduct() != null) {
                TenderProduct prod = res.getTenderProduct();
                ProductAccumulator acc = productMap.computeIfAbsent(prod.getId(), k -> new ProductAccumulator(prod.getId(), prod.getProductName()));
                acc.totalSpecs++;
                if ("Match".equalsIgnoreCase(verdict)) acc.matched++;
                else if ("Unmatch".equalsIgnoreCase(verdict)) acc.unmatched++;
                else acc.uncertain++;
                acc.weightedSum += (weight * score);
                acc.weightSum += weight;
            }

            breakdowns.add(Map.of(
                    "specName", canonicalName,
                    "verdict", verdict,
                    "weight", weight,
                    "score", score
            ));
        }

        double rawScore = totalWeight > 0 ? (totalWeightedScore / totalWeight * 100.0) : 0.0;
        double overallScore = Math.round(rawScore * 10.0) / 10.0;

        List<ProductScoreDto> productScores = productMap.values().stream().map(acc -> {
            double pScore = acc.weightSum > 0 ? (acc.weightedSum / acc.weightSum * 100.0) : 0.0;
            return new ProductScoreDto(
                    acc.productId,
                    acc.productName,
                    Math.round(pScore * 10.0) / 10.0,
                    acc.totalSpecs,
                    acc.matched,
                    acc.unmatched,
                    acc.uncertain
            );
        }).collect(Collectors.toList());

        VendorScoringData data = new VendorScoringData();
        data.vendor = vendor;
        data.overallScore = overallScore;
        data.totalSpecs = results.size();
        data.matched = matched;
        data.unmatched = unmatched;
        data.uncertain = uncertain;
        data.productScores = productScores;
        data.breakdowns = breakdowns;
        return data;
    }

    private double verdictToScore(String verdict) {
        if (verdict == null) return 0.0;
        return switch (verdict.trim()) {
            case "Match" -> 1.0;
            case "AI Can't Predict", "AI Err" -> 0.5;
            default -> 0.0;
        };
    }

    public static double resolveSpecWeight(String specName, Map<String, Double> weightsMap) {
        if (weightsMap != null && !weightsMap.isEmpty()) {
            if (weightsMap.containsKey(specName)) {
                return weightsMap.get(specName);
            }
            for (Map.Entry<String, Double> entry : weightsMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(specName)) {
                    return entry.getValue();
                }
            }
        }
        return 1.0;
    }

    public static String resolveSpecCategory(String specName, Map<String, ScoringWeight> weightMap) {
        if (weightMap != null) {
            if (weightMap.containsKey(specName)) {
                return weightMap.get(specName).getWeightCategory();
            }
            for (Map.Entry<String, ScoringWeight> entry : weightMap.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(specName)) {
                    return entry.getValue().getWeightCategory();
                }
            }
        }
        String lower = specName != null ? specName.toLowerCase() : "";
        if (lower.contains("ram") || lower.contains("memory") || lower.contains("processor")
                || lower.contains("cpu") || lower.contains("storage") || lower.contains("ssd") || lower.contains("core")) {
            return "critical";
        }
        if (lower.contains("display") || lower.contains("screen") || lower.contains("battery")
                || lower.contains("warranty") || lower.contains("operating system") || lower.contains("os")) {
            return "major";
        }
        if (lower.contains("color") || lower.contains("colour") || lower.contains("finish")
                || lower.contains("chassis") || lower.contains("sticker") || lower.contains("bag")) {
            return "cosmetic";
        }
        return "medium";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private List<ProductScoreDto> parseProductScores(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, List.class);
            return list.stream().map(m -> new ProductScoreDto(
                    m.get("tenderProductId") != null ? UUID.fromString(m.get("tenderProductId").toString()) : null,
                    (String) m.get("productName"),
                    ((Number) m.get("score")).doubleValue(),
                    ((Number) m.get("totalSpecs")).intValue(),
                    ((Number) m.get("matched")).intValue(),
                    ((Number) m.get("unmatched")).intValue(),
                    ((Number) m.get("uncertain")).intValue()
            )).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to parse product scores JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static class ProductAccumulator {
        UUID productId;
        String productName;
        int totalSpecs = 0;
        int matched = 0;
        int unmatched = 0;
        int uncertain = 0;
        double weightedSum = 0.0;
        double weightSum = 0.0;

        ProductAccumulator(UUID productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }
    }

    private static class VendorScoringData {
        VendorSubmission vendor;
        double overallScore;
        int rank;
        int totalSpecs;
        int matched;
        int unmatched;
        int uncertain;
        List<ProductScoreDto> productScores;
        List<Map<String, Object>> breakdowns;
    }
}
