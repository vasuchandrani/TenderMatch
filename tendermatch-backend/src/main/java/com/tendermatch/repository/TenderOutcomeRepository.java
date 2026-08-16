package com.tendermatch.repository;

import com.tendermatch.model.TenderOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenderOutcomeRepository extends JpaRepository<TenderOutcome, UUID> {

    Optional<TenderOutcome> findByTenderId(UUID tenderId);

    @Modifying
    @Transactional
    void deleteByTenderId(UUID tenderId);
}
