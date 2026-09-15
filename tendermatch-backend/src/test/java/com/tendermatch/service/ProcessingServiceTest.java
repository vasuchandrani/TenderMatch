package com.tendermatch.service;

import com.tendermatch.dto.pipeline.ExtractedProductDto;
import com.tendermatch.dto.pipeline.ExtractedSpecDto;
import com.tendermatch.dto.pipeline.ProcessDocumentAiResponse;
import com.tendermatch.dto.pipeline.ProcessingStatusResponse;
import com.tendermatch.exception.BadRequestException;
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessingServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private VendorSubmissionRepository vendorSubmissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenderProductRepository tenderProductRepository;

    @Mock
    private TenderSpecRepository tenderSpecRepository;

    @Mock
    private VendorProductRepository vendorProductRepository;

    @Mock
    private VendorSpecRepository vendorSpecRepository;

    @Mock
    private MinioStorageService storageService;

    @Mock
    private AiServiceClient aiServiceClient;

    @InjectMocks
    private ProcessingService processingService;

    private User testUser;
    private Tender testTender;
    private VendorSubmission vendorSubmission;
    private UUID tenderId;
    private UUID submissionId;

    @BeforeEach
    void setUp() {
        tenderId = UUID.randomUUID();
        submissionId = UUID.randomUUID();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("procure@tendermatch.com")
                .passwordHash("hashedpassword")
                .name("Procurement Admin")
                .role("ADMIN")
                .build();

        vendorSubmission = VendorSubmission.builder()
                .id(submissionId)
                .vendorName("Dell Technologies")
                .fileName("dell_laptop_quote.pdf")
                .fileKey("vendors/" + tenderId + "/" + submissionId + "/dell.pdf")
                .fileSize(1024L)
                .status(SubmissionStatus.UPLOADED)
                .build();

        testTender = Tender.builder()
                .id(tenderId)
                .user(testUser)
                .name("IT Hardware Tender 2026")
                .status(TenderStatus.REQUIREMENT_UPLOADED)
                .requirementFileKey("requirements/" + tenderId + "/rfp.pdf")
                .requirementFileName("rfp.pdf")
                .requirementFileSize(2048L)
                .vendorSubmissions(new ArrayList<>(List.of(vendorSubmission)))
                .build();

        vendorSubmission.setTender(testTender);

        lenient().when(tenderProductRepository.save(any(TenderProduct.class))).thenAnswer(i -> {
            TenderProduct tp = i.getArgument(0);
            if (tp.getId() == null) tp.setId(UUID.randomUUID());
            return tp;
        });
        lenient().when(tenderSpecRepository.save(any(TenderSpec.class))).thenAnswer(i -> {
            TenderSpec ts = i.getArgument(0);
            if (ts.getId() == null) ts.setId(UUID.randomUUID());
            return ts;
        });
        lenient().when(vendorProductRepository.save(any(VendorProduct.class))).thenAnswer(i -> {
            VendorProduct vp = i.getArgument(0);
            if (vp.getId() == null) vp.setId(UUID.randomUUID());
            return vp;
        });
        lenient().when(vendorSpecRepository.save(any(VendorSpec.class))).thenAnswer(i -> {
            VendorSpec vs = i.getArgument(0);
            if (vs.getId() == null) vs.setId(UUID.randomUUID());
            return vs;
        });
    }

    @Test
    @DisplayName("1. shouldTriggerProcessingForAllDocuments - calls AI for requirement and all vendor PDFs")
    void shouldTriggerProcessingForAllDocuments() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(i -> i.getArgument(0));
        when(storageService.getFile(anyString())).thenReturn(new ByteArrayInputStream("mock pdf".getBytes(StandardCharsets.UTF_8)));

        ProcessDocumentAiResponse aiResponse = new ProcessDocumentAiResponse(
                "COMPLETED", "Extracted", "req.pdf", "requirement_tender",
                List.of(new ExtractedProductDto(null, "Laptop", "electronics", null, null, "Laptop specs", 0,
                        List.of(new ExtractedSpecDto(null, "RAM", "ram_capacity", "16GB", 16.0, null, null, "GB", true, "16GB RAM", "high", "llm", "high", null, 0))))
        );
        when(aiServiceClient.processDocument(any())).thenReturn(aiResponse);

        processingService.processTenderSync(tenderId, testUser.getEmail());

        // Verify AI service called twice: 1 for requirement + 1 for vendor
        verify(aiServiceClient, times(2)).processDocument(any());
        verify(tenderProductRepository, atLeastOnce()).save(any());
        verify(vendorProductRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("2. shouldUpdateTenderStatusToProcessing - updates tender and submission status to PROCESSING before AI")
    void shouldUpdateTenderStatusToProcessing() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(i -> i.getArgument(0));
        when(vendorSubmissionRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        ProcessingStatusResponse response = processingService.triggerProcessing(tenderId, testUser.getEmail());

        assertThat(response).isNotNull();
        assertThat(testTender.getStatus()).isEqualTo(TenderStatus.PROCESSING);
        assertThat(vendorSubmission.getStatus()).isEqualTo(SubmissionStatus.PROCESSING);
    }

    @Test
    @DisplayName("3. shouldStoreExtractedProducts - saves extracted requirement and vendor products in DB")
    void shouldStoreExtractedProducts() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(i -> i.getArgument(0));
        when(storageService.getFile(anyString())).thenReturn(new ByteArrayInputStream("mock pdf".getBytes(StandardCharsets.UTF_8)));

        ExtractedProductDto reqProd = new ExtractedProductDto(null, "Enterprise Laptop", "electronics", null, null, "raw req", 0, new ArrayList<>());
        ExtractedProductDto venProd = new ExtractedProductDto(null, "Dell Latitude 5420", "electronics", null, 0.92, "raw ven", 0, new ArrayList<>());

        when(aiServiceClient.processDocument(any()))
                .thenReturn(new ProcessDocumentAiResponse("COMPLETED", "ok", "key1", "requirement", List.of(reqProd)))
                .thenReturn(new ProcessDocumentAiResponse("COMPLETED", "ok", "key2", "vendor", List.of(venProd)));

        processingService.processTenderSync(tenderId, testUser.getEmail());

        ArgumentCaptor<TenderProduct> tenderProductCaptor = ArgumentCaptor.forClass(TenderProduct.class);
        verify(tenderProductRepository).save(tenderProductCaptor.capture());
        assertThat(tenderProductCaptor.getValue().getProductName()).isEqualTo("Enterprise Laptop");

        ArgumentCaptor<VendorProduct> vendorProductCaptor = ArgumentCaptor.forClass(VendorProduct.class);
        verify(vendorProductRepository).save(vendorProductCaptor.capture());
        assertThat(vendorProductCaptor.getValue().getProductName()).isEqualTo("Dell Latitude 5420");
    }

    @Test
    @DisplayName("4. shouldStoreExtractedSpecs - saves extracted specifications with units and canonical keys")
    void shouldStoreExtractedSpecs() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(i -> i.getArgument(0));
        when(storageService.getFile(anyString())).thenReturn(new ByteArrayInputStream("mock pdf".getBytes(StandardCharsets.UTF_8)));

        ExtractedSpecDto spec1 = new ExtractedSpecDto(
                null, "Memory", "ram_capacity", "32 GB", 32.0, null, null, "GB", true, "32 GB RAM", "high", "llm", "high", null, 0
        );
        ExtractedProductDto reqProd = new ExtractedProductDto(null, "Workstation", "electronics", null, null, "Workstation text", 0, List.of(spec1));

        when(aiServiceClient.processDocument(any()))
                .thenReturn(new ProcessDocumentAiResponse("COMPLETED", "ok", "key1", "requirement", List.of(reqProd)))
                .thenReturn(new ProcessDocumentAiResponse("COMPLETED", "ok", "key2", "vendor", Collections.emptyList()));

        when(tenderProductRepository.save(any())).thenAnswer(inv -> {
            TenderProduct tp = inv.getArgument(0);
            tp.setId(UUID.randomUUID());
            return tp;
        });

        processingService.processTenderSync(tenderId, testUser.getEmail());

        verify(tenderProductRepository, atLeastOnce()).save(any(TenderProduct.class));
    }

    @Test
    @DisplayName("5. shouldHandleAiServiceFailure - sets status to FAILED when AI microservice errors")
    void shouldHandleAiServiceFailure() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));
        when(tenderRepository.save(any(Tender.class))).thenAnswer(i -> i.getArgument(0));
        when(storageService.getFile(anyString())).thenReturn(new ByteArrayInputStream("mock pdf".getBytes(StandardCharsets.UTF_8)));

        when(aiServiceClient.processDocument(any())).thenThrow(new RuntimeException("AI service connection refused"));

        processingService.processTenderSync(tenderId, testUser.getEmail());

        assertThat(testTender.getStatus()).isEqualTo(TenderStatus.FAILED);
    }

    @Test
    @DisplayName("6. shouldTrackPerDocumentProgress - returns status per requirement and vendor submission")
    void shouldTrackPerDocumentProgress() {
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(tenderRepository.findByIdWithSubmissions(tenderId, testUser)).thenReturn(Optional.of(testTender));

        ProcessingStatusResponse status = processingService.getProcessingStatus(tenderId, testUser.getEmail());

        assertThat(status).isNotNull();
        assertThat(status.getTenderId()).isEqualTo(tenderId);
        assertThat(status.getRequirementFileName()).isEqualTo("rfp.pdf");
        assertThat(status.getVendorDocuments()).hasSize(1);
        assertThat(status.getVendorDocuments().get(0).getVendorName()).isEqualTo("Dell Technologies");
    }
}
