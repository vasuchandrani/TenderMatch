package com.tendermatch.dto.pipeline;

import com.tendermatch.model.enums.SubmissionStatus;
import com.tendermatch.model.enums.TenderStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcessingStatusResponse {

    private UUID tenderId;
    private TenderStatus tenderStatus;
    private String tenderStage = "IDLE";
    private String tenderErrorMessage;
    private String requirementFileName;
    private String requirementFileKey;
    private String requirementStatus;
    private int totalDocuments;
    private int completedDocuments;
    private List<VendorDocumentStatus> vendorDocuments = new ArrayList<>();

    public ProcessingStatusResponse() {}

    public ProcessingStatusResponse(UUID tenderId, TenderStatus tenderStatus, String requirementFileName,
                                    String requirementFileKey, String requirementStatus,
                                    int totalDocuments, int completedDocuments,
                                    List<VendorDocumentStatus> vendorDocuments) {
        this(tenderId, tenderStatus, "IDLE", null, requirementFileName, requirementFileKey,
                requirementStatus, totalDocuments, completedDocuments, vendorDocuments);
    }

    public ProcessingStatusResponse(UUID tenderId, TenderStatus tenderStatus, String tenderStage,
                                    String tenderErrorMessage, String requirementFileName,
                                    String requirementFileKey, String requirementStatus,
                                    int totalDocuments, int completedDocuments,
                                    List<VendorDocumentStatus> vendorDocuments) {
        this.tenderId = tenderId;
        this.tenderStatus = tenderStatus;
        this.tenderStage = tenderStage != null ? tenderStage : "IDLE";
        this.tenderErrorMessage = tenderErrorMessage;
        this.requirementFileName = requirementFileName;
        this.requirementFileKey = requirementFileKey;
        this.requirementStatus = requirementStatus;
        this.totalDocuments = totalDocuments;
        this.completedDocuments = completedDocuments;
        this.vendorDocuments = vendorDocuments != null ? vendorDocuments : new ArrayList<>();
    }

    public UUID getTenderId() { return tenderId; }
    public void setTenderId(UUID tenderId) { this.tenderId = tenderId; }

    public TenderStatus getTenderStatus() { return tenderStatus; }
    public void setTenderStatus(TenderStatus tenderStatus) { this.tenderStatus = tenderStatus; }

    public String getTenderStage() { return tenderStage; }
    public void setTenderStage(String tenderStage) { this.tenderStage = tenderStage; }

    public String getTenderErrorMessage() { return tenderErrorMessage; }
    public void setTenderErrorMessage(String tenderErrorMessage) { this.tenderErrorMessage = tenderErrorMessage; }

    public String getRequirementFileName() { return requirementFileName; }
    public void setRequirementFileName(String requirementFileName) { this.requirementFileName = requirementFileName; }

    public String getRequirementFileKey() { return requirementFileKey; }
    public void setRequirementFileKey(String requirementFileKey) { this.requirementFileKey = requirementFileKey; }

    public String getRequirementStatus() { return requirementStatus; }
    public void setRequirementStatus(String requirementStatus) { this.requirementStatus = requirementStatus; }

    public int getTotalDocuments() { return totalDocuments; }
    public void setTotalDocuments(int totalDocuments) { this.totalDocuments = totalDocuments; }

    public int getCompletedDocuments() { return completedDocuments; }
    public void setCompletedDocuments(int completedDocuments) { this.completedDocuments = completedDocuments; }

    public List<VendorDocumentStatus> getVendorDocuments() { return vendorDocuments; }
    public void setVendorDocuments(List<VendorDocumentStatus> vendorDocuments) { this.vendorDocuments = vendorDocuments; }

    public static class VendorDocumentStatus {
        private UUID submissionId;
        private String vendorName;
        private String fileName;
        private SubmissionStatus status;
        private String processingStage = "QUEUED";
        private String errorMessage;
        private int productCount;

        public VendorDocumentStatus() {}

        public VendorDocumentStatus(UUID submissionId, String vendorName, String fileName,
                                    SubmissionStatus status, int productCount) {
            this(submissionId, vendorName, fileName, status, "QUEUED", null, productCount);
        }

        public VendorDocumentStatus(UUID submissionId, String vendorName, String fileName,
                                    SubmissionStatus status, String processingStage,
                                    String errorMessage, int productCount) {
            this.submissionId = submissionId;
            this.vendorName = vendorName;
            this.fileName = fileName;
            this.status = status;
            this.processingStage = processingStage != null ? processingStage : "QUEUED";
            this.errorMessage = errorMessage;
            this.productCount = productCount;
        }

        public UUID getSubmissionId() { return submissionId; }
        public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }

        public String getVendorName() { return vendorName; }
        public void setVendorName(String vendorName) { this.vendorName = vendorName; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public SubmissionStatus getStatus() { return status; }
        public void setStatus(SubmissionStatus status) { this.status = status; }

        public String getProcessingStage() { return processingStage; }
        public void setProcessingStage(String processingStage) { this.processingStage = processingStage; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public int getProductCount() { return productCount; }
        public void setProductCount(int productCount) { this.productCount = productCount; }
    }
}
