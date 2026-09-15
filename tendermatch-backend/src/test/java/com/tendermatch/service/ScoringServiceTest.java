package com.tendermatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tendermatch.dto.scoring.ComparisonMatrixResponse;
import com.tendermatch.dto.scoring.TenderScoresResponse;
import com.tendermatch.dto.scoring.VendorScoreResponse;
import com.tendermatch.model.*;
import com.tendermatch.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private VendorSubmissionRepository vendorSubmissionRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private ScoringWeightRepository scoringWeightRepository;

    @Mock
    private VendorScoreRepository vendorScoreRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ScoringService scoringService;

    private UUID tenderId;
    private UUID vendorId1;
    private UUID vendorId2;
    private Tender tender;
    private VendorSubmission vendor1;
    private VendorSubmission vendor2;
    private TenderProduct tenderProduct1;
    private TenderProduct tenderProduct2;

    @BeforeEach
    void setUp() {
        tenderId = UUID.randomUUID();
        vendorId1 = UUID.randomUUID();
        vendorId2 = UUID.randomUUID();

        tender = Tender.builder()
                .id(tenderId)
                .name("Government IT Procurement 2026")
                .status(com.tendermatch.model.enums.TenderStatus.COMPLETED)
                .build();

        vendor1 = VendorSubmission.builder()
                .id(vendorId1)
                .tender(tender)
                .vendorName("Dell Technologies")
                .fileName("dell_quote.pdf")
                .build();
        vendor1.setReviewLocked(true);

        vendor2 = VendorSubmission.builder()
                .id(vendorId2)
                .tender(tender)
                .vendorName("HP Inc.")
                .fileName("hp_quote.pdf")
                .build();
        vendor2.setReviewLocked(true);

        tenderProduct1 = new TenderProduct();
        tenderProduct1.setId(UUID.randomUUID());
        tenderProduct1.setTender(tender);
        tenderProduct1.setProductName("Laptop");

        tenderProduct2 = new TenderProduct();
        tenderProduct2.setId(UUID.randomUUID());
        tenderProduct2.setTender(tender);
        tenderProduct2.setProductName("Monitor");
    }

    private MatchResult createResult(VendorSubmission vendor, TenderProduct product, String canonicalName,
                                     String systemVerdict, String humanVerdict, String reqVal, String offVal) {
        TenderSpec reqSpec = new TenderSpec();
        reqSpec.setId(UUID.randomUUID());
        reqSpec.setTenderProduct(product);
        reqSpec.setRawSpecName(canonicalName);
        reqSpec.setCanonicalSpecName(canonicalName);
        reqSpec.setSpecValue(reqVal);
        reqSpec.setUnit("GB");

        VendorSpec offSpec = null;
        if (offVal != null) {
            offSpec = new VendorSpec();
            offSpec.setId(UUID.randomUUID());
            offSpec.setRawSpecName(canonicalName);
            offSpec.setCanonicalSpecName(canonicalName);
            offSpec.setSpecValue(offVal);
            offSpec.setUnit("GB");
        }

        MatchResult res = MatchResult.builder()
                .id(UUID.randomUUID())
                .tender(tender)
                .vendorSubmission(vendor)
                .tenderProduct(product)
                .tenderSpec(reqSpec)
                .vendorSpec(offSpec)
                .comparisonOperator(">=")
                .systemVerdict(systemVerdict)
                .humanVerdict(humanVerdict)
                .confidenceScore(0.95)
                .build();
        res.setReviewedAt(ZonedDateTime.now());
        return res;
    }

    @Test
    void shouldCalculateEqualWeightScore() {
        // 14 Match, 2 Unmatch -> 14.0 / 16.0 * 100 = 87.5%
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(vendorSubmissionRepository.findByTender(tender)).thenReturn(List.of(vendor1));
        when(scoringWeightRepository.findByTenderId(tenderId)).thenReturn(Collections.emptyList());

        List<MatchResult> results = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            results.add(createResult(vendor1, tenderProduct1, "spec_" + i, "Match", "Match", "16", "32"));
        }
        for (int i = 14; i < 16; i++) {
            results.add(createResult(vendor1, tenderProduct1, "spec_" + i, "Unmatch", "Unmatch", "16", "8"));
        }

        when(matchResultRepository.findByTenderAndVendorSubmission(tender, vendor1)).thenReturn(results);
        when(vendorScoreRepository.save(any(VendorScore.class))).thenAnswer(inv -> inv.getArgument(0));

        TenderScoresResponse response = scoringService.calculateScores(tenderId);

        assertThat(response.scores()).hasSize(1);
        VendorScoreResponse score = response.scores().get(0);
        assertThat(score.vendorName()).isEqualTo("Dell Technologies");
        assertThat(score.overallScore()).isEqualTo(87.5);
        assertThat(score.totalSpecs()).isEqualTo(16);
        assertThat(score.matched()).isEqualTo(14);
        assertThat(score.unmatched()).isEqualTo(2);
        assertThat(score.uncertain()).isEqualTo(0);
        assertThat(score.rank()).isEqualTo(1);
    }

    @Test
    void shouldApplyCustomWeights() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(vendorSubmissionRepository.findByTender(tender)).thenReturn(List.of(vendor1));

        // Setup custom weights:
        // ram: critical (3.0) -> Match (1.0 * 3.0 = 3.0)
        // storage: high (2.0) -> Match (1.0 * 2.0 = 2.0)
        // color: low (0.5) -> Unmatch (0.0 * 0.5 = 0.0)
        // Total weighted sum = 5.0, Total weight = 5.5 -> 5.0 / 5.5 * 100 = 90.9%
        ScoringWeight w1 = ScoringWeight.builder().canonicalSpecName("ram").weightCategory("critical").weightValue(3.0).build();
        ScoringWeight w2 = ScoringWeight.builder().canonicalSpecName("storage").weightCategory("high").weightValue(2.0).build();
        ScoringWeight w3 = ScoringWeight.builder().canonicalSpecName("color").weightCategory("low").weightValue(0.5).build();

        when(scoringWeightRepository.findByTenderId(tenderId)).thenReturn(List.of(w1, w2, w3));

        List<MatchResult> results = List.of(
                createResult(vendor1, tenderProduct1, "ram", "Match", "Match", "16", "32"),
                createResult(vendor1, tenderProduct1, "storage", "Match", "Match", "512", "1024"),
                createResult(vendor1, tenderProduct1, "color", "Unmatch", "Unmatch", "Black", "Silver")
        );

        when(matchResultRepository.findByTenderAndVendorSubmission(tender, vendor1)).thenReturn(results);
        when(vendorScoreRepository.save(any(VendorScore.class))).thenAnswer(inv -> inv.getArgument(0));

        TenderScoresResponse response = scoringService.calculateScores(tenderId);

        VendorScoreResponse score = response.scores().get(0);
        assertThat(score.overallScore()).isEqualTo(90.9);
    }

    @Test
    void shouldScoreAiCantPredictAsPartial() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(vendorSubmissionRepository.findByTender(tender)).thenReturn(List.of(vendor1));
        when(scoringWeightRepository.findByTenderId(tenderId)).thenReturn(Collections.emptyList());

        // 1 Match (1.0), 1 AI Can't Predict (0.5) with equal weight 1.0
        // Score = (1.0 + 0.5) / 2.0 * 100 = 75.0%
        List<MatchResult> results = List.of(
                createResult(vendor1, tenderProduct1, "ram", "Match", "Match", "16", "32"),
                createResult(vendor1, tenderProduct1, "gpu", "AI Err", "AI Can't Predict", "RTX", "Custom")
        );

        when(matchResultRepository.findByTenderAndVendorSubmission(tender, vendor1)).thenReturn(results);
        when(vendorScoreRepository.save(any(VendorScore.class))).thenAnswer(inv -> inv.getArgument(0));

        TenderScoresResponse response = scoringService.calculateScores(tenderId);

        VendorScoreResponse score = response.scores().get(0);
        assertThat(score.overallScore()).isEqualTo(75.0);
        assertThat(score.uncertain()).isEqualTo(1);
    }

    @Test
    void shouldCalculatePerProductScores() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(vendorSubmissionRepository.findByTender(tender)).thenReturn(List.of(vendor1));
        when(scoringWeightRepository.findByTenderId(tenderId)).thenReturn(Collections.emptyList());

        // Product 1 (Laptop): 2 specs, both Match -> 100.0%
        // Product 2 (Monitor): 2 specs, 1 Match, 1 Unmatch -> 50.0%
        List<MatchResult> results = List.of(
                createResult(vendor1, tenderProduct1, "ram", "Match", "Match", "16", "32"),
                createResult(vendor1, tenderProduct1, "cpu", "Match", "Match", "i7", "i7"),
                createResult(vendor1, tenderProduct2, "screen_size", "Match", "Match", "27", "27"),
                createResult(vendor1, tenderProduct2, "resolution", "Unmatch", "Unmatch", "4K", "1080p")
        );

        when(matchResultRepository.findByTenderAndVendorSubmission(tender, vendor1)).thenReturn(results);
        when(vendorScoreRepository.save(any(VendorScore.class))).thenAnswer(inv -> inv.getArgument(0));

        TenderScoresResponse response = scoringService.calculateScores(tenderId);

        VendorScoreResponse score = response.scores().get(0);
        assertThat(score.productScores()).hasSize(2);
        assertThat(score.productScores()).anySatisfy(p -> {
            if ("Laptop".equals(p.productName())) {
                assertThat(p.score()).isEqualTo(100.0);
                assertThat(p.matched()).isEqualTo(2);
            }
            if ("Monitor".equals(p.productName())) {
                assertThat(p.score()).isEqualTo(50.0);
                assertThat(p.matched()).isEqualTo(1);
                assertThat(p.unmatched()).isEqualTo(1);
            }
        });
    }

    @Test
    void shouldRankVendorsByDescendingScore() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(vendorSubmissionRepository.findByTender(tender)).thenReturn(List.of(vendor1, vendor2));
        when(scoringWeightRepository.findByTenderId(tenderId)).thenReturn(Collections.emptyList());

        // Dell: 100%
        List<MatchResult> dellResults = List.of(
                createResult(vendor1, tenderProduct1, "ram", "Match", "Match", "16", "32")
        );
        // HP: 0%
        List<MatchResult> hpResults = List.of(
                createResult(vendor2, tenderProduct1, "ram", "Unmatch", "Unmatch", "16", "8")
        );

        when(matchResultRepository.findByTenderAndVendorSubmission(tender, vendor1)).thenReturn(dellResults);
        when(matchResultRepository.findByTenderAndVendorSubmission(tender, vendor2)).thenReturn(hpResults);
        when(vendorScoreRepository.save(any(VendorScore.class))).thenAnswer(inv -> inv.getArgument(0));

        TenderScoresResponse response = scoringService.calculateScores(tenderId);

        assertThat(response.scores()).hasSize(2);
        assertThat(response.scores().get(0).vendorName()).isEqualTo("Dell Technologies");
        assertThat(response.scores().get(0).rank()).isEqualTo(1);
        assertThat(response.scores().get(0).overallScore()).isEqualTo(100.0);

        assertThat(response.scores().get(1).vendorName()).isEqualTo("HP Inc.");
        assertThat(response.scores().get(1).rank()).isEqualTo(2);
        assertThat(response.scores().get(1).overallScore()).isEqualTo(0.0);
    }

    @Test
    void shouldHandleTiedScoresWithSameRank() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(vendorSubmissionRepository.findByTender(tender)).thenReturn(List.of(vendor1, vendor2));
        when(scoringWeightRepository.findByTenderId(tenderId)).thenReturn(Collections.emptyList());

        // Both 100%
        List<MatchResult> dellResults = List.of(
                createResult(vendor1, tenderProduct1, "ram", "Match", "Match", "16", "32")
        );
        List<MatchResult> hpResults = List.of(
                createResult(vendor2, tenderProduct1, "ram", "Match", "Match", "16", "32")
        );

        when(matchResultRepository.findByTenderAndVendorSubmission(tender, vendor1)).thenReturn(dellResults);
        when(matchResultRepository.findByTenderAndVendorSubmission(tender, vendor2)).thenReturn(hpResults);
        when(vendorScoreRepository.save(any(VendorScore.class))).thenAnswer(inv -> inv.getArgument(0));

        TenderScoresResponse response = scoringService.calculateScores(tenderId);

        assertThat(response.scores().get(0).rank()).isEqualTo(1);
        assertThat(response.scores().get(1).rank()).isEqualTo(1);
    }

    @Test
    void shouldAssembleComparisonMatrix() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(vendorSubmissionRepository.findByTender(tender)).thenReturn(List.of(vendor1, vendor2));
        when(scoringWeightRepository.findByTenderId(tenderId)).thenReturn(Collections.emptyList());

        MatchResult r1 = createResult(vendor1, tenderProduct1, "ram", "Match", "Match", "16", "32");
        MatchResult r2 = createResult(vendor2, tenderProduct1, "ram", "Unmatch", "Unmatch", "16", "8");
        r2.setTenderSpec(r1.getTenderSpec());

        when(matchResultRepository.findByTender(tender)).thenReturn(List.of(r1, r2));

        ComparisonMatrixResponse matrix = scoringService.getComparisonMatrix(tenderId);

        assertThat(matrix.tenderTitle()).isEqualTo("Government IT Procurement 2026");
        assertThat(matrix.vendorNames()).containsExactlyInAnyOrder("Dell Technologies", "HP Inc.");
        assertThat(matrix.rows()).hasSize(1);
        assertThat(matrix.rows().get(0).requiredSpecName()).isEqualTo("ram");
        assertThat(matrix.rows().get(0).vendorCells()).hasSize(2);
    }
}
