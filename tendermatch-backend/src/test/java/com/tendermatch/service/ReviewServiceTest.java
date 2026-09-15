package com.tendermatch.service;

import com.tendermatch.dto.review.*;
import com.tendermatch.model.*;
import com.tendermatch.model.enums.SubmissionStatus;
import com.tendermatch.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private VendorSubmissionRepository vendorSubmissionRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private CorrectionLogRepository correctionLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.tendermatch.service.ai.AiServiceClient aiServiceClient;

    @Mock
    private ScoringService scoringService;

    @Mock
    private ScoringWeightRepository scoringWeightRepository;

    @InjectMocks
    private ReviewService reviewService;


    private User testUser;
    private Tender testTender;
    private VendorSubmission testVendor;
    private TenderProduct testProduct;
    private TenderSpec testTenderSpec;
    private VendorSpec testVendorSpec;
    private MatchResult testResult;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("reviewer@example.com")
                .name("Reviewer Bob")
                .role("ADMIN")
                .build();

        testTender = Tender.builder()
                .id(UUID.randomUUID())
                .name("Procurement Tender")
                .user(testUser)
                .build();

        testVendor = VendorSubmission.builder()
                .id(UUID.randomUUID())
                .tender(testTender)
                .vendorName("Acme Tech")
                .fileName("acme_proposal.pdf")
                .status(SubmissionStatus.EXTRACTED)
                .build();

        testProduct = new TenderProduct();
        testProduct.setId(UUID.randomUUID());
        testProduct.setTender(testTender);
        testProduct.setProductName("High-End Laptop");

        testTenderSpec = new TenderSpec();
        testTenderSpec.setId(UUID.randomUUID());
        testTenderSpec.setTenderProduct(testProduct);
        testTenderSpec.setRawSpecName("RAM");
        testTenderSpec.setCanonicalSpecName("ram_capacity");
        testTenderSpec.setSpecValue("16");
        testTenderSpec.setUnit("GB");
        testTenderSpec.setRawText("Minimum 16 GB DDR5 RAM");

        VendorProduct vendorProduct = new VendorProduct();
        vendorProduct.setId(UUID.randomUUID());
        vendorProduct.setVendorSubmission(testVendor);
        vendorProduct.setProductName("ProBook");

        testVendorSpec = new VendorSpec();
        testVendorSpec.setId(UUID.randomUUID());
        testVendorSpec.setVendorProduct(vendorProduct);
        testVendorSpec.setRawSpecName("Memory");
        testVendorSpec.setCanonicalSpecName("ram_capacity");
        testVendorSpec.setSpecValue("32");
        testVendorSpec.setUnit("GB");
        testVendorSpec.setRawText("32 GB DDR5 RAM");

        testResult = MatchResult.builder()
                .id(UUID.randomUUID())
                .tender(testTender)
                .vendorSubmission(testVendor)
                .tenderProduct(testProduct)
                .vendorProduct(vendorProduct)
                .tenderSpec(testTenderSpec)
                .vendorSpec(testVendorSpec)
                .systemVerdict("Match")
                .confidenceScore(0.95)
                .comparisonMethod("numeric_exact")
                .comparisonOperator(">=")
                .details("32 >= 16 GB")
                .build();
    }

    @Test
    @DisplayName("Should return review data grouped by vendor, product, and spec pairs")
    void shouldReturnReviewDataGroupedByVendorProductSpec() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(testTender.getId(), testUser))
                .thenReturn(Optional.of(testTender));
        when(vendorSubmissionRepository.findByIdAndTender(testVendor.getId(), testTender))
                .thenReturn(Optional.of(testVendor));
        when(matchResultRepository.findByTenderAndVendorSubmission(testTender, testVendor))
                .thenReturn(List.of(testResult));

        ReviewVendorDto reviewData = reviewService.getVendorReviewData(testTender.getId(), testVendor.getId(), testUser.getEmail());

        assertThat(reviewData).isNotNull();
        assertThat(reviewData.vendorId()).isEqualTo(testVendor.getId());
        assertThat(reviewData.vendorName()).isEqualTo("Acme Tech");
        assertThat(reviewData.products()).hasSize(1);

        ReviewProductDto productDto = reviewData.products().get(0);
        assertThat(productDto.tenderProductName()).isEqualTo("High-End Laptop");
        assertThat(productDto.specPairs()).hasSize(1);

        ReviewSpecPairDto specPair = productDto.specPairs().get(0);
        assertThat(specPair.resultId()).isEqualTo(testResult.getId());
        assertThat(specPair.requiredSpecName()).isEqualTo("RAM");
        assertThat(specPair.offeredSpecName()).isEqualTo("Memory");
        assertThat(specPair.systemVerdict()).isEqualTo("Match");
        assertThat(specPair.isReviewed()).isFalse();
    }

    @Test
    @DisplayName("Should submit human verdict and detect override with correction log")
    void shouldSubmitHumanVerdictAndDetectOverride() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(testTender.getId(), testUser))
                .thenReturn(Optional.of(testTender));
        when(matchResultRepository.findById(testResult.getId())).thenReturn(Optional.of(testResult));
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(inv -> inv.getArgument(0));
        when(matchResultRepository.findByTenderAndVendorSubmission(testTender, testVendor))
                .thenReturn(List.of(testResult));

        SubmitVerdictRequest request = new SubmitVerdictRequest("Unmatch", "Vendor RAM does not meet frequency requirement");

        SubmitVerdictResponse response = reviewService.submitVerdict(
                testTender.getId(), testResult.getId(), request, testUser.getEmail());

        assertThat(response).isNotNull();
        assertThat(response.systemVerdict()).isEqualTo("Match");
        assertThat(response.humanVerdict()).isEqualTo("Unmatch");
        assertThat(response.isOverride()).isTrue();
        assertThat(response.userComment()).isEqualTo("Vendor RAM does not meet frequency requirement");

        // Verify MatchResult was updated
        assertThat(testResult.getHumanVerdict()).isEqualTo("Unmatch");
        assertThat(testResult.getReviewedBy()).isEqualTo(testUser);
        assertThat(testResult.getReviewedAt()).isNotNull();

        // Verify CorrectionLog was saved
        ArgumentCaptor<CorrectionLog> logCaptor = ArgumentCaptor.forClass(CorrectionLog.class);
        verify(correctionLogRepository).save(logCaptor.capture());
        CorrectionLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getSystemVerdict()).isEqualTo("Match");
        assertThat(savedLog.getHumanVerdict()).isEqualTo("Unmatch");
        assertThat(savedLog.isOverride()).isTrue();
        assertThat(savedLog.isAutoConfirmed()).isFalse();
        assertThat(savedLog.getUserComment()).contains("frequency requirement");
        assertThat(savedLog.getContext()).contains("ram_capacity");
    }

    @Test
    @DisplayName("Should reject verdict submission if vendor review is locked")
    void shouldRejectVerdictOnFinalizedLockedReview() {
        testVendor.setReviewLocked(true);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(testTender.getId(), testUser))
                .thenReturn(Optional.of(testTender));
        when(matchResultRepository.findById(testResult.getId())).thenReturn(Optional.of(testResult));

        SubmitVerdictRequest request = new SubmitVerdictRequest("Match", null);

        assertThatThrownBy(() -> reviewService.submitVerdict(testTender.getId(), testResult.getId(), request, testUser.getEmail()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Review is finalized and locked");

        verify(matchResultRepository, never()).save(any());
        verify(correctionLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate review progress accurately")
    void shouldCalculateReviewProgressAccurately() {
        MatchResult result1 = MatchResult.builder()
                .id(UUID.randomUUID())
                .tender(testTender)
                .vendorSubmission(testVendor)
                .systemVerdict("Match")
                .humanVerdict("Match")
                .confidenceScore(0.9)
                .build();

        MatchResult result2 = MatchResult.builder()
                .id(UUID.randomUUID())
                .tender(testTender)
                .vendorSubmission(testVendor)
                .systemVerdict("Match")
                .humanVerdict("Unmatch")
                .confidenceScore(0.8)
                .build();

        MatchResult result3 = MatchResult.builder()
                .id(UUID.randomUUID())
                .tender(testTender)
                .vendorSubmission(testVendor)
                .systemVerdict("AI Err")
                .humanVerdict(null)
                .confidenceScore(0.4)
                .build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(testTender.getId(), testUser))
                .thenReturn(Optional.of(testTender));
        when(vendorSubmissionRepository.findByIdAndTender(testVendor.getId(), testTender))
                .thenReturn(Optional.of(testVendor));
        when(matchResultRepository.findByTenderAndVendorSubmission(testTender, testVendor))
                .thenReturn(List.of(result1, result2, result3));

        ReviewProgressResponse progress = reviewService.getVendorReviewProgress(testTender.getId(), testVendor.getId(), testUser.getEmail());

        assertThat(progress.reviewedCount()).isEqualTo(2);
        assertThat(progress.totalCount()).isEqualTo(3);
        assertThat(progress.percentage()).isEqualTo(66.7);
        assertThat(progress.overrideCount()).isEqualTo(1);
        assertThat(progress.isLocked()).isFalse();
    }

    @Test
    @DisplayName("Should bulk confirm remaining unreviewed specs with auto_confirmed flag")
    void shouldBulkConfirmRemainingSpecsWithAutoConfirmedFlag() {
        MatchResult unreviewedResult = MatchResult.builder()
                .id(UUID.randomUUID())
                .tender(testTender)
                .vendorSubmission(testVendor)
                .tenderProduct(testProduct)
                .tenderSpec(testTenderSpec)
                .vendorSpec(testVendorSpec)
                .systemVerdict("Match")
                .humanVerdict(null)
                .confidenceScore(0.85)
                .build();

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(testTender.getId(), testUser))
                .thenReturn(Optional.of(testTender));
        when(vendorSubmissionRepository.findByIdAndTender(testVendor.getId(), testTender))
                .thenReturn(Optional.of(testVendor));
        when(matchResultRepository.findByTenderAndVendorSubmission(testTender, testVendor))
                .thenReturn(List.of(unreviewedResult));

        ReviewProgressResponse progress = reviewService.bulkConfirmRemaining(
                testTender.getId(), testVendor.getId(), testUser.getEmail());

        assertThat(progress.reviewedCount()).isEqualTo(1);
        assertThat(unreviewedResult.getHumanVerdict()).isEqualTo("Match");
        assertThat(unreviewedResult.getReviewedBy()).isEqualTo(testUser);

        ArgumentCaptor<CorrectionLog> logCaptor = ArgumentCaptor.forClass(CorrectionLog.class);
        verify(correctionLogRepository).save(logCaptor.capture());
        CorrectionLog log = logCaptor.getValue();
        assertThat(log.isAutoConfirmed()).isTrue();
        assertThat(log.isOverride()).isFalse();
        assertThat(log.getHumanVerdict()).isEqualTo("Match");
    }

    @Test
    @DisplayName("Should finalize and lock vendor review session")
    void shouldFinalizeAndLockVendorReview() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(testTender.getId(), testUser))
                .thenReturn(Optional.of(testTender));
        when(vendorSubmissionRepository.findByIdAndTender(testVendor.getId(), testTender))
                .thenReturn(Optional.of(testVendor));
        when(vendorSubmissionRepository.save(any(VendorSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewSummaryResponse summary = reviewService.finalizeReview(
                testTender.getId(), testVendor.getId(), testUser.getEmail());

        assertThat(summary.isLocked()).isTrue();
        assertThat(testVendor.isReviewLocked()).isTrue();
        assertThat(testVendor.getReviewFinalizedAt()).isNotNull();
        assertThat(testVendor.getReviewFinalizedBy()).isEqualTo(testUser);
    }
}
