package com.tendermatch.repository;

import com.tendermatch.model.Tender;
import com.tendermatch.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenderRepository extends JpaRepository<Tender, UUID> {
    Page<Tender> findByUserAndDeletedAtIsNullOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<Tender> findByIdAndUserAndDeletedAtIsNull(UUID id, User user);

    @Query("SELECT t FROM Tender t LEFT JOIN FETCH t.vendorSubmissions WHERE t.id = :id AND t.user = :user AND t.deletedAt IS NULL")
    Optional<Tender> findByIdWithSubmissions(UUID id, User user);
}
