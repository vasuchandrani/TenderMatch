package com.tendermatch.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tendermatch.dto.scoring.ProductScoreDto;
import com.tendermatch.dto.scoring.TenderScoresResponse;
import com.tendermatch.dto.scoring.VendorScoreResponse;
import com.tendermatch.model.MatchResult;
import com.tendermatch.model.ScoringWeight;
import com.tendermatch.model.Tender;
import com.tendermatch.repository.MatchResultRepository;
import com.tendermatch.repository.ScoringWeightRepository;
import com.tendermatch.repository.TenderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final TenderRepository tenderRepository;
    private final MatchResultRepository matchResultRepository;
    private final ScoringWeightRepository scoringWeightRepository;
    private final ScoringService scoringService;

    public ExportService(TenderRepository tenderRepository,
                         MatchResultRepository matchResultRepository,
                         ScoringWeightRepository scoringWeightRepository,
                         ScoringService scoringService) {
        this.tenderRepository = tenderRepository;
        this.matchResultRepository = matchResultRepository;
        this.scoringWeightRepository = scoringWeightRepository;
        this.scoringService = scoringService;
    }

    public String exportCsv(UUID tenderId) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found: " + tenderId));

        List<MatchResult> results = matchResultRepository.findByTender(tender);
        List<ScoringWeight> weights = scoringWeightRepository.findByTenderId(tenderId);
        Map<String, Double> weightMap = weights.stream()
                .collect(Collectors.toMap(ScoringWeight::getCanonicalSpecName, ScoringWeight::getWeightValue, (a, b) -> b));

        StringBuilder sb = new StringBuilder();
        sb.append("vendor_name,product_name,spec_name,required_value,required_unit,offered_value,offered_unit,system_verdict,human_verdict,confidence,weight,weighted_score\n");

        for (MatchResult r : results) {
            String vendorName = r.getVendorSubmission() != null ? r.getVendorSubmission().getVendorName() : "";
            String productName = r.getTenderProduct() != null ? r.getTenderProduct().getProductName() : "";
            String specName = r.getTenderSpec() != null && r.getTenderSpec().getCanonicalSpecName() != null
                    ? r.getTenderSpec().getCanonicalSpecName()
                    : (r.getTenderSpec() != null ? r.getTenderSpec().getRawSpecName() : "");
            String reqVal = r.getTenderSpec() != null ? r.getTenderSpec().getSpecValue() : "";
            String reqUnit = r.getTenderSpec() != null && r.getTenderSpec().getUnit() != null ? r.getTenderSpec().getUnit() : "";
            String offVal = r.getVendorSpec() != null ? r.getVendorSpec().getSpecValue() : "-";
            String offUnit = r.getVendorSpec() != null && r.getVendorSpec().getUnit() != null ? r.getVendorSpec().getUnit() : "";
            String sysVer = r.getSystemVerdict() != null ? r.getSystemVerdict() : "";
            String humVer = r.getHumanVerdict() != null ? r.getHumanVerdict() : "";
            double conf = r.getConfidenceScore() != null ? r.getConfidenceScore() : 0.0;
            double weight = weightMap.getOrDefault(specName, 1.0);

            String effVerdict = !humVer.isBlank() ? humVer : sysVer;
            double score = "Match".equalsIgnoreCase(effVerdict) ? 1.0 : ("AI Can't Predict".equalsIgnoreCase(effVerdict) || "AI Err".equalsIgnoreCase(effVerdict) ? 0.5 : 0.0);
            double weightedScore = weight * score;

            sb.append(escapeCsv(vendorName)).append(",")
                    .append(escapeCsv(productName)).append(",")
                    .append(escapeCsv(specName)).append(",")
                    .append(escapeCsv(reqVal)).append(",")
                    .append(escapeCsv(reqUnit)).append(",")
                    .append(escapeCsv(offVal)).append(",")
                    .append(escapeCsv(offUnit)).append(",")
                    .append(escapeCsv(sysVer)).append(",")
                    .append(escapeCsv(humVer)).append(",")
                    .append(conf).append(",")
                    .append(weight).append(",")
                    .append(weightedScore).append("\n");
        }

        return sb.toString();
    }

    public byte[] exportPdf(UUID tenderId) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found: " + tenderId));

        TenderScoresResponse scores = scoringService.getTenderScores(tenderId);
        List<MatchResult> results = matchResultRepository.findByTender(tender);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 41, 59));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
            Font cellBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);

            // Title
            Paragraph title = new Paragraph("TenderMatch — Executive Evaluation Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph meta = new Paragraph(
                    "Tender: " + tender.getName() + " | Generated: " +
                            java.time.ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    subtitleFont
            );
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(18);
            document.add(meta);

            // Section 1: Leaderboard Table
            Paragraph leaderboardTitle = new Paragraph("1. Vendor Ranking Leaderboard", sectionFont);
            leaderboardTitle.setSpacingAfter(8);
            document.add(leaderboardTitle);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 4.0f, 2.0f, 1.5f, 1.5f, 1.5f});
            table.setSpacingAfter(16);

            addHeaderCell(table, "Rank", headerFont);
            addHeaderCell(table, "Vendor Name", headerFont);
            addHeaderCell(table, "Overall Score", headerFont);
            addHeaderCell(table, "Matched", headerFont);
            addHeaderCell(table, "Unmatched", headerFont);
            addHeaderCell(table, "Uncertain", headerFont);

            for (VendorScoreResponse vs : scores.scores()) {
                String rankText = vs.rank() != null ? "#" + vs.rank() : "-";
                addCell(table, rankText, cellBold, Element.ALIGN_CENTER);
                addCell(table, vs.vendorName(), cellBold, Element.ALIGN_LEFT);
                addCell(table, vs.overallScore() + "%", cellBold, Element.ALIGN_CENTER);
                addCell(table, String.valueOf(vs.matched()), cellFont, Element.ALIGN_CENTER);
                addCell(table, String.valueOf(vs.unmatched()), cellFont, Element.ALIGN_CENTER);
                addCell(table, String.valueOf(vs.uncertain()), cellFont, Element.ALIGN_CENTER);
            }
            document.add(table);

            // Section 2: Detailed Product Breakdown per Vendor
            Paragraph detailsTitle = new Paragraph("2. Per-Product Evaluation Summary", sectionFont);
            detailsTitle.setSpacingAfter(8);
            document.add(detailsTitle);

            for (VendorScoreResponse vs : scores.scores()) {
                Paragraph vendorHeading = new Paragraph("• " + vs.vendorName() + " (Final Score: " + vs.overallScore() + "%)", cellBold);
                vendorHeading.setSpacingAfter(4);
                document.add(vendorHeading);

                if (vs.productScores() != null && !vs.productScores().isEmpty()) {
                    PdfPTable prodTable = new PdfPTable(5);
                    prodTable.setWidthPercentage(95);
                    prodTable.setWidths(new float[]{3.5f, 1.5f, 1.5f, 1.5f, 1.5f});
                    prodTable.setSpacingAfter(10);

                    addHeaderCell(prodTable, "Product", headerFont);
                    addHeaderCell(prodTable, "Score", headerFont);
                    addHeaderCell(prodTable, "Matched", headerFont);
                    addHeaderCell(prodTable, "Unmatched", headerFont);
                    addHeaderCell(prodTable, "Uncertain", headerFont);

                    for (ProductScoreDto ps : vs.productScores()) {
                        addCell(prodTable, ps.productName(), cellFont, Element.ALIGN_LEFT);
                        addCell(prodTable, ps.score() + "%", cellBold, Element.ALIGN_CENTER);
                        addCell(prodTable, String.valueOf(ps.matched()), cellFont, Element.ALIGN_CENTER);
                        addCell(prodTable, String.valueOf(ps.unmatched()), cellFont, Element.ALIGN_CENTER);
                        addCell(prodTable, String.valueOf(ps.uncertain()), cellFont, Element.ALIGN_CENTER);
                    }
                    document.add(prodTable);
                }
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            log.error("Failed to generate PDF export: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF generation failed: " + e.getMessage());
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(30, 41, 59));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String escapeCsv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
