package com.tendermatch.repository;

import com.tendermatch.model.Tender;
import com.tendermatch.model.TenderProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenderProductRepository extends JpaRepository<TenderProduct, UUID> {
    List<TenderProduct> findByTenderOrderByDisplayOrderAsc(Tender tender);

    @Query("SELECT tp FROM TenderProduct tp LEFT JOIN FETCH tp.specifications WHERE tp.id = :id")
    Optional<TenderProduct> findByIdWithSpecs(@Param("id") UUID id);

    @Query("SELECT tp FROM TenderProduct tp LEFT JOIN FETCH tp.specifications WHERE tp.tender = :tender ORDER BY tp.displayOrder ASC")
    List<TenderProduct> findByTenderWithSpecs(@Param("tender") Tender tender);

    @Modifying
    @Transactional
    void deleteByTender(Tender tender);
}
