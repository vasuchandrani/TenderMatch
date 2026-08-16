package com.tendermatch.repository;

import com.tendermatch.model.MatchResult;
import com.tendermatch.model.Tender;
import com.tendermatch.model.VendorSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, UUID> {

    List<MatchResult> findByTender(Tender tender);

    List<MatchResult> findByVendorSubmission(VendorSubmission vendorSubmission);

    List<MatchResult> findByTenderAndVendorSubmission(Tender tender, VendorSubmission vendorSubmission);

    @Query("SELECT mr FROM MatchResult mr " +
            "LEFT JOIN FETCH mr.tenderSpec " +
            "LEFT JOIN FETCH mr.vendorSpec " +
            "LEFT JOIN FETCH mr.tenderProduct " +
            "LEFT JOIN FETCH mr.vendorProduct " +
            "WHERE mr.tender = :tender AND mr.vendorSubmission = :submission")
    List<MatchResult> findByTenderAndVendorSubmissionWithDetails(
            @Param("tender") Tender tender,
            @Param("submission") VendorSubmission submission);

    @Query("SELECT mr FROM MatchResult mr " +
            "LEFT JOIN FETCH mr.tenderSpec " +
            "LEFT JOIN FETCH mr.vendorSpec " +
            "WHERE mr.vendorSubmission = :submission")
    List<MatchResult> findByVendorSubmissionWithSpecs(@Param("submission") VendorSubmission submission);

    @Query("SELECT mr FROM MatchResult mr " +
            "LEFT JOIN FETCH mr.tenderSpec " +
            "LEFT JOIN FETCH mr.vendorSpec " +
            "WHERE mr.tender = :tender")
    List<MatchResult> findByTenderWithSpecs(@Param("tender") Tender tender);

    long countByVendorSubmissionAndSystemVerdict(VendorSubmission vendorSubmission, String systemVerdict);

    @Modifying
    @Transactional
    void deleteByTender(Tender tender);

    @Modifying
    @Transactional
    void deleteByVendorSubmission(VendorSubmission vendorSubmission);
}
