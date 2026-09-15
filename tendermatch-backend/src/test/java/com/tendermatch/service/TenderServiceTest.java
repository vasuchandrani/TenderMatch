package com.tendermatch.service;

import com.tendermatch.dto.tender.CreateTenderRequest;
import com.tendermatch.dto.tender.TenderResponse;
import com.tendermatch.dto.tender.VendorSubmissionResponse;
import com.tendermatch.exception.BadRequestException;
import com.tendermatch.model.Tender;
import com.tendermatch.model.User;
import com.tendermatch.model.VendorSubmission;
import com.tendermatch.model.enums.SubmissionStatus;
import com.tendermatch.model.enums.TenderStatus;
import com.tendermatch.repository.TenderRepository;
import com.tendermatch.repository.UserRepository;
import com.tendermatch.repository.VendorSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenderServiceTest {

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private VendorSubmissionRepository vendorSubmissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MinioStorageService storageService;

    @InjectMocks
    private TenderService tenderService;

    private User sampleUser;
    private final String userEmail = "buyer@gov.org";
    private final UUID tenderId = UUID.randomUUID();
    private Tender sampleTender;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .name("Gov Procurement")
                .email(userEmail)
                .build();

        sampleTender = Tender.builder()
                .id(tenderId)
                .user(sampleUser)
                .name("Laptops Tender 2026")
                .description("Procurement of 500 office laptops")
                .status(TenderStatus.CREATED)
                .vendorSubmissions(new ArrayList<>())
                .createdAt(ZonedDateTime.now())
                .build();
    }

    @Test
    void shouldCreateTenderSession() {
        CreateTenderRequest request = new CreateTenderRequest("Laptops Tender 2026", "Procurement of 500 office laptops");

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(sampleUser));
        when(tenderRepository.save(any(Tender.class))).thenReturn(sampleTender);

        TenderResponse response = tenderService.createTender(request, userEmail);

        assertNotNull(response);
        assertEquals(tenderId, response.getId());
        assertEquals("Laptops Tender 2026", response.getName());
        assertEquals(TenderStatus.CREATED, response.getStatus());
        verify(tenderRepository).save(any(Tender.class));
    }

    @Test
    void shouldUploadRequirementPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "requirement.pdf",
                "application/pdf",
                "dummy pdf content".getBytes()
        );

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(sampleUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(tenderId, sampleUser))
                .thenReturn(Optional.of(sampleTender));
        when(storageService.uploadFile(any(), eq(file))).thenReturn("requirements/" + tenderId + "/requirement.pdf");
        when(tenderRepository.save(any(Tender.class))).thenAnswer(inv -> inv.getArgument(0));

        TenderResponse response = tenderService.uploadRequirementPdf(tenderId, file, userEmail);

        assertNotNull(response);
        assertEquals(TenderStatus.REQUIREMENT_UPLOADED, response.getStatus());
        assertEquals("requirement.pdf", response.getRequirementFileName());
        verify(storageService).uploadFile(any(), eq(file));
        verify(tenderRepository).save(sampleTender);
    }

    @Test
    void shouldRejectNonPdfFile() {
        MockMultipartFile wordFile = new MockMultipartFile(
                "file",
                "specs.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "word content".getBytes()
        );

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(sampleUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(tenderId, sampleUser))
                .thenReturn(Optional.of(sampleTender));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                tenderService.uploadRequirementPdf(tenderId, wordFile, userEmail)
        );
        assertTrue(ex.getMessage().contains("Only PDF files are supported"));
        verify(storageService, never()).uploadFile(any(), any());
    }

    @Test
    void shouldRejectOversizedFile() {
        byte[] largeBytes = new byte[100]; // simulate size via overridden mock
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "huge.pdf",
                "application/pdf",
                largeBytes
        ) {
            @Override
            public long getSize() {
                return 51L * 1024 * 1024; // 51MB
            }
        };

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(sampleUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(tenderId, sampleUser))
                .thenReturn(Optional.of(sampleTender));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                tenderService.uploadRequirementPdf(tenderId, largeFile, userEmail)
        );
        assertTrue(ex.getMessage().contains("exceeds 50MB limit"));
        verify(storageService, never()).uploadFile(any(), any());
    }

    @Test
    void shouldUploadMultipleVendorPdfs() {
        MockMultipartFile vendor1 = new MockMultipartFile("files", "Dell_Proposal.pdf", "application/pdf", "pdf1".getBytes());
        MockMultipartFile vendor2 = new MockMultipartFile("files", "HP_Bid.pdf", "application/pdf", "pdf2".getBytes());

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(sampleUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(tenderId, sampleUser))
                .thenReturn(Optional.of(sampleTender));
        when(vendorSubmissionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<VendorSubmissionResponse> results = tenderService.uploadVendorPdfs(
                tenderId,
                List.of(vendor1, vendor2),
                userEmail
        );

        assertEquals(2, results.size());
        assertEquals("Dell Proposal", results.get(0).getVendorName());
        assertEquals("Dell_Proposal.pdf", results.get(0).getFileName());
        assertEquals("HP Bid", results.get(1).getVendorName());
        assertEquals("HP_Bid.pdf", results.get(1).getFileName());
        verify(storageService, times(2)).uploadFile(any(), any());
        verify(vendorSubmissionRepository).saveAll(any());
    }

    @Test
    void shouldListTendersForUser() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tender> tenderPage = new PageImpl<>(List.of(sampleTender), pageable, 1);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(sampleUser));
        when(tenderRepository.findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(sampleUser, pageable))
                .thenReturn(tenderPage);

        Page<TenderResponse> result = tenderService.listTenders(userEmail, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Laptops Tender 2026", result.getContent().get(0).getName());
    }

    @Test
    void shouldSoftDeleteTender() {
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(sampleUser));
        when(tenderRepository.findByIdAndUserAndDeletedAtIsNull(tenderId, sampleUser))
                .thenReturn(Optional.of(sampleTender));

        tenderService.deleteTender(tenderId, userEmail);

        assertNotNull(sampleTender.getDeletedAt());
        verify(tenderRepository).save(sampleTender);
    }
}
