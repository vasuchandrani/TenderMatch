package com.tendermatch.repository;

import com.tendermatch.model.ScoringWeight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScoringWeightRepository extends JpaRepository<ScoringWeight, UUID> {

    List<ScoringWeight> findByTenderId(UUID tenderId);

    Optional<ScoringWeight> findByTenderIdAndCanonicalSpecName(UUID tenderId, String canonicalSpecName);

    List<ScoringWeight> findByTemplateName(String templateName);

    @Query("SELECT DISTINCT s.templateName FROM ScoringWeight s WHERE s.templateName IS NOT NULL ORDER BY s.templateName ASC")
    List<String> findDistinctTemplateNames();

    @Modifying
    @Transactional
    void deleteByTenderId(UUID tenderId);

    @Modifying
    @Transactional
    void deleteByTemplateName(String templateName);
}
