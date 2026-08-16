package com.tendermatch.repository;

import com.tendermatch.model.VendorProduct;
import com.tendermatch.model.VendorSpec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorSpecRepository extends JpaRepository<VendorSpec, UUID> {
    List<VendorSpec> findByVendorProductOrderByDisplayOrderAsc(VendorProduct vendorProduct);
}
