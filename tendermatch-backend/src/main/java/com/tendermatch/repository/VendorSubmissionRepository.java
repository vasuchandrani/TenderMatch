package com.tendermatch.repository;

import com.tendermatch.model.Tender;
import com.tendermatch.model.VendorSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorSubmissionRepository extends JpaRepository<VendorSubmission, UUID> {
    List<VendorSubmission> findByTenderOrderByCreatedAtAsc(Tender tender);

    List<VendorSubmission> findByTender(Tender tender);

    java.util.Optional<VendorSubmission> findByIdAndTender(UUID id, Tender tender);
}
