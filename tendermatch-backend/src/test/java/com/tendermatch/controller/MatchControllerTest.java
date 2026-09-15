package com.tendermatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tendermatch.dto.matching.MatchResultResponse;
import com.tendermatch.dto.matching.VendorMatchSummaryDto;
import com.tendermatch.security.CustomUserDetailsService;
import com.tendermatch.security.JwtService;
import com.tendermatch.service.MatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchController.class)
@AutoConfigureMockMvc(addFilters = false)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MatchingService matchingService;

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
    @WithMockUser(username = "procure@tendermatch.com")
    @DisplayName("1. POST /api/tenders/{id}/match returns 202 Accepted")
    void triggerMatch_shouldReturn202Accepted() throws Exception {
        doNothing().when(matchingService).triggerMatching(eq(tenderId), any());
        doNothing().when(matchingService).matchTenderAsync(eq(tenderId), any());

        mockMvc.perform(post("/api/tenders/{id}/match", tenderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "procure@tendermatch.com")
    @DisplayName("2. GET /api/tenders/{id}/results returns 200 with all match results")
    void getResults_shouldReturn200() throws Exception {
        MatchResultResponse result = new MatchResultResponse();
        result.setId(UUID.randomUUID());
        result.setSystemVerdict("Match");
        result.setConfidenceScore(0.95);

        when(matchingService.getMatchResults(eq(tenderId), any())).thenReturn(List.of(result));

        mockMvc.perform(get("/api/tenders/{id}/results", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].systemVerdict").value("Match"));
    }

    @Test
    @WithMockUser(username = "procure@tendermatch.com")
    @DisplayName("3. GET /api/tenders/{id}/vendors/{vid}/results returns 200 with vendor results")
    void getVendorResults_shouldReturn200() throws Exception {
        MatchResultResponse result = new MatchResultResponse();
        result.setId(UUID.randomUUID());
        result.setSystemVerdict("Unmatch");
        result.setVendorName("Lenovo");

        when(matchingService.getVendorResults(eq(tenderId), eq(vendorId), any())).thenReturn(List.of(result));

        mockMvc.perform(get("/api/tenders/{id}/vendors/{vendorId}/results", tenderId, vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].systemVerdict").value("Unmatch"))
                .andExpect(jsonPath("$.data[0].vendorName").value("Lenovo"));
    }

    @Test
    @WithMockUser(username = "procure@tendermatch.com")
    @DisplayName("4. GET /api/tenders/{id}/results/summary returns 200 with vendor summaries")
    void getResultsSummary_shouldReturn200() throws Exception {
        VendorMatchSummaryDto summary = new VendorMatchSummaryDto(vendorId, "Lenovo", "quote.pdf", 8, 2, 1);
        when(matchingService.getMatchSummary(eq(tenderId), any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/tenders/{id}/results/summary", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].vendorName").value("Lenovo"))
                .andExpect(jsonPath("$.data[0].matchCount").value(8))
                .andExpect(jsonPath("$.data[0].unmatchCount").value(2))
                .andExpect(jsonPath("$.data[0].aiErrCount").value(1));
    }

    @Test
    @WithMockUser(username = "procure@tendermatch.com")
    @DisplayName("5. GET /api/tenders/{id}/match-status returns 200 with status info")
    void getMatchStatus_shouldReturn200() throws Exception {
        VendorMatchSummaryDto summary = new VendorMatchSummaryDto(vendorId, "Lenovo", "quote.pdf", 8, 2, 1);
        when(matchingService.getMatchSummary(eq(tenderId), any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/tenders/{id}/match-status", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
