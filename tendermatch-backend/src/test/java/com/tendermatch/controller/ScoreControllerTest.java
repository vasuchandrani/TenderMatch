package com.tendermatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tendermatch.dto.scoring.*;
import com.tendermatch.security.CustomUserDetailsService;
import com.tendermatch.security.JwtService;
import com.tendermatch.service.ExportService;
import com.tendermatch.service.ScoringService;
import com.tendermatch.service.WeightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScoreController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScoringService scoringService;

    @MockBean
    private WeightService weightService;

    @MockBean
    private ExportService exportService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private UUID tenderId;
    private UUID vendorId;

    @BeforeEach
    void setUp() {
        tenderId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldCalculateScores() throws Exception {
        TenderScoresResponse res = new TenderScoresResponse(
                tenderId, "Tender 2026", Collections.emptyList(), "weighted_average", true, ZonedDateTime.now()
        );
        when(scoringService.calculateScores(tenderId)).thenReturn(res);

        mockMvc.perform(post("/api/tenders/{id}/calculate-scores", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenderTitle").value("Tender 2026"));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldGetTenderScores() throws Exception {
        TenderScoresResponse res = new TenderScoresResponse(
                tenderId, "Tender 2026", Collections.emptyList(), "weighted_average", true, ZonedDateTime.now()
        );
        when(scoringService.getTenderScores(tenderId)).thenReturn(res);

        mockMvc.perform(get("/api/tenders/{id}/scores", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenderTitle").value("Tender 2026"));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldGetVendorScore() throws Exception {
        VendorScoreResponse res = new VendorScoreResponse(
                UUID.randomUUID(), vendorId, "Dell", "dell.pdf", 90.0, 1, 10, 9, 1, 0, true,
                Collections.emptyList(), "weighted_average", ZonedDateTime.now()
        );
        when(scoringService.getVendorScore(tenderId, vendorId)).thenReturn(res);

        mockMvc.perform(get("/api/tenders/{id}/scores/{vendorId}", tenderId, vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vendorName").value("Dell"))
                .andExpect(jsonPath("$.data.overallScore").value(90.0));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldGetComparisonMatrix() throws Exception {
        ComparisonMatrixResponse res = new ComparisonMatrixResponse(
                tenderId, "Tender 2026", List.of("Dell", "HP"), Collections.emptyList()
        );
        when(scoringService.getComparisonMatrix(tenderId)).thenReturn(res);

        mockMvc.perform(get("/api/tenders/{id}/comparison-matrix", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vendorNames[0]").value("Dell"));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldGetWeights() throws Exception {
        List<SpecWeightDto> weights = List.of(new SpecWeightDto("ram", "high", 2.0));
        when(weightService.getWeightsForTender(tenderId)).thenReturn(weights);

        mockMvc.perform(get("/api/tenders/{id}/weights", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].canonicalSpecName").value("ram"));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldUpdateWeights() throws Exception {
        List<SpecWeightDto> weights = List.of(new SpecWeightDto("ram", "high", 2.0));
        UpdateWeightsRequest req = new UpdateWeightsRequest(weights);
        when(weightService.updateWeights(eq(tenderId), any(String.class), eq(weights))).thenReturn(weights);

        mockMvc.perform(put("/api/tenders/{id}/weights", tenderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].weightValue").value(2.0));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldSaveTemplate() throws Exception {
        List<SpecWeightDto> weights = List.of(new SpecWeightDto("ram", "high", 2.0));
        SaveWeightTemplateRequest req = new SaveWeightTemplateRequest("Template 1", weights);
        when(weightService.saveTemplate(any(String.class), eq("Template 1"), eq(weights)))
                .thenReturn(new WeightTemplateDto("Template 1", weights));

        mockMvc.perform(post("/api/tenders/{id}/weights/template", tenderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.templateName").value("Template 1"));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldListTemplates() throws Exception {
        when(weightService.listTemplates()).thenReturn(List.of(new WeightTemplateDto("Template 1", Collections.emptyList())));

        mockMvc.perform(get("/api/weights/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].templateName").value("Template 1"));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldApplyTemplate() throws Exception {
        List<SpecWeightDto> weights = List.of(new SpecWeightDto("ram", "high", 2.0));
        when(weightService.applyTemplate(eq(tenderId), any(String.class), eq("Template 1"))).thenReturn(weights);

        mockMvc.perform(post("/api/tenders/{id}/weights/from-template/{templateName}", tenderId, "Template 1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].canonicalSpecName").value("ram"));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldExportCsv() throws Exception {
        when(exportService.exportCsv(tenderId)).thenReturn("header1,header2\nval1,val2");

        mockMvc.perform(get("/api/tenders/{id}/export/csv", tenderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("header1,header2")));
    }

    @Test
    @WithMockUser(username = "admin@example.com")
    void shouldExportPdf() throws Exception {
        when(exportService.exportPdf(tenderId)).thenReturn("%PDF-1.4 mock".getBytes());

        mockMvc.perform(get("/api/tenders/{id}/export/pdf", tenderId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=")));
    }
}
