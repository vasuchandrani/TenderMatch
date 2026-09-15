package com.tendermatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tendermatch.dto.tender.CreateTenderRequest;
import com.tendermatch.dto.tender.TenderResponse;
import com.tendermatch.dto.tender.VendorSubmissionResponse;
import com.tendermatch.model.enums.SubmissionStatus;
import com.tendermatch.model.enums.TenderStatus;
import com.tendermatch.security.CustomUserDetailsService;
import com.tendermatch.security.JwtService;
import com.tendermatch.service.TenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TenderController.class)
@AutoConfigureMockMvc(addFilters = false)
class TenderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenderService tenderService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private UUID tenderId;
    private TenderResponse mockTenderResponse;

    @BeforeEach
    void setUp() {
        tenderId = UUID.randomUUID();
        mockTenderResponse = new TenderResponse(
                tenderId,
                "Laptops Tender",
                "Procurement description",
                TenderStatus.CREATED,
                null,
                null,
                null,
                new ArrayList<>(),
                ZonedDateTime.now(),
                ZonedDateTime.now()
        );
    }

    @Test
    @WithMockUser(username = "buyer@gov.in")
    void shouldCreateTender() throws Exception {
        CreateTenderRequest request = new CreateTenderRequest("Laptops Tender", "Procurement description");
        when(tenderService.createTender(any(CreateTenderRequest.class), eq("buyer@gov.in")))
                .thenReturn(mockTenderResponse);

        mockMvc.perform(post("/api/tenders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(tenderId.toString()))
                .andExpect(jsonPath("$.data.name").value("Laptops Tender"));
    }

    @Test
    @WithMockUser(username = "buyer@gov.in")
    void shouldUploadRequirementPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "requirement.pdf",
                "application/pdf",
                "pdf binary dummy".getBytes()
        );

        mockTenderResponse.setStatus(TenderStatus.REQUIREMENT_UPLOADED);
        mockTenderResponse.setRequirementFileName("requirement.pdf");

        when(tenderService.uploadRequirementPdf(eq(tenderId), any(), eq("buyer@gov.in")))
                .thenReturn(mockTenderResponse);

        mockMvc.perform(multipart("/api/tenders/{id}/requirement", tenderId)
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requirementFileName").value("requirement.pdf"));
    }

    @Test
    @WithMockUser(username = "buyer@gov.in")
    void shouldUploadVendorPdfs() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "VendorA.pdf", "application/pdf", "pdf1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "VendorB.pdf", "application/pdf", "pdf2".getBytes());

        List<VendorSubmissionResponse> subs = List.of(
                new VendorSubmissionResponse(UUID.randomUUID(), tenderId, "Vendor A", "VendorA.pdf", 5L, SubmissionStatus.UPLOADED, ZonedDateTime.now()),
                new VendorSubmissionResponse(UUID.randomUUID(), tenderId, "Vendor B", "VendorB.pdf", 5L, SubmissionStatus.UPLOADED, ZonedDateTime.now())
        );

        when(tenderService.uploadVendorPdfs(eq(tenderId), any(), eq("buyer@gov.in")))
                .thenReturn(subs);

        mockMvc.perform(multipart("/api/tenders/{id}/vendors", tenderId)
                        .file(file1)
                        .file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = "buyer@gov.in")
    void shouldGetTenderById() throws Exception {
        when(tenderService.getTenderById(eq(tenderId), eq("buyer@gov.in")))
                .thenReturn(mockTenderResponse);

        mockMvc.perform(get("/api/tenders/{id}", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(tenderId.toString()));
    }

    @Test
    @WithMockUser(username = "buyer@gov.in")
    void shouldListTenders() throws Exception {
        PageImpl<TenderResponse> page = new PageImpl<>(List.of(mockTenderResponse), PageRequest.of(0, 20), 1);
        when(tenderService.listTenders(eq("buyer@gov.in"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/tenders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @WithMockUser(username = "buyer@gov.in")
    void shouldDeleteTender() throws Exception {
        doNothing().when(tenderService).deleteTender(eq(tenderId), eq("buyer@gov.in"));

        mockMvc.perform(delete("/api/tenders/{id}", tenderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
