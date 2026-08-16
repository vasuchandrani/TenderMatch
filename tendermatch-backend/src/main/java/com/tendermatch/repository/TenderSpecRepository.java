package com.tendermatch.repository;

import com.tendermatch.model.TenderProduct;
import com.tendermatch.model.TenderSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenderSpecRepository extends JpaRepository<TenderSpec, UUID> {

    List<TenderSpec> findByTenderProductOrderByDisplayOrderAsc(TenderProduct tenderProduct);

    @Query("SELECT s FROM TenderSpec s WHERE s.tenderProduct.tender.id = :tenderId")
    List<TenderSpec> findByTenderId(@Param("tenderId") UUID tenderId);
}
