package com.tendermatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tendermatch.dto.review.*;
import com.tendermatch.security.CustomUserDetailsService;
import com.tendermatch.security.JwtService;
import com.tendermatch.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private UUID tenderId;
    private UUID vendorId;
    private UUID resultId;

    @BeforeEach
    void setUp() {
        tenderId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        resultId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/tenders/{id}/review returns list of vendor review structures")
    @WithMockUser(username = "reviewer@example.com")
    void shouldGetTenderReviewData() throws Exception {
        ReviewProgressResponse progress = new ReviewProgressResponse(tenderId, vendorId, 0, 10, 0.0, 0, false);
        ReviewVendorDto vendorDto = new ReviewVendorDto(vendorId, "Acme Tech", "acme.pdf", false, List.of(), progress);

        when(reviewService.getTenderReviewData(eq(tenderId), eq("reviewer@example.com")))
                .thenReturn(List.of(vendorDto));

        mockMvc.perform(get("/api/tenders/{id}/review", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].vendorId").value(vendorId.toString()))
                .andExpect(jsonPath("$.data[0].vendorName").value("Acme Tech"));
    }

    @Test
    @DisplayName("GET /api/tenders/{id}/vendors/{vendorId}/review returns vendor review structure")
    @WithMockUser(username = "reviewer@example.com")
    void shouldGetVendorReviewData() throws Exception {
        ReviewProgressResponse progress = new ReviewProgressResponse(tenderId, vendorId, 2, 10, 20.0, 1, false);
        ReviewVendorDto vendorDto = new ReviewVendorDto(vendorId, "Acme Tech", "acme.pdf", false, List.of(), progress);

        when(reviewService.getVendorReviewData(eq(tenderId), eq(vendorId), eq("reviewer@example.com")))
                .thenReturn(vendorDto);

        mockMvc.perform(get("/api/tenders/{id}/vendors/{vendorId}/review", tenderId, vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vendorName").value("Acme Tech"))
                .andExpect(jsonPath("$.data.progress.percentage").value(20.0));
    }

    @Test
    @DisplayName("PUT /api/tenders/{id}/results/{resultId}/verdict updates human verdict successfully")
    @WithMockUser(username = "reviewer@example.com")
    void shouldSubmitHumanVerdict() throws Exception {
        SubmitVerdictRequest request = new SubmitVerdictRequest("Unmatch", "Hardware incompatible");
        ReviewProgressResponse progress = new ReviewProgressResponse(tenderId, vendorId, 1, 10, 10.0, 1, false);
        SubmitVerdictResponse response = new SubmitVerdictResponse(
                resultId, "Match", "Unmatch", true, "Hardware incompatible",
                UUID.randomUUID(), progress, ZonedDateTime.now());

        when(reviewService.submitVerdict(eq(tenderId), eq(resultId), any(SubmitVerdictRequest.class), eq("reviewer@example.com")))
                .thenReturn(response);

        mockMvc.perform(put("/api/tenders/{id}/results/{resultId}/verdict", tenderId, resultId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.humanVerdict").value("Unmatch"))
                .andExpect(jsonPath("$.data.isOverride").value(true));
    }

    @Test
    @DisplayName("PUT /api/tenders/{id}/results/{resultId}/verdict returns 400 for invalid verdict")
    @WithMockUser(username = "reviewer@example.com")
    void shouldRejectInvalidHumanVerdict() throws Exception {
        String invalidJson = "{\"humanVerdict\": \"InvalidVerdict\", \"comment\": \"invalid\"}";

        mockMvc.perform(put("/api/tenders/{id}/results/{resultId}/verdict", tenderId, resultId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/tenders/{id}/results/{resultId}/verdict returns 409 when review is locked")
    @WithMockUser(username = "reviewer@example.com")
    void shouldReturn409WhenReviewIsLocked() throws Exception {
        SubmitVerdictRequest request = new SubmitVerdictRequest("Match", null);

        when(reviewService.submitVerdict(eq(tenderId), eq(resultId), any(SubmitVerdictRequest.class), eq("reviewer@example.com")))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Review is finalized and locked for this vendor"));

        mockMvc.perform(put("/api/tenders/{id}/results/{resultId}/verdict", tenderId, resultId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/tenders/{id}/vendors/{vendorId}/bulk-confirm bulk confirms remaining specs")
    @WithMockUser(username = "reviewer@example.com")
    void shouldBulkConfirmRemainingSpecs() throws Exception {
        ReviewProgressResponse progress = new ReviewProgressResponse(tenderId, vendorId, 10, 10, 100.0, 0, false);

        when(reviewService.bulkConfirmRemaining(eq(tenderId), eq(vendorId), eq("reviewer@example.com")))
                .thenReturn(progress);

        mockMvc.perform(post("/api/tenders/{id}/vendors/{vendorId}/bulk-confirm", tenderId, vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.percentage").value(100.0));
    }

    @Test
    @DisplayName("GET /api/tenders/{id}/vendors/{vendorId}/review-summary returns summary report")
    @WithMockUser(username = "reviewer@example.com")
    void shouldGetReviewSummary() throws Exception {
        ReviewSummaryResponse summary = new ReviewSummaryResponse(
                tenderId, vendorId, "Acme Tech", false, 8, 2, 0, 10, 1, List.of());

        when(reviewService.getReviewSummary(eq(tenderId), eq(vendorId), eq("reviewer@example.com")))
                .thenReturn(summary);

        mockMvc.perform(get("/api/tenders/{id}/vendors/{vendorId}/review-summary", tenderId, vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vendorName").value("Acme Tech"))
                .andExpect(jsonPath("$.data.matchCount").value(8));
    }

    @Test
    @DisplayName("POST /api/tenders/{id}/vendors/{vendorId}/finalize locks review session")
    @WithMockUser(username = "reviewer@example.com")
    void shouldFinalizeVendorReview() throws Exception {
        ReviewSummaryResponse summary = new ReviewSummaryResponse(
                tenderId, vendorId, "Acme Tech", true, 8, 2, 0, 10, 1, List.of());

        when(reviewService.finalizeReview(eq(tenderId), eq(vendorId), eq("reviewer@example.com")))
                .thenReturn(summary);

        mockMvc.perform(post("/api/tenders/{id}/vendors/{vendorId}/finalize", tenderId, vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isLocked").value(true));
    }

    @Test
    @DisplayName("POST /api/tenders/{id}/vendors/{vendorId}/reverify-reflection performs Pass 2 reflection and locks session")
    @WithMockUser(username = "reviewer@example.com")
    void shouldReverifyReflectionVendorReview() throws Exception {
        ReviewSummaryResponse summary = new ReviewSummaryResponse(
                tenderId, vendorId, "Acme Tech", true, 9, 1, 0, 10, 2, List.of());

        when(reviewService.reverifyWithAiReflection(eq(tenderId), eq(vendorId), eq("reviewer@example.com")))
                .thenReturn(summary);

        mockMvc.perform(post("/api/tenders/{id}/vendors/{vendorId}/reverify-reflection", tenderId, vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isLocked").value(true))
                .andExpect(jsonPath("$.data.matchCount").value(9));
    }
}

