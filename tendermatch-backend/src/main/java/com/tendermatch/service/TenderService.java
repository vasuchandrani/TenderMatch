package com.tendermatch.service;

import com.tendermatch.dto.tender.CreateTenderRequest;
import com.tendermatch.dto.tender.TenderResponse;
import com.tendermatch.dto.tender.VendorSubmissionResponse;
import com.tendermatch.exception.BadRequestException;
import com.tendermatch.exception.ResourceNotFoundException;
import com.tendermatch.model.Tender;
import com.tendermatch.model.User;
import com.tendermatch.model.VendorSubmission;
import com.tendermatch.model.enums.SubmissionStatus;
import com.tendermatch.model.enums.TenderStatus;
import com.tendermatch.repository.TenderRepository;
import com.tendermatch.repository.UserRepository;
import com.tendermatch.repository.VendorSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TenderService {

    private static final Logger log = LoggerFactory.getLogger(TenderService.class);

    private final TenderRepository tenderRepository;
    private final VendorSubmissionRepository vendorSubmissionRepository;
    private final UserRepository userRepository;
    private final MinioStorageService storageService;

    public TenderService(TenderRepository tenderRepository,
                         VendorSubmissionRepository vendorSubmissionRepository,
                         UserRepository userRepository,
                         MinioStorageService storageService) {
        this.tenderRepository = tenderRepository;
        this.vendorSubmissionRepository = vendorSubmissionRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional
    public TenderResponse createTender(CreateTenderRequest request, String userEmail) {
        User user = getUser(userEmail);

        Tender tender = Tender.builder()
                .user(user)
                .name(request.getName().trim())
                .description(request.getDescription())
                .status(TenderStatus.CREATED)
                .build();

        tender = tenderRepository.save(tender);
        return TenderResponse.fromEntity(tender);
    }

    @Transactional
    public TenderResponse uploadRequirementPdf(UUID tenderId, MultipartFile file, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);

        validatePdfFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "requirement.pdf";
        String objectKey = String.format("requirements/%s/%s", tenderId, sanitizeFilename(originalFilename));

        storageService.uploadFile(objectKey, file);

        tender.setRequirementFileKey(objectKey);
        tender.setRequirementFileName(originalFilename);
        tender.setRequirementFileSize(file.getSize());
        tender.setStatus(TenderStatus.REQUIREMENT_UPLOADED);

        tender = tenderRepository.save(tender);
        return TenderResponse.fromEntity(tender);
    }

    @Transactional
    public List<VendorSubmissionResponse> uploadVendorPdfs(UUID tenderId, List<MultipartFile> files, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("At least one vendor PDF file must be provided");
        }

        List<VendorSubmission> submissions = new ArrayList<>();

        for (MultipartFile file : files) {
            validatePdfFile(file);

            UUID submissionId = UUID.randomUUID();
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "vendor_offer.pdf";
            String objectKey = String.format("vendors/%s/%s/%s", tenderId, submissionId, sanitizeFilename(originalFilename));

            storageService.uploadFile(objectKey, file);

            String inferredVendorName = cleanVendorNameFromFilename(originalFilename);

            VendorSubmission submission = VendorSubmission.builder()
                    .id(submissionId)
                    .tender(tender)
                    .vendorName(inferredVendorName)
                    .fileName(originalFilename)
                    .fileKey(objectKey)
                    .fileSize(file.getSize())
                    .status(SubmissionStatus.UPLOADED)
                    .build();

            submissions.add(submission);
        }

        submissions = vendorSubmissionRepository.saveAll(submissions);
        return submissions.stream().map(VendorSubmissionResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TenderResponse getTenderById(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = tenderRepository.findByIdWithSubmissions(tenderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id: " + tenderId));
        return TenderResponse.fromEntity(tender);
    }

    @Transactional(readOnly = true)
    public Page<TenderResponse> listTenders(String userEmail, Pageable pageable) {
        User user = getUser(userEmail);
        return tenderRepository.findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user, pageable)
                .map(TenderResponse::fromEntity);
    }

    @Transactional
    public void deleteTender(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = getTender(tenderId, user);
        tender.setDeletedAt(ZonedDateTime.now());
        tenderRepository.save(tender);
    }

    @Transactional
    public TenderResponse triggerMatch(UUID tenderId, String userEmail) {
        User user = getUser(userEmail);
        Tender tender = tenderRepository.findByIdWithSubmissions(tenderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id: " + tenderId));

        if (tender.getRequirementFileKey() == null) {
            throw new BadRequestException("Cannot match without a requirement PDF uploaded");
        }

        if (tender.getVendorSubmissions() == null || tender.getVendorSubmissions().isEmpty()) {
            throw new BadRequestException("Cannot match without at least one vendor PDF uploaded");
        }

        tender.setStatus(TenderStatus.PROCESSING);
        for (VendorSubmission sub : tender.getVendorSubmissions()) {
            sub.setStatus(SubmissionStatus.PROCESSING);
        }

        tender = tenderRepository.save(tender);
        return TenderResponse.fromEntity(tender);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private Tender getTender(UUID tenderId, User user) {
        return tenderRepository.findByIdAndUserAndDeletedAtIsNull(tenderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Tender not found with id: " + tenderId));
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file cannot be empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are supported");
        }
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new BadRequestException("File size exceeds 50MB limit");
        }
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String cleanVendorNameFromFilename(String filename) {
        String base = filename.replaceFirst("(?i)\\.pdf$", "");
        base = base.replaceAll("[_-]", " ").trim();
        return base.isEmpty() ? "Vendor" : base;
    }
}
