package com.tendermatch.repository;

import com.tendermatch.model.Tender;
import com.tendermatch.model.VendorProduct;
import com.tendermatch.model.VendorSubmission;
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
public interface VendorProductRepository extends JpaRepository<VendorProduct, UUID> {
    List<VendorProduct> findByVendorSubmissionOrderByDisplayOrderAsc(VendorSubmission vendorSubmission);

    @Query("SELECT vp FROM VendorProduct vp LEFT JOIN FETCH vp.specifications WHERE vp.vendorSubmission = :submission ORDER BY vp.displayOrder ASC")
    List<VendorProduct> findByVendorSubmissionWithSpecs(@Param("submission") VendorSubmission submission);

    @Query("SELECT vp FROM VendorProduct vp LEFT JOIN FETCH vp.specifications LEFT JOIN FETCH vp.vendorSubmission vs WHERE vs.tender = :tender ORDER BY vs.createdAt ASC, vp.displayOrder ASC")
    List<VendorProduct> findByTenderWithSpecs(@Param("tender") Tender tender);

    @Query("SELECT vp FROM VendorProduct vp LEFT JOIN FETCH vp.specifications WHERE vp.id = :id")
    Optional<VendorProduct> findByIdWithSpecs(@Param("id") UUID id);

    @Modifying
    @Transactional
    void deleteByVendorSubmission(VendorSubmission vendorSubmission);
}
