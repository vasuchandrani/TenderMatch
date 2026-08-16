package com.tendermatch.repository;

import com.tendermatch.model.ShadowPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShadowPredictionRepository extends JpaRepository<ShadowPrediction, UUID> {

    List<ShadowPrediction> findByModelNameOrderByCreatedAtDesc(String modelName);

    long countByModelName(String modelName);

    long countByModelNameAndAgreedTrue(String modelName);

    long countByModelNameAndAgreedFalse(String modelName);

    List<ShadowPrediction> findTop20ByModelNameAndAgreedFalseOrderByCreatedAtDesc(String modelName);
}
