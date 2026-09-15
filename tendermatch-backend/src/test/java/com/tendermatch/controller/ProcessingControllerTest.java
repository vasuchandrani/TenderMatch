package com.tendermatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tendermatch.dto.pipeline.ExtractedProductDto;
import com.tendermatch.dto.pipeline.ExtractedSpecDto;
import com.tendermatch.dto.pipeline.ProcessingStatusResponse;
import com.tendermatch.model.enums.SubmissionStatus;
import com.tendermatch.model.enums.TenderStatus;
import com.tendermatch.security.CustomUserDetailsService;
import com.tendermatch.security.JwtService;
import com.tendermatch.service.ProcessingService;
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

@WebMvcTest(ProcessingController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProcessingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessingService processingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private UUID tenderId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        tenderId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    @WithMockUser(username = "procure@tendermatch.com")
    @DisplayName("1. POST /api/tenders/{id}/process returns 202 Accepted and triggers async extraction")
    void triggerProcess_shouldReturn202Accepted() throws Exception {
        ProcessingStatusResponse statusResponse = new ProcessingStatusResponse(
                tenderId, TenderStatus.PROCESSING, "rfp.pdf", "requirements/rfp.pdf",
                "PROCESSING", 2, 0, List.of()
        );
        when(processingService.triggerProcessing(eq(tenderId), any())).thenReturn(statusResponse);
        doNothing().when(processingService).processTenderAsync(eq(tenderId), any());

        mockMvc.perform(post("/api/tenders/{id}/process", tenderId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenderStatus").value("PROCESSING"));
    }

    @Test
    @WithMockUser(username = "procure@tendermatch.com")
    @DisplayName("2. GET /api/tenders/{id}/processing-status returns 200 with per-document status")
    void getProcessingStatus_shouldReturn200() throws Exception {
        ProcessingStatusResponse.VendorDocumentStatus vDoc = new ProcessingStatusResponse.VendorDocumentStatus(
                UUID.randomUUID(), "Dell Inc", "quote.pdf", SubmissionStatus.EXTRACTED, 3
        );
        ProcessingStatusResponse statusResponse = new ProcessingStatusResponse(
                tenderId, TenderStatus.EXTRACTED, "rfp.pdf", "requirements/rfp.pdf",
                "DONE", 2, 2, List.of(vDoc)
        );
        when(processingService.getProcessingStatus(eq(tenderId), any())).thenReturn(statusResponse);

        mockMvc.perform(get("/api/tenders/{id}/processing-status", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tenderStatus").value("EXTRACTED"))
                .andExpect(jsonPath("$.data.totalDocuments").value(2))
                .andExpect(jsonPath("$.data.completedDocuments").value(2))
                .andExpect(jsonPath("$.data.vendorDocuments[0].vendorName").value("Dell Inc"));
    }

    @Test
    @WithMockUser(username = "procure@tendermatch.com")
    @DisplayName("3. GET /api/tenders/{id}/products returns 200 with extracted products")
    void getProducts_shouldReturn200() throws Exception {
        ExtractedProductDto product = new ExtractedProductDto(
                productId, "Enterprise Laptop", "electronics", null, null, "Raw text", 0, List.of()
        );
        when(processingService.getTenderProducts(eq(tenderId), any())).thenReturn(List.of(product));

        mockMvc.perform(get("/api/tenders/{id}/products", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].product_name").value("Enterprise Laptop"));
    }

    @Test
    @WithMockUser(username = "procure@tendermatch.com")
    @DisplayName("4. GET /api/tenders/{id}/products/{pid}/specs returns 200 with extracted specs")
    void getSpecs_shouldReturn200() throws Exception {
        ExtractedSpecDto spec = new ExtractedSpecDto(
                UUID.randomUUID(), "RAM", "ram_capacity", "16GB", 16.0, null, null, "GB", true,
                "16GB RAM", "high", "llm", "high", null, 0
        );
        when(processingService.getTenderProductSpecs(eq(tenderId), eq(productId), any())).thenReturn(List.of(spec));

        mockMvc.perform(get("/api/tenders/{id}/products/{productId}/specs", tenderId, productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].raw_spec_name").value("RAM"))
                .andExpect(jsonPath("$.data[0].canonical_spec_name").value("ram_capacity"))
                .andExpect(jsonPath("$.data[0].spec_value").value("16GB"));
    }
}
