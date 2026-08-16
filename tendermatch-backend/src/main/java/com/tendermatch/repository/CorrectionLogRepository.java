package com.tendermatch.repository;

import com.tendermatch.model.CorrectionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CorrectionLogRepository extends JpaRepository<CorrectionLog, UUID> {

    List<CorrectionLog> findByTenderIdOrderByCreatedAtDesc(UUID tenderId);

    List<CorrectionLog> findByMatchResultIdOrderByCreatedAtDesc(UUID matchResultId);

    long countByTenderId(UUID tenderId);

    long countByTenderIdAndIsOverrideTrue(UUID tenderId);

    long countByIsOverrideTrue();

    long countByIsAutoConfirmedTrue();
}
