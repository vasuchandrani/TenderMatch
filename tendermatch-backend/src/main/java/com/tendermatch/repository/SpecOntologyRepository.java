package com.tendermatch.repository;

import com.tendermatch.model.SpecOntology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpecOntologyRepository extends JpaRepository<SpecOntology, UUID> {
    Optional<SpecOntology> findByCanonicalName(String canonicalName);

    Optional<SpecOntology> findByCanonicalNameIgnoreCase(String canonicalName);

    List<SpecOntology> findByCategoryOrderByCanonicalNameAsc(String category);

    List<SpecOntology> findAllByOrderByCanonicalNameAsc();

    @Query("SELECT DISTINCT s.category FROM SpecOntology s WHERE s.category IS NOT NULL ORDER BY s.category ASC")
    List<String> findDistinctCategories();
}
