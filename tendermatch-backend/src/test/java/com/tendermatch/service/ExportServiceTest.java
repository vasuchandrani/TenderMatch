package com.tendermatch.service;

import com.tendermatch.dto.scoring.ProductScoreDto;
import com.tendermatch.dto.scoring.TenderScoresResponse;
import com.tendermatch.dto.scoring.VendorScoreResponse;
import com.tendermatch.model.*;
import com.tendermatch.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private VendorSubmissionRepository vendorSubmissionRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private ScoringWeightRepository scoringWeightRepository;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private ExportService exportService;

    private UUID tenderId;
    private Tender tender;
    private VendorSubmission vendor1;
    private TenderProduct product1;
    private TenderSpec tenderSpec1;
    private VendorSpec vendorSpec1;
    private MatchResult matchResult1;

    @BeforeEach
    void setUp() {
        tenderId = UUID.randomUUID();

        tender = Tender.builder()
                .id(tenderId)
                .name("IT Procurement 2026")
                .build();

        vendor1 = VendorSubmission.builder()
                .id(UUID.randomUUID())
                .tender(tender)
                .vendorName("Dell Technologies")
                .fileName("dell_quote.pdf")
                .build();

        product1 = new TenderProduct();
        product1.setId(UUID.randomUUID());
        product1.setTender(tender);
        product1.setProductName("Workstation");

        tenderSpec1 = new TenderSpec();
        tenderSpec1.setId(UUID.randomUUID());
        tenderSpec1.setTenderProduct(product1);
        tenderSpec1.setRawSpecName("RAM Capacity");
        tenderSpec1.setCanonicalSpecName("ram_capacity");
        tenderSpec1.setSpecValue("32");
        tenderSpec1.setUnit("GB");

        vendorSpec1 = new VendorSpec();
        vendorSpec1.setId(UUID.randomUUID());
        vendorSpec1.setRawSpecName("RAM");
        vendorSpec1.setCanonicalSpecName("ram_capacity");
        vendorSpec1.setSpecValue("64");
        vendorSpec1.setUnit("GB");

        matchResult1 = MatchResult.builder()
                .id(UUID.randomUUID())
                .tender(tender)
                .vendorSubmission(vendor1)
                .tenderProduct(product1)
                .tenderSpec(tenderSpec1)
                .vendorSpec(vendorSpec1)
                .systemVerdict("Match")
                .humanVerdict("Match")
                .confidenceScore(0.95)
                .comparisonOperator(">=")
                .build();
    }

    @Test
    void shouldGenerateCsvWithCorrectHeadersAndRows() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(matchResultRepository.findByTender(tender)).thenReturn(List.of(matchResult1));
        when(scoringWeightRepository.findByTenderId(tenderId)).thenReturn(Collections.emptyList());

        String csv = exportService.exportCsv(tenderId);

        assertThat(csv).isNotNull();
        assertThat(csv).startsWith("vendor_name,product_name,spec_name,required_value,required_unit,offered_value,offered_unit,system_verdict,human_verdict,confidence,weight,weighted_score");
        assertThat(csv).contains("Dell Technologies");
        assertThat(csv).contains("Workstation");
        assertThat(csv).contains("ram_capacity");
        assertThat(csv).contains("32");
        assertThat(csv).contains("64");
        assertThat(csv).contains("Match");
    }

    @Test
    void shouldGeneratePdfReport() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));

        VendorScoreResponse vsr = new VendorScoreResponse(
                UUID.randomUUID(),
                vendor1.getId(),
                "Dell Technologies",
                "dell_quote.pdf",
                95.0,
                1,
                10,
                9,
                1,
                0,
                true,
                List.of(new ProductScoreDto(product1.getId(), "Workstation", 95.0, 10, 9, 1, 0)),
                "weighted_average",
                ZonedDateTime.now()
        );

        TenderScoresResponse scores = new TenderScoresResponse(
                tenderId,
                "IT Procurement 2026",
                List.of(vsr),
                "weighted_average",
                true,
                ZonedDateTime.now()
        );

        when(scoringService.getTenderScores(tenderId)).thenReturn(scores);

        byte[] pdfBytes = exportService.exportPdf(tenderId);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(100);
        // Verify PDF magic bytes header %PDF
        String header = new String(Arrays.copyOfRange(pdfBytes, 0, 4), StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF");
    }
}
