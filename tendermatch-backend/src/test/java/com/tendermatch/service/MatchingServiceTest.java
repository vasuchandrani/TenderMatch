package com.tendermatch.service;

import com.tendermatch.dto.matching.MatchResultResponse;
import com.tendermatch.dto.matching.MatchSpecsAiResponse;
import com.tendermatch.dto.matching.VendorMatchSummaryDto;
import com.tendermatch.model.*;
import com.tendermatch.model.enums.SubmissionStatus;
import com.tendermatch.model.enums.TenderStatus;
import com.tendermatch.repository.*;
import com.tendermatch.service.ai.AiServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private VendorSubmissionRepository vendorSubmissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenderProductRepository tenderProductRepository;

    @Mock
    private VendorProductRepository vendorProductRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private ProcessingService processingService;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private MatchingService matchingService;


    private User testUser;
    private Tender testTender;
    private VendorSubmission vendorSubmission;
    private TenderProduct tenderProduct;
    private TenderSpec reqSpec1;
    private TenderSpec reqSpec2;
    private VendorProduct vendorProduct;
    private VendorSpec venSpec1;
    private UUID tenderId;
    private UUID submissionId;

    @BeforeEach
    void setUp() {
        tenderId = UUID.randomUUID();
        submissionId = UUID.randomUUID();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("procure@tendermatch.com")
                .passwordHash("secret")
                .name("Procurement Officer")
                .role("ADMIN")
                .build();

        vendorSubmission = VendorSubmission.builder()
                .id(submissionId)
                .vendorName("Lenovo Global")
                .fileName("lenovo_thinkpad.pdf")
                .status(SubmissionStatus.EXTRACTED)
                .build();

        testTender = Tender.builder()
                .id(tenderId)
                .user(testUser)
                .name("Corporate Laptops 2026")
                .status(TenderStatus.EXTRACTED)
                .vendorSubmissions(new ArrayList<>(List.of(vendorSubmission)))
                .build();
        vendorSubmission.setTender(testTender);

        tenderProduct = new TenderProduct();
        tenderProduct.setId(UUID.randomUUID());
        tenderProduct.setProductName("Business Laptop");
        tenderProduct.setTender(testTender);

        reqSpec1 = new TenderSpec();
        reqSpec1.setId(UUID.randomUUID());
        reqSpec1.setTenderProduct(tenderProduct);
        reqSpec1.setRawSpecName("RAM");
        reqSpec1.setCanonicalSpecName("ram_capacity");
        reqSpec1.setNumericValue(16.0);
        reqSpec1.setUnit("GB");
        reqSpec1.setIsNumeric(true);

        reqSpec2 = new TenderSpec();
        reqSpec2.setId(UUID.randomUUID());
        reqSpec2.setTenderProduct(tenderProduct);
        reqSpec2.setRawSpecName("Security");
        reqSpec2.setCanonicalSpecName("features");
        reqSpec2.setSpecValue("TPM 2.0 Chip");
        reqSpec2.setIsNumeric(false);

        tenderProduct.setSpecifications(List.of(reqSpec1, reqSpec2));

        vendorProduct = new VendorProduct();
        vendorProduct.setId(UUID.randomUUID());
        vendorProduct.setProductName("ThinkPad T14");
        vendorProduct.setVendorSubmission(vendorSubmission);
        vendorProduct.setMatchedTenderProduct(tenderProduct);

        venSpec1 = new VendorSpec();
        venSpec1.setId(UUID.randomUUID());
        venSpec1.setVendorProduct(vendorProduct);
        venSpec1.setRawSpecName("Memory");
        venSpec1.setCanonicalSpecName("ram_capacity");
        venSpec1.setNumericValue(32.0);
        venSpec1.setUnit("GB");
        venSpec1.setIsNumeric(true);
        venSpec1.setMatchedTenderSpec(reqSpec1);

        // Note: vendorProduct has no match for reqSpec2 (missing spec case)
        vendorProduct.setSpecifications(List.of(venSpec1));

        lenient().when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(i -> {
            MatchResult mr = i.getArgument(0);
            if (mr.getId() == null) mr.setId(UUID.randomUUID());
            return mr;
        });
    }

    @Test
    @DisplayName("1. shouldTriggerMatchingForAllVendors - triggers AI batch match for each vendor")
    void shouldTriggerMatchingForAllVendors() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(tenderProductRepository.findByTenderWithSpecs(testTender)).thenReturn(List.of(tenderProduct));
        when(vendorProductRepository.findByVendorSubmissionWithSpecs(vendorSubmission)).thenReturn(List.of(vendorProduct));

        MatchSpecsAiResponse aiResponse = new MatchSpecsAiResponse(
                tenderId.toString(), submissionId.toString(),
                List.of(new MatchSpecsAiResponse.MatchResultItemAiDto(
                        reqSpec1.getId().toString(), ">=", "numeric_exact", "Match",
                        0.95, "32GB >= 16GB", null, null, null, null, null
                ))
        );
        when(aiServiceClient.matchSpecs(any())).thenReturn(aiResponse);

        matchingService.matchTenderSync(tenderId, testUser.getEmail());

        verify(aiServiceClient).matchSpecs(any());
        verify(matchResultRepository, atLeastOnce()).save(any());
        assertThat(testTender.getStatus()).isEqualTo(TenderStatus.COMPLETED);
    }

    @Test
    @DisplayName("2. shouldStoreMatchResults - saves AI evaluation verdicts into database")
    void shouldStoreMatchResults() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(tenderProductRepository.findByTenderWithSpecs(testTender)).thenReturn(List.of(tenderProduct));
        when(vendorProductRepository.findByVendorSubmissionWithSpecs(vendorSubmission)).thenReturn(List.of(vendorProduct));

        MatchSpecsAiResponse aiResponse = new MatchSpecsAiResponse(
                tenderId.toString(), submissionId.toString(),
                List.of(new MatchSpecsAiResponse.MatchResultItemAiDto(
                        reqSpec1.getId().toString(), ">=", "numeric_exact", "Match",
                        0.95, "32GB >= 16GB", null, null, null, null, null
                ))
        );
        when(aiServiceClient.matchSpecs(any())).thenReturn(aiResponse);

        matchingService.matchTenderSync(tenderId, testUser.getEmail());

        ArgumentCaptor<MatchResult> captor = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository, atLeastOnce()).save(captor.capture());

        List<MatchResult> saved = captor.getAllValues();
        assertThat(saved).anyMatch(r -> "Match".equals(r.getSystemVerdict()) && reqSpec1.equals(r.getTenderSpec()));
    }

    @Test
    @DisplayName("3. shouldHandleMissingVendorSpec - assigns Unmatch with method missing when vendor spec omitted")
    void shouldHandleMissingVendorSpec() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(tenderProductRepository.findByTenderWithSpecs(testTender)).thenReturn(List.of(tenderProduct));
        when(vendorProductRepository.findByVendorSubmissionWithSpecs(vendorSubmission)).thenReturn(List.of(vendorProduct));

        // reqSpec1 is matched, reqSpec2 is omitted by vendor
        MatchSpecsAiResponse aiResponse = new MatchSpecsAiResponse(
                tenderId.toString(), submissionId.toString(),
                List.of(
                        new MatchSpecsAiResponse.MatchResultItemAiDto(
                                reqSpec1.getId().toString(), ">=", "numeric_exact", "Match",
                                0.95, "32GB >= 16GB", null, null, null, null, null
                        ),
                        new MatchSpecsAiResponse.MatchResultItemAiDto(
                                reqSpec2.getId().toString(), "none", "missing", "Unmatch",
                                1.0, "Requirement omitted / not offered by vendor", null, null, null, null, null
                        )
                )
        );
        when(aiServiceClient.matchSpecs(any())).thenReturn(aiResponse);

        matchingService.matchTenderSync(tenderId, testUser.getEmail());

        ArgumentCaptor<MatchResult> captor = ArgumentCaptor.forClass(MatchResult.class);
        verify(matchResultRepository, atLeast(2)).save(captor.capture());

        List<MatchResult> saved = captor.getAllValues();
        assertThat(saved).anyMatch(r -> "Unmatch".equals(r.getSystemVerdict()) && "missing".equals(r.getComparisonMethod()));
    }

    @Test
    @DisplayName("4. shouldUpdateTenderStatusToMatching - transitions status to MATCHING on trigger")
    void shouldUpdateTenderStatusToMatching() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(i -> i.getArgument(0));

        matchingService.triggerMatching(tenderId, testUser.getEmail());

        assertThat(testTender.getStatus()).isEqualTo(TenderStatus.MATCHING);
        assertThat(vendorSubmission.getStatus()).isEqualTo(SubmissionStatus.PROCESSING);
    }

    @Test
    @DisplayName("5. shouldComputeSummaryPerVendor - calculates Match, Unmatch, and AI Err counts")
    void shouldComputeSummaryPerVendor() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(matchResultRepository.countByVendorSubmissionAndSystemVerdict(vendorSubmission, "Match")).thenReturn(8L);
        when(matchResultRepository.countByVendorSubmissionAndSystemVerdict(vendorSubmission, "Unmatch")).thenReturn(2L);
        when(matchResultRepository.countByVendorSubmissionAndSystemVerdict(vendorSubmission, "AI Err")).thenReturn(1L);

        List<VendorMatchSummaryDto> summary = matchingService.getMatchSummary(tenderId, testUser.getEmail());

        assertThat(summary).hasSize(1);
        VendorMatchSummaryDto s = summary.get(0);
        assertThat(s.getMatchCount()).isEqualTo(8L);
        assertThat(s.getUnmatchCount()).isEqualTo(2L);
        assertThat(s.getAiErrCount()).isEqualTo(1L);
        assertThat(s.getTotalSpecs()).isEqualTo(11L);
        assertThat(s.getMatchPercentage()).isEqualTo(72.7);
    }

    @Test
    @DisplayName("6. shouldHandleAiServiceMatchFailure - marks tender as FAILED on AI error")
    void shouldHandleAiServiceMatchFailure() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(tenderProductRepository.findByTenderWithSpecs(testTender)).thenReturn(List.of(tenderProduct));
        when(vendorProductRepository.findByVendorSubmissionWithSpecs(vendorSubmission)).thenReturn(List.of(vendorProduct));
        when(aiServiceClient.matchSpecs(any())).thenThrow(new RuntimeException("AI match endpoint timeout"));

        matchingService.matchTenderSync(tenderId, testUser.getEmail());

        assertThat(testTender.getStatus()).isEqualTo(TenderStatus.FAILED);
    }
}
