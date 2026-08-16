package com.tendermatch.repository;

import com.tendermatch.model.VendorScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorScoreRepository extends JpaRepository<VendorScore, UUID> {

    List<VendorScore> findByTenderIdOrderByRankAsc(UUID tenderId);

    Optional<VendorScore> findByTenderIdAndVendorSubmissionId(UUID tenderId, UUID vendorSubmissionId);

    @Modifying
    @Transactional
    void deleteByTenderId(UUID tenderId);

    long countByTenderId(UUID tenderId);
}
