package com.tendermatch.repository;

import com.tendermatch.model.ModelRegistryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModelRegistryRepository extends JpaRepository<ModelRegistryEntry, UUID> {

    List<ModelRegistryEntry> findByModelNameOrderByVersionDesc(String modelName);

    Optional<ModelRegistryEntry> findByModelNameAndVersion(String modelName, Integer version);

    Optional<ModelRegistryEntry> findFirstByModelNameAndStatus(String modelName, String status);

    @Query("SELECT DISTINCT m.modelName FROM ModelRegistryEntry m")
    List<String> findDistinctModelNames();

    @Query("SELECT m FROM ModelRegistryEntry m WHERE m.version = " +
           "(SELECT MAX(m2.version) FROM ModelRegistryEntry m2 WHERE m2.modelName = m.modelName)")
    List<ModelRegistryEntry> findLatestVersionsForAllModels();
}
