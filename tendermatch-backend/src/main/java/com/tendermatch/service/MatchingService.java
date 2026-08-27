package com.tendermatch.service;

import com.tendermatch.dto.matching.MatchResultResponse;
import com.tendermatch.dto.matching.MatchSpecPairAiDto;
import com.tendermatch.dto.matching.MatchSpecsAiRequest;
import com.tendermatch.dto.matching.MatchSpecsAiResponse;
import com.tendermatch.dto.matching.VendorMatchSummaryDto;
import com.tendermatch.exception.BadRequestException;
import com.tendermatch.exception.ResourceNotFoundException;
import com.tendermatch.model.*;
import com.tendermatch.model.enums.SubmissionStatus;
import com.tendermatch.model.enums.TenderStatus;
import com.tendermatch.repository.*;
import com.tendermatch.service.ai.AiServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final TenderRepository tenderRepository;
    private final VendorSubmissionRepository vendorSubmissionRepository;
    private final UserRepository userRepository;
    private final TenderProductRepository tenderProductRepository;
    private final VendorProductRepository vendorProductRepository;
    private final MatchResultRepository matchResultRepository;
    private final AiServiceClient aiServiceClient;
    private final ProcessingService processingService;
    private final ScoringService scoringService;

    public MatchingService(TenderRepository tenderRepository,
                           VendorSubmissionRepository vendorSubmissionRepository,
                           UserRepository userRepository,
                           TenderProductRepository tenderProductRepository,
                           VendorProductRepository vendorProductRepository,
                           MatchResultRepository matchResultRepository,
                           AiServiceClient aiServiceClient,
                           ProcessingService processingService,
                           ScoringService scoringService) {
        this.tenderRepository = tenderRepository;
        this.vendorSubmissionRepository = vendorSubmissionRepository;
        this.userRepository = userRepository;
        this.tenderProductRepository = tenderProductRepository;
        this.vendorProductRepository = vendorProductRepository;
        this.matchResultRepository = matchResultRepository;
        this.aiServiceClient = aiServiceClient;
        this.processingService = processingService;
        this.scoringService = scoringService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private MatchingService self;

    private MatchingService getSelf() {
        return self != null ? self : this;
    }

    @Transactional
    public void triggerMatching(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = tenderRepository.findByIdWithSubmissions(tenderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id: " + tenderId));

        if (tender.getVendorSubmissions() == null || tender.getVendorSubmissions().isEmpty()) {
            throw new BadRequestException("Cannot match tender without vendor submissions");
        }

        tender.setStatus(TenderStatus.MATCHING);
        tender.setProcessingStage("MATCHING_SPECS");
        tender.setErrorMessage(null);
        for (VendorSubmission sub : tender.getVendorSubmissions()) {
            sub.setStatus(SubmissionStatus.PROCESSING);
            sub.setProcessingStage("MATCHING_SPECS");
            sub.setErrorMessage(null);
        }
        tenderRepository.save(tender);
    }

    @Async
    public void matchTenderAsync(UUID tenderId, String userEmail) {
        try {
            getSelf().matchTenderSync(tenderId, userEmail);
        } catch (Exception e) {
            log.error("Asynchronous matching failed for tenderId: {}", tenderId, e);
            try {
                getSelf().markTenderFailed(tenderId, userEmail, e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    public void matchTenderSync(UUID tenderId, String userEmail) {
        try {
            getSelf().initiateTenderMatching(tenderId, userEmail);

            Tender tender = getTender(tenderId, getUser(userEmail));
            List<TenderProduct> requirementProducts = tenderProductRepository.findByTenderWithSpecs(tender);
            if (requirementProducts == null || requirementProducts.isEmpty()) {
                log.info("Requirement products not yet extracted for tender {}. Automatically extracting documents first...", tenderId);
                processingService.processTenderSync(tenderId, userEmail);
                tender = getTender(tenderId, getUser(userEmail));
                requirementProducts = tenderProductRepository.findByTenderWithSpecs(tender);
            }

            // Ensure tender status is MATCHING throughout the vendor matching loop
            getSelf().initiateTenderMatching(tenderId, userEmail);

            if (requirementProducts == null || requirementProducts.isEmpty()) {
                log.warn("No requirement products could be extracted for tender: {}", tenderId);
                getSelf().markTenderCompleted(tenderId, userEmail);
                return;
            }

            List<UUID> submissionIds = (tender.getVendorSubmissions() != null && !tender.getVendorSubmissions().isEmpty())
                    ? tender.getVendorSubmissions().stream().map(VendorSubmission::getId).collect(Collectors.toList())
                    : processingService.getVendorSubmissionIds(tenderId, userEmail);

            for (UUID submissionId : submissionIds) {
                getSelf().matchVendorSubmission(tenderId, submissionId, requirementProducts, userEmail);
            }

            getSelf().finalizeTenderMatching(tenderId, userEmail);
            log.info("Matching successfully completed for tender: {}", tenderId);

            // Automatically compute scores & rankings so leaderboard is populated immediately
            try {
                scoringService.calculateScores(tenderId);
            } catch (Exception scoreEx) {
                log.warn("Auto score calculation after matching encountered an issue: {}", scoreEx.getMessage());
            }

        } catch (Exception e) {
            log.error("Specification matching pipeline failed for tender: {}", tenderId, e);
            getSelf().markTenderFailed(tenderId, userEmail, "Matching pipeline failed: " + e.getMessage());
        }
    }

    @Transactional
    public void initiateTenderMatching(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        tender.setStatus(TenderStatus.MATCHING);
        tender.setProcessingStage("MATCHING_SPECS");
        tender.setErrorMessage(null);
        tenderRepository.save(tender);
    }

    @Transactional
    public void matchVendorSubmission(UUID tenderId, UUID submissionId,
                                       List<TenderProduct> requirementProducts,
                                       String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        VendorSubmission submission = vendorSubmissionRepository.findById(submissionId)
                .or(() -> {
                    if (tender.getVendorSubmissions() != null) {
                        return tender.getVendorSubmissions().stream()
                                .filter(s -> submissionId.equals(s.getId()))
                                .findFirst();
                    }
                    return Optional.empty();
                })
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        submission.setProcessingStage("MATCHING_SPECS");
        submission.setStatus(SubmissionStatus.PROCESSING);
        vendorSubmissionRepository.save(submission);

        matchResultRepository.deleteByVendorSubmission(submission);

        List<VendorProduct> vendorProducts = vendorProductRepository.findByVendorSubmissionWithSpecs(submission);
        Map<String, TenderSpec> specMap = new HashMap<>();
        Map<String, VendorSpec> vendorSpecMap = new HashMap<>();
        Map<String, TenderProduct> productMap = new HashMap<>();
        Map<String, VendorProduct> vendorProductMap = new HashMap<>();

        List<MatchSpecPairAiDto> pairsToEvaluate = new ArrayList<>();

        for (TenderProduct reqProd : requirementProducts) {
            VendorProduct matchedVenProd = findMatchingVendorProduct(reqProd, vendorProducts);

            if (reqProd.getSpecifications() != null) {
                for (TenderSpec reqSpec : reqProd.getSpecifications()) {
                    String pairId = reqSpec.getId().toString();
                    specMap.put(pairId, reqSpec);
                    productMap.put(pairId, reqProd);

                    VendorSpec matchedVenSpec = null;
                    if (matchedVenProd != null && matchedVenProd.getSpecifications() != null) {
                        matchedVenSpec = findMatchingVendorSpec(reqSpec, matchedVenProd.getSpecifications());
                    }

                    if (matchedVenSpec != null) {
                        vendorSpecMap.put(pairId, matchedVenSpec);
                        vendorProductMap.put(pairId, matchedVenProd);
                    }

                    Map<String, Object> reqMap = buildReqSpecMap(reqSpec);
                    Map<String, Object> venMap = matchedVenSpec != null ? buildVenSpecMap(matchedVenSpec) : null;

                    pairsToEvaluate.add(new MatchSpecPairAiDto(pairId, reqMap, venMap));
                }
            }
        }

        if (!pairsToEvaluate.isEmpty()) {
            MatchSpecsAiRequest aiReq = new MatchSpecsAiRequest(
                    tenderId.toString(),
                    submission.getId().toString(),
                    pairsToEvaluate
            );

            MatchSpecsAiResponse aiResp = aiServiceClient.matchSpecs(aiReq);

            if (aiResp != null && aiResp.getResults() != null) {
                for (MatchSpecsAiResponse.MatchResultItemAiDto item : aiResp.getResults()) {
                    TenderSpec reqSpec = specMap.get(item.getPairId());
                    if (reqSpec == null) continue;

                    TenderProduct reqProd = productMap.get(item.getPairId());
                    VendorSpec venSpec = vendorSpecMap.get(item.getPairId());
                    VendorProduct venProd = vendorProductMap.get(item.getPairId());

                    MatchResult result = MatchResult.builder()
                            .tender(tender)
                            .vendorSubmission(submission)
                            .tenderProduct(reqProd)
                            .vendorProduct(venProd)
                            .tenderSpec(reqSpec)
                            .vendorSpec(venSpec)
                            .comparisonOperator(item.getOperator())
                            .comparisonMethod(item.getMethod())
                            .crossEncoderScore(item.getCrossEncoderScore())
                            .llmVerdict(item.getLlmVerdict())
                            .llmReasoning(item.getLlmReasoning())
                            .numericComparison(item.getNumericComparison() != null ? item.getNumericComparison().toString() : null)
                            .systemVerdict(item.getVerdict() != null ? item.getVerdict() : "AI Err")
                            .confidenceScore(item.getConfidence() != null ? item.getConfidence() : 0.50)
                            .details(item.getDetails())
                            .build();

                    matchResultRepository.save(result);
                }
            }
        }

        submission.setStatus(SubmissionStatus.MATCHED);
        submission.setProcessingStage("MATCHED");
        submission.setErrorMessage(null);
        vendorSubmissionRepository.save(submission);
    }

    @Transactional
    public void finalizeTenderMatching(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        tender.setStatus(TenderStatus.COMPLETED);
        tender.setProcessingStage("COMPLETED");
        tender.setErrorMessage(null);
        tenderRepository.save(tender);
    }

    @Transactional
    public void markTenderCompleted(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        tender.setStatus(TenderStatus.COMPLETED);
        tender.setProcessingStage("COMPLETED");
        tenderRepository.save(tender);
    }

    @Transactional
    public void markTenderFailed(UUID tenderId, String userEmail, String errorMessage) {
        try {
            User user = getUser(userEmail);
            Tender tender = getTender(tenderId, user);
            tender.setStatus(TenderStatus.FAILED);
            tender.setProcessingStage("FAILED");
            tender.setErrorMessage(errorMessage);
            tenderRepository.save(tender);
        } catch (Exception e) {
            log.error("Could not mark tender {} as FAILED: {}", tenderId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<MatchResultResponse> getMatchResults(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);

        return matchResultRepository.findByTenderWithSpecs(tender).stream()
                .map(MatchResultResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchResultResponse> getVendorResults(UUID tenderId, UUID vendorSubmissionId, String userEmail) {
        User user = getUser(userEmail);
        tenderRepository.findByIdAndUserAndDeletedAtIsNull(tenderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id: " + tenderId));

        VendorSubmission submission = vendorSubmissionRepository.findById(vendorSubmissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor submission not found with id: " + vendorSubmissionId));

        return matchResultRepository.findByVendorSubmissionWithSpecs(submission).stream()
                .map(MatchResultResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VendorMatchSummaryDto> getMatchSummary(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = tenderRepository.findByIdWithSubmissions(tenderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id: " + tenderId));

        List<VendorMatchSummaryDto> summaries = new ArrayList<>();
        if (tender.getVendorSubmissions() != null) {
            for (VendorSubmission sub : tender.getVendorSubmissions()) {
                long match = matchResultRepository.countByVendorSubmissionAndSystemVerdict(sub, "Match");
                long unmatch = matchResultRepository.countByVendorSubmissionAndSystemVerdict(sub, "Unmatch");
                long aiErr = matchResultRepository.countByVendorSubmissionAndSystemVerdict(sub, "AI Err");

                summaries.add(new VendorMatchSummaryDto(
                        sub.getId(),
                        sub.getVendorName(),
                        sub.getFileName(),
                        match,
                        unmatch,
                        aiErr
                ));
            }
        }
        return summaries;
    }

    private boolean containsWordOrPhrase(String text, String kw) {
        if (text == null || kw == null || text.isBlank() || kw.isBlank()) return false;
        String regex = "(?i)\\b" + java.util.regex.Pattern.quote(kw.trim()) + "\\b";
        return java.util.regex.Pattern.compile(regex).matcher(text).find();
    }

    private VendorProduct findMatchingVendorProduct(TenderProduct reqProd, List<VendorProduct> vendorProducts) {
        if (vendorProducts == null || vendorProducts.isEmpty() || reqProd == null) return null;

        // 1. Explicit ID link if set
        for (VendorProduct vp : vendorProducts) {
            if (vp.getMatchedTenderProduct() != null && vp.getMatchedTenderProduct().getId().equals(reqProd.getId())) {
                return vp;
            }
        }

        String reqName = (reqProd.getProductName() != null ? reqProd.getProductName() : "").trim().toLowerCase();

        // 2. Exact product name match
        for (VendorProduct vp : vendorProducts) {
            String venName = (vp.getProductName() != null ? vp.getProductName() : "").trim().toLowerCase();
            if (!reqName.isEmpty() && !venName.isEmpty() && reqName.equals(venName)) {
                return vp;
            }
        }

        // 3. Keyword / Domain Token Overlap (with strict word boundaries)
        String[][] productDomains = {
                {"laptop", "notebook", "workstation", "thinkpad", "ideapad", "macbook", "latitude", "precision", "zenbook", "elitebook", "probook", "ultrabook", "pc", "computer"},
                {"server", "rack server", "blade server", "poweredge", "proliant"},
                {"wlan", "wifi", "wi-fi", "wireless", "access point", "ap", "controller"},
                {"switch", "managed switch", "gigabit switch", "network switch", "poe switch", "cisco catalyst", "aruba switch"},
                {"firewall", "utm", "security gateway", "cyberoam", "fortigate", "sophos", "palo alto"},
                {"ups", "uninterruptible power", "battery backup", "power supply", "apc", "eaton"},
                {"cable", "utp", "patch cord", "faceplate", "casing", "pvc pipe", "rack"},
                {"monitor", "display", "screen", "projector"}
        };

        for (VendorProduct vp : vendorProducts) {
            String venName = (vp.getProductName() != null ? vp.getProductName() : "").trim().toLowerCase();
            for (String[] domain : productDomains) {
                boolean reqInDomain = false;
                for (String kw : domain) {
                    if (containsWordOrPhrase(reqName, kw)) {
                        reqInDomain = true;
                        break;
                    }
                }
                if (reqInDomain) {
                    for (String kw : domain) {
                        if (containsWordOrPhrase(venName, kw)) {
                            return vp;
                        }
                    }
                }
            }
        }

        // 4. If single requirement and single vendor in computing/laptop domain
        if (vendorProducts.size() == 1) {
            VendorProduct vp = vendorProducts.get(0);
            String venName = (vp.getProductName() != null ? vp.getProductName() : "").trim().toLowerCase();
            boolean isComputingReq = containsWordOrPhrase(reqName, "laptop") || containsWordOrPhrase(reqName, "workstation") || containsWordOrPhrase(reqName, "notebook");
            boolean isComputingVen = containsWordOrPhrase(venName, "laptop") || containsWordOrPhrase(venName, "workstation") || containsWordOrPhrase(venName, "notebook") || containsWordOrPhrase(venName, "thinkpad") || containsWordOrPhrase(venName, "ideapad");
            if (isComputingReq && isComputingVen) {
                return vp;
            }
        }

        // Distinct category / unmatched product
        return null;
    }

    private VendorSpec findMatchingVendorSpec(TenderSpec reqSpec, List<VendorSpec> vendorSpecs) {
        if (vendorSpecs == null || vendorSpecs.isEmpty() || reqSpec == null) return null;

        // 1. Explicit ID link if set
        for (VendorSpec vs : vendorSpecs) {
            if (vs.getMatchedTenderSpec() != null && vs.getMatchedTenderSpec().getId().equals(reqSpec.getId())) {
                return vs;
            }
        }

        String reqRaw = (reqSpec.getRawSpecName() != null ? reqSpec.getRawSpecName() : "").trim().toLowerCase();
        String reqCanon = (reqSpec.getCanonicalSpecName() != null ? reqSpec.getCanonicalSpecName() : "").trim().toLowerCase();
        String reqUnit = (reqSpec.getUnit() != null ? reqSpec.getUnit() : "").trim().toLowerCase();

        // 2. Canonical spec name exact match (only for valid specific canonical names with semantic validation)
        if (!reqCanon.isEmpty() && !reqCanon.equals("general") && !reqCanon.equals("custom_spec") && !reqCanon.equals("capacity") && !reqCanon.equals("size") && !reqCanon.equals("type") && !reqCanon.equals("quantity")) {
            boolean validCanon = true;
            if (reqCanon.equals("storage_capacity")) {
                validCanon = containsWordOrPhrase(reqRaw, "storage") || containsWordOrPhrase(reqRaw, "ssd") || containsWordOrPhrase(reqRaw, "hdd") || containsWordOrPhrase(reqRaw, "disk") || containsWordOrPhrase(reqRaw, "drive") || reqUnit.equals("gb") || reqUnit.equals("tb") || reqUnit.equals("mb");
            } else if (reqCanon.equals("processor_model") || reqCanon.equals("processor_speed")) {
                validCanon = containsWordOrPhrase(reqRaw, "cpu") || containsWordOrPhrase(reqRaw, "processor") || containsWordOrPhrase(reqRaw, "core") || containsWordOrPhrase(reqRaw, "clock");
            } else if (reqCanon.equals("battery_capacity")) {
                validCanon = containsWordOrPhrase(reqRaw, "battery") || containsWordOrPhrase(reqRaw, "backup") || reqUnit.equals("wh") || reqUnit.equals("mah") || reqUnit.equals("minutes") || reqUnit.equals("hours");
            }

            if (validCanon) {
                for (VendorSpec vs : vendorSpecs) {
                    String venCanon = (vs.getCanonicalSpecName() != null ? vs.getCanonicalSpecName() : "").trim().toLowerCase();
                    if (reqCanon.equals(venCanon)) {
                        return vs;
                    }
                }
            }
        }

        // 3. Raw spec name exact match (case-insensitive)
        if (!reqRaw.isEmpty()) {
            for (VendorSpec vs : vendorSpecs) {
                String venRaw = (vs.getRawSpecName() != null ? vs.getRawSpecName() : "").trim().toLowerCase();
                if (reqRaw.equals(venRaw)) {
                    return vs;
                }
            }
        }

        // 4. Strict Domain-Scoped Synonyms with word boundary checks
        String[][] specificSynonyms = {
                {"ram", "system memory", "ddr4", "ddr5", "memory capacity"},
                {"gpu", "graphics", "video card", "vram", "dedicated graphics"},
                {"cpu", "processor", "core i", "ryzen", "clock speed"},
                {"storage", "ssd", "hard drive", "disk", "nvme", "m.2 ssd", "storage capacity"},
                {"display", "screen", "panel", "display resolution", "aspect ratio", "refresh rate"},
                {"operating system", "os edition", "os license"},
                {"battery backup", "battery life", "battery capacity"},
                {"warranty", "onsite support", "premier support", "labor and spares"},
                {"chassis", "color", "casing finish"},
                {"weight", "chassis weight", "laptop weight"}
        };

        for (String[] group : specificSynonyms) {
            boolean reqHas = false;
            for (String kw : group) {
                if (containsWordOrPhrase(reqRaw, kw) || (!reqCanon.isEmpty() && containsWordOrPhrase(reqCanon, kw))) {
                    reqHas = true;
                    break;
                }
            }
            if (reqHas) {
                for (VendorSpec vs : vendorSpecs) {
                    String vRaw = (vs.getRawSpecName() != null ? vs.getRawSpecName() : "").toLowerCase();
                    String vCanon = (vs.getCanonicalSpecName() != null ? vs.getCanonicalSpecName() : "").toLowerCase();
                    for (String kw : group) {
                        if (containsWordOrPhrase(vRaw, kw) || (!vCanon.isEmpty() && containsWordOrPhrase(vCanon, kw))) {
                            return vs;
                        }
                    }
                }
            }
        }

        return null;
    }

    private Map<String, Object> buildReqSpecMap(TenderSpec spec) {
        Map<String, Object> map = new HashMap<>();
        map.put("spec_name", spec.getRawSpecName());
        map.put("canonical_name", spec.getCanonicalSpecName());
        map.put("value", spec.getSpecValue());
        map.put("numeric_value", spec.getNumericValue());
        map.put("min_value", spec.getMinValue());
        map.put("max_value", spec.getMaxValue());
        map.put("unit", spec.getUnit());
        map.put("is_numeric", spec.getIsNumeric());
        map.put("raw_text", spec.getRawText());
        return map;
    }

    private Map<String, Object> buildVenSpecMap(VendorSpec spec) {
        Map<String, Object> map = new HashMap<>();
        map.put("spec_name", spec.getRawSpecName());
        map.put("canonical_name", spec.getCanonicalSpecName());
        map.put("value", spec.getSpecValue());
        map.put("numeric_value", spec.getNumericValue());
        map.put("min_value", spec.getMinValue());
        map.put("max_value", spec.getMaxValue());
        map.put("unit", spec.getUnit());
        map.put("is_numeric", spec.getIsNumeric());
        map.put("raw_text", spec.getRawText());
        map.put("extraction_confidence", spec.getExtractionConfidence());
        map.put("normalization_confidence", spec.getNormalizationConfidence());
        return map;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private Tender getTender(UUID tenderId, User user) {
        return tenderRepository.findByIdWithSubmissions(tenderId, user)
                .or(() -> tenderRepository.findByIdAndUserAndDeletedAtIsNull(tenderId, user))
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id: " + tenderId));
    }
}
