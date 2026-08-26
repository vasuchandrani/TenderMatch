package com.tendermatch.service;

import com.tendermatch.dto.pipeline.ExtractedProductDto;
import com.tendermatch.dto.pipeline.ExtractedSpecDto;
import com.tendermatch.dto.pipeline.ProcessDocumentAiRequest;
import com.tendermatch.dto.pipeline.ProcessDocumentAiResponse;
import com.tendermatch.dto.pipeline.ProcessingStatusResponse;
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

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingService.class);

    private final TenderRepository tenderRepository;
    private final VendorSubmissionRepository vendorSubmissionRepository;
    private final UserRepository userRepository;
    private final TenderProductRepository tenderProductRepository;
    private final TenderSpecRepository tenderSpecRepository;
    private final VendorProductRepository vendorProductRepository;
    private final VendorSpecRepository vendorSpecRepository;
    private final MinioStorageService storageService;
    private final AiServiceClient aiServiceClient;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private ProcessingService self;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private MatchResultRepository matchResultRepository;

    private ProcessingService getSelf() {
        return self != null ? self : this;
    }

    public ProcessingService(TenderRepository tenderRepository,
                             VendorSubmissionRepository vendorSubmissionRepository,
                             UserRepository userRepository,
                             TenderProductRepository tenderProductRepository,
                             TenderSpecRepository tenderSpecRepository,
                             VendorProductRepository vendorProductRepository,
                             VendorSpecRepository vendorSpecRepository,
                             MinioStorageService storageService,
                             AiServiceClient aiServiceClient) {
        this.tenderRepository = tenderRepository;
        this.vendorSubmissionRepository = vendorSubmissionRepository;
        this.userRepository = userRepository;
        this.tenderProductRepository = tenderProductRepository;
        this.tenderSpecRepository = tenderSpecRepository;
        this.vendorProductRepository = vendorProductRepository;
        this.vendorSpecRepository = vendorSpecRepository;
        this.storageService = storageService;
        this.aiServiceClient = aiServiceClient;
    }

    @Transactional
    public ProcessingStatusResponse triggerProcessing(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);

        if (tender.getRequirementFileKey() == null) {
            throw new BadRequestException("Cannot process tender without a requirement PDF uploaded");
        }

        tender.setStatus(TenderStatus.PROCESSING);
        tender.setProcessingStage("OCR_REQUIREMENT");
        tender.setErrorMessage(null);
        tender = tenderRepository.save(tender);

        if (tender.getVendorSubmissions() != null) {
            for (VendorSubmission submission : tender.getVendorSubmissions()) {
                submission.setStatus(SubmissionStatus.PROCESSING);
                submission.setProcessingStage("QUEUED");
                submission.setErrorMessage(null);
            }
            vendorSubmissionRepository.saveAll(tender.getVendorSubmissions());
        }

        return getProcessingStatus(tenderId, userEmail);
    }

    @Async
    public void processTenderAsync(UUID tenderId, String userEmail) {
        try {
            processTenderSync(tenderId, userEmail);
        } catch (Exception e) {
            log.error("Asynchronous processing failed for tenderId: {}", tenderId, e);
            try {
                getSelf().markTenderFailed(tenderId, userEmail, e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    /**
     * Orchestrator — NOT @Transactional.
     * Each step below runs in its own transaction so the DB commits after
     * each document/stage, allowing the frontend to see incremental progress.
     */
    public void processTenderSync(UUID tenderId, String userEmail) {
        // Step 1: Mark tender as PROCESSING / OCR_REQUIREMENT
        getSelf().markTenderProcessing(tenderId, userEmail);

        // Step 2: Process requirement PDF
        log.info("Step [1/N]: Processing requirement document for tender: {}", tenderId);
        List<TenderProduct> savedTenderProducts;
        try {
            getSelf().updateTenderStage(tenderId, userEmail, "OCR_REQUIREMENT");
            savedTenderProducts = getSelf().processRequirementDocument(tenderId, userEmail);
            getSelf().updateTenderStage(tenderId, userEmail, "REQUIREMENT_DONE");
        } catch (Exception e) {
            log.error("Requirement document processing failed for tender: {}", tenderId, e);
            getSelf().markTenderFailed(tenderId, userEmail, "Requirement parsing failed: " + e.getMessage());
            return;
        }

        // Build the product map for vendor matching
        List<Map<String, Object>> reqProductsForMatching = new ArrayList<>();
        for (TenderProduct tp : savedTenderProducts) {
            Map<String, Object> m = new HashMap<>();
            m.put("product_name", tp.getProductName());
            if (tp.getId() != null) {
                m.put("id", tp.getId().toString());
            }
            reqProductsForMatching.add(m);
        }

        // Step 3: Process each vendor submission one-by-one with granular stages
        List<UUID> submissionIds = getSelf().getVendorSubmissionIds(tenderId, userEmail);
        for (int i = 0; i < submissionIds.size(); i++) {
            UUID subId = submissionIds.get(i);
            log.info("Step [{}/{}]: Processing vendor submission {} for tender: {}",
                    i + 2, submissionIds.size() + 1, subId, tenderId);
            try {
                getSelf().updateVendorStage(subId, "OCR_SCANNING");
                getSelf().processVendorDocument(tenderId, subId, savedTenderProducts, reqProductsForMatching, userEmail);
            } catch (Exception ve) {
                log.error("Vendor document processing failed for submission: {}", subId, ve);
                getSelf().markVendorFailed(subId, "Vendor extraction failed: " + ve.getMessage());
            }
        }

        // Step 4: Mark tender as EXTRACTED
        getSelf().markTenderExtracted(tenderId, userEmail);
        log.info("Processing successfully completed for tender: {}", tenderId);
    }

    @Transactional
    public void markTenderProcessing(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        tender.setStatus(TenderStatus.PROCESSING);
        tender.setProcessingStage("OCR_REQUIREMENT");
        tender.setErrorMessage(null);
        tenderRepository.save(tender);

        // Set all vendor submissions to PROCESSING / QUEUED
        if (tender.getVendorSubmissions() != null) {
            for (VendorSubmission sub : tender.getVendorSubmissions()) {
                if (sub.getStatus() != SubmissionStatus.EXTRACTED && sub.getStatus() != SubmissionStatus.MATCHED) {
                    sub.setStatus(SubmissionStatus.PROCESSING);
                    sub.setProcessingStage("QUEUED");
                    sub.setErrorMessage(null);
                }
            }
            vendorSubmissionRepository.saveAll(tender.getVendorSubmissions());
        }
    }

    @Transactional
    public void updateTenderStage(UUID tenderId, String userEmail, String stage) {
        User user = getUser(userEmail);
        Tender tender = tenderRepository.findByIdAndUserAndDeletedAtIsNull(tenderId, user).orElse(null);
        if (tender != null) {
            tender.setProcessingStage(stage);
            tenderRepository.save(tender);
        }
    }

    @Transactional
    public void updateVendorStage(UUID submissionId, String stage) {
        VendorSubmission sub = vendorSubmissionRepository.findById(submissionId).orElse(null);
        if (sub != null) {
            sub.setProcessingStage(stage);
            sub.setStatus(SubmissionStatus.PROCESSING);
            vendorSubmissionRepository.save(sub);
        }
    }

    @Transactional
    public List<TenderProduct> processRequirementDocument(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = tenderRepository.findByIdWithSubmissions(tenderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id: " + tenderId));

        tender.setProcessingStage("SPEC_EXTRACTION");
        tenderRepository.save(tender);

        String reqBase64 = readFileAsBase64(tender.getRequirementFileKey());

        ProcessDocumentAiRequest reqRequest = new ProcessDocumentAiRequest(
                tender.getRequirementFileKey(),
                "requirement",
                tenderId.toString(),
                null,
                reqBase64,
                null
        );

        ProcessDocumentAiResponse reqResponse = aiServiceClient.processDocument(reqRequest);

        // Clean old requirement products if re-processing
        if (matchResultRepository != null) {
            matchResultRepository.deleteByTender(tender);
        }
        tenderProductRepository.deleteByTender(tender);

        List<TenderProduct> savedTenderProducts = new ArrayList<>();

        if (reqResponse.getProducts() != null) {
            int prodOrder = 0;
            for (ExtractedProductDto prodDto : reqResponse.getProducts()) {
                TenderProduct product = new TenderProduct();
                product.setTender(tender);
                product.setProductName(prodDto.getProductName());
                product.setProductCategory(prodDto.getProductCategory() != null ? prodDto.getProductCategory() : "electronics");
                product.setDisplayOrder(prodOrder++);
                product.setRawText(prodDto.getRawText());

                TenderProduct saved = tenderProductRepository.save(product);
                if (saved != null) {
                    product = saved;
                }
                savedTenderProducts.add(product);

                int specOrder = 0;
                List<TenderSpec> savedSpecs = new ArrayList<>();
                if (prodDto.getSpecifications() != null) {
                    for (ExtractedSpecDto specDto : prodDto.getSpecifications()) {
                        TenderSpec spec = new TenderSpec();
                        spec.setTenderProduct(product);
                        spec.setRawSpecName(specDto.getRawSpecName());
                        spec.setCanonicalSpecName(specDto.getCanonicalSpecName());
                        spec.setSpecValue(specDto.getSpecValue());
                        spec.setNumericValue(specDto.getNumericValue());
                        spec.setMinValue(specDto.getMinValue());
                        spec.setMaxValue(specDto.getMaxValue());
                        spec.setUnit(specDto.getUnit());
                        spec.setIsNumeric(specDto.getIsNumeric() != null ? specDto.getIsNumeric() : false);
                        spec.setRawText(specDto.getRawText());
                        spec.setExtractionConfidence(specDto.getExtractionConfidence() != null ? specDto.getExtractionConfidence() : "high");
                        spec.setExtractionSource(specDto.getExtractionSource() != null ? specDto.getExtractionSource() : "llm");
                        spec.setNormalizationConfidence(specDto.getNormalizationConfidence() != null ? specDto.getNormalizationConfidence() : "high");
                        spec.setDisplayOrder(specOrder++);

                        TenderSpec savedSpec = tenderSpecRepository.save(spec);
                        savedSpecs.add(savedSpec);
                    }
                }
                product.setSpecifications(savedSpecs);
            }
        }

        return savedTenderProducts;
    }

    @Transactional
    public void processVendorDocument(UUID tenderId, UUID submissionId,
                                      List<TenderProduct> savedTenderProducts,
                                      List<Map<String, Object>> reqProductsForMatching,
                                      String userEmail) {
        VendorSubmission submission = vendorSubmissionRepository.findById(submissionId)
                .or(() -> {
                    try {
                        User user = getUser(userEmail);
                        Tender tender = getTender(tenderId, user);
                        if (tender.getVendorSubmissions() != null) {
                            return tender.getVendorSubmissions().stream()
                                    .filter(s -> submissionId.equals(s.getId()))
                                    .findFirst();
                        }
                    } catch (Exception ignored) {}
                    return Optional.empty();
                })
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        submission.setStatus(SubmissionStatus.PROCESSING);
        submission.setProcessingStage("SPEC_EXTRACTION");
        submission.setErrorMessage(null);
        vendorSubmissionRepository.save(submission);

        String vendorBase64 = readFileAsBase64(submission.getFileKey());
        ProcessDocumentAiRequest vendorRequest = new ProcessDocumentAiRequest(
                submission.getFileKey(),
                "vendor",
                tenderId.toString(),
                submission.getId().toString(),
                vendorBase64,
                reqProductsForMatching
        );

        ProcessDocumentAiResponse vendorResponse = aiServiceClient.processDocument(vendorRequest);

        // Clean old vendor products for this submission
        vendorProductRepository.deleteByVendorSubmission(submission);

        if (vendorResponse.getProducts() != null) {
            int vProdOrder = 0;
            for (ExtractedProductDto vProdDto : vendorResponse.getProducts()) {
                VendorProduct vProd = new VendorProduct();
                vProd.setVendorSubmission(submission);
                vProd.setProductName(vProdDto.getProductName());
                vProd.setProductCategory(vProdDto.getProductCategory() != null ? vProdDto.getProductCategory() : "electronics");
                vProd.setMatchConfidence(vProdDto.getMatchConfidence());
                vProd.setDisplayOrder(vProdOrder++);
                vProd.setRawText(vProdDto.getRawText());

                if (vProdDto.getMatchedTenderProductId() != null) {
                    try {
                        UUID matchedId = UUID.fromString(vProdDto.getMatchedTenderProductId());
                        savedTenderProducts.stream()
                                .filter(tp -> tp.getId().equals(matchedId))
                                .findFirst()
                                .ifPresent(vProd::setMatchedTenderProduct);
                    } catch (IllegalArgumentException ignored) {}
                }

                VendorProduct savedVProd = vendorProductRepository.save(vProd);
                if (savedVProd != null) {
                    vProd = savedVProd;
                }

                int vSpecOrder = 0;
                List<VendorSpec> savedVSpecs = new ArrayList<>();
                if (vProdDto.getSpecifications() != null) {
                    for (ExtractedSpecDto vSpecDto : vProdDto.getSpecifications()) {
                        VendorSpec vSpec = new VendorSpec();
                        vSpec.setVendorProduct(vProd);
                        vSpec.setRawSpecName(vSpecDto.getRawSpecName());
                        vSpec.setCanonicalSpecName(vSpecDto.getCanonicalSpecName());
                        vSpec.setSpecValue(vSpecDto.getSpecValue());
                        vSpec.setNumericValue(vSpecDto.getNumericValue());
                        vSpec.setMinValue(vSpecDto.getMinValue());
                        vSpec.setMaxValue(vSpecDto.getMaxValue());
                        vSpec.setUnit(vSpecDto.getUnit());
                        vSpec.setIsNumeric(vSpecDto.getIsNumeric() != null ? vSpecDto.getIsNumeric() : false);
                        vSpec.setRawText(vSpecDto.getRawText());
                        vSpec.setExtractionConfidence(vSpecDto.getExtractionConfidence() != null ? vSpecDto.getExtractionConfidence() : "high");
                        vSpec.setExtractionSource(vSpecDto.getExtractionSource() != null ? vSpecDto.getExtractionSource() : "llm");
                        vSpec.setNormalizationConfidence(vSpecDto.getNormalizationConfidence() != null ? vSpecDto.getNormalizationConfidence() : "high");
                        vSpec.setDisplayOrder(vSpecOrder++);

                        VendorSpec savedVSpec = vendorSpecRepository.save(vSpec);
                        savedVSpecs.add(savedVSpec);
                    }
                }
                vProd.setSpecifications(savedVSpecs);
            }
        }

        submission.setStatus(SubmissionStatus.EXTRACTED);
        submission.setProcessingStage("EXTRACTED");
        submission.setErrorMessage(null);
        vendorSubmissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public List<UUID> getVendorSubmissionIds(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        if (tender.getVendorSubmissions() == null) return new ArrayList<>();
        return tender.getVendorSubmissions().stream()
                .map(VendorSubmission::getId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markTenderExtracted(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        tender.setStatus(TenderStatus.EXTRACTED);
        tender.setProcessingStage("ALL_EXTRACTED");
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

    @Transactional
    public void markVendorFailed(UUID submissionId, String errorMessage) {
        try {
            VendorSubmission sub = vendorSubmissionRepository.findById(submissionId).orElse(null);
            if (sub != null) {
                sub.setStatus(SubmissionStatus.FAILED);
                sub.setProcessingStage("FAILED");
                sub.setErrorMessage(errorMessage);
                vendorSubmissionRepository.save(sub);
            }
        } catch (Exception e) {
            log.error("Could not mark vendor submission {} as FAILED: {}", submissionId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ProcessingStatusResponse getProcessingStatus(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);

        int totalDocs = 1; // Requirement PDF
        int completedDocs = 0;

        // Detect requirement completion by checking if TenderProducts exist
        // This works even while the tender is still PROCESSING (mid-pipeline)
        boolean requirementHasProducts = !tenderProductRepository.findByTenderOrderByDisplayOrderAsc(tender).isEmpty();

        String reqStatus;
        if (tender.getStatus() == TenderStatus.FAILED) {
            reqStatus = "FAILED";
        } else if (requirementHasProducts) {
            reqStatus = "DONE";
            completedDocs++;
        } else if (tender.getStatus() == TenderStatus.PROCESSING) {
            reqStatus = "PROCESSING";
        } else if (tender.getStatus() == TenderStatus.EXTRACTED || tender.getStatus() == TenderStatus.COMPLETED
                || tender.getStatus() == TenderStatus.MATCHING || tender.getStatus() == TenderStatus.REVIEW
                || tender.getStatus() == TenderStatus.SCORED) {
            reqStatus = "DONE";
            completedDocs++;
        } else {
            reqStatus = "PENDING";
        }

        List<ProcessingStatusResponse.VendorDocumentStatus> vendorStatuses = new ArrayList<>();
        if (tender.getVendorSubmissions() != null) {
            totalDocs += tender.getVendorSubmissions().size();
            for (VendorSubmission sub : tender.getVendorSubmissions()) {
                if (sub.getStatus() == SubmissionStatus.EXTRACTED || sub.getStatus() == SubmissionStatus.MATCHED) {
                    completedDocs++;
                }
                int count = vendorProductRepository.findByVendorSubmissionOrderByDisplayOrderAsc(sub).size();
                vendorStatuses.add(new ProcessingStatusResponse.VendorDocumentStatus(
                        sub.getId(),
                        sub.getVendorName(),
                        sub.getFileName(),
                        sub.getStatus(),
                        sub.getProcessingStage(),
                        sub.getErrorMessage(),
                        count
                ));
            }
        }

        return new ProcessingStatusResponse(
                tender.getId(),
                tender.getStatus(),
                tender.getProcessingStage(),
                tender.getErrorMessage(),
                tender.getRequirementFileName(),
                tender.getRequirementFileKey(),
                reqStatus,
                totalDocs,
                completedDocs,
                vendorStatuses
        );
    }

    @Transactional(readOnly = true)
    public List<ExtractedProductDto> getTenderProducts(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        List<TenderProduct> products = tenderProductRepository.findByTenderWithSpecs(tender);
        return products.stream()
                .map(ExtractedProductDto::fromTenderProduct)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExtractedProductDto> getAllVendorProducts(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        List<VendorProduct> products = vendorProductRepository.findByTenderWithSpecs(tender);
        return products.stream()
                .map(ExtractedProductDto::fromVendorProduct)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExtractedProductDto> getVendorSubmissionProducts(UUID tenderId, UUID submissionId, String userEmail) {
        User user = getUser(userEmail);
        getTender(tenderId, user);
        VendorSubmission submission = vendorSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));
        List<VendorProduct> products = vendorProductRepository.findByVendorSubmissionWithSpecs(submission);
        return products.stream()
                .map(ExtractedProductDto::fromVendorProduct)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExtractedSpecDto> getTenderProductSpecs(UUID tenderId, UUID productId, String userEmail) {
        User user = getUser(userEmail);
        getTender(tenderId, user);
        TenderProduct product = tenderProductRepository.findByIdWithSpecs(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        return product.getSpecifications().stream()
                .map(ExtractedSpecDto::fromTenderSpec)
                .collect(Collectors.toList());
    }

    private String readFileAsBase64(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            return "";
        }
        try (InputStream in = storageService.getFile(fileKey)) {
            if (in == null) return "";
            byte[] bytes = in.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("Could not read file from storage for key {}: {}", fileKey, e.getMessage());
            return "";
        }
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
