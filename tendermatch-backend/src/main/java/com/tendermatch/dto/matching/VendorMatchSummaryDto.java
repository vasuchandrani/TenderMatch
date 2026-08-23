package com.tendermatch.dto.matching;

import java.util.UUID;

public class VendorMatchSummaryDto {

    private UUID submissionId;
    private String vendorName;
    private String fileName;
    private long matchCount;
    private long unmatchCount;
    private long aiErrCount;
    private long totalSpecs;
    private double matchPercentage;

    public VendorMatchSummaryDto() {}

    public VendorMatchSummaryDto(UUID submissionId, String vendorName, String fileName,
                                 long matchCount, long unmatchCount, long aiErrCount) {
        this.submissionId = submissionId;
        this.vendorName = vendorName;
        this.fileName = fileName;
        this.matchCount = matchCount;
        this.unmatchCount = unmatchCount;
        this.aiErrCount = aiErrCount;
        this.totalSpecs = matchCount + unmatchCount + aiErrCount;
        this.matchPercentage = this.totalSpecs > 0
                ? Math.round(((double) matchCount / this.totalSpecs) * 1000.0) / 10.0
                : 0.0;
    }

    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getMatchCount() { return matchCount; }
    public void setMatchCount(long matchCount) { this.matchCount = matchCount; }

    public long getUnmatchCount() { return unmatchCount; }
    public void setUnmatchCount(long unmatchCount) { this.unmatchCount = unmatchCount; }

    public long getAiErrCount() { return aiErrCount; }
    public void setAiErrCount(long aiErrCount) { this.aiErrCount = aiErrCount; }

    public long getTotalSpecs() { return totalSpecs; }
    public void setTotalSpecs(long totalSpecs) { this.totalSpecs = totalSpecs; }

    public double getMatchPercentage() { return matchPercentage; }
    public void setMatchPercentage(double matchPercentage) { this.matchPercentage = matchPercentage; }
}
