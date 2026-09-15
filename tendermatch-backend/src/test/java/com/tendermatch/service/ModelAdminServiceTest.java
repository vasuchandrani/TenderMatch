package com.tendermatch.service;

import com.tendermatch.dto.admin.ModelSummaryDto;
import com.tendermatch.dto.admin.ModelVersionDto;
import com.tendermatch.dto.admin.ShadowStatsDto;
import com.tendermatch.dto.admin.TrainingDataStatsResponse;
import com.tendermatch.model.ModelRegistryEntry;
import com.tendermatch.model.ShadowPrediction;
import com.tendermatch.repository.CorrectionLogRepository;
import com.tendermatch.repository.MatchResultRepository;
import com.tendermatch.repository.ModelRegistryRepository;
import com.tendermatch.repository.ShadowPredictionRepository;
import com.tendermatch.repository.TenderOutcomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.tendermatch.service.ai.AiServiceClient;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelAdminServiceTest {

    @Mock
    private ModelRegistryRepository modelRegistryRepository;

    @Mock
    private ShadowPredictionRepository shadowPredictionRepository;

    @Mock
    private CorrectionLogRepository correctionLogRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private TenderOutcomeRepository tenderOutcomeRepository;

    @Mock
    private AiServiceClient aiServiceClient;

    private ModelAdminService modelAdminService;

    @BeforeEach
    void setUp() {
        modelAdminService = new ModelAdminService(
                modelRegistryRepository,
                shadowPredictionRepository,
                correctionLogRepository,
                matchResultRepository,
                tenderOutcomeRepository,
                aiServiceClient
        );
    }

    @Test
    void getTrainingDataStats_computesCorrectCountsAndReadiness() {
        when(correctionLogRepository.count()).thenReturn(650L);
        when(correctionLogRepository.countByIsOverrideTrue()).thenReturn(85L);
        when(correctionLogRepository.countByIsAutoConfirmedTrue()).thenReturn(400L);
        when(matchResultRepository.count()).thenReturn(1200L);
        when(tenderOutcomeRepository.count()).thenReturn(210L);

        TrainingDataStatsResponse stats = modelAdminService.getTrainingDataStats();

        assertThat(stats.totalCorrectionLogs()).isEqualTo(650L);
        assertThat(stats.totalOverrides()).isEqualTo(85L);
        assertThat(stats.totalAutoConfirmed()).isEqualTo(400L);
        assertThat(stats.totalMatchResults()).isEqualTo(1200L);
        assertThat(stats.totalTenderOutcomes()).isEqualTo(210L);
        assertThat(stats.confidenceModelReady()).isTrue();
        assertThat(stats.operatorModelReady()).isTrue();
        assertThat(stats.rankingModelReady()).isTrue();
    }

    @Test
    void listModels_returnsBaselineModelsWhenRepositoryEmpty() {
        when(modelRegistryRepository.findLatestVersionsForAllModels()).thenReturn(Collections.emptyList());

        List<ModelSummaryDto> models = modelAdminService.listModels();

        assertThat(models).isNotEmpty();
        assertThat(models).extracting(ModelSummaryDto::modelName)
                .contains("confidence_model", "operator_model", "doctype_model", "normalization_model", "ranking_model");
    }

    @Test
    void promoteModel_marksTargetPromotedAndRetiresOthers() {
        ModelRegistryEntry v1 = ModelRegistryEntry.builder()
                .id(UUID.randomUUID())
                .modelName("confidence_model")
                .version(1)
                .status("promoted")
                .trainingDataCount(500)
                .trainedAt(ZonedDateTime.now())
                .evalMetrics("{}")
                .modelPath("models/v1.joblib")
                .build();

        ModelRegistryEntry v2 = ModelRegistryEntry.builder()
                .id(UUID.randomUUID())
                .modelName("confidence_model")
                .version(2)
                .status("shadow")
                .trainingDataCount(600)
                .trainedAt(ZonedDateTime.now())
                .evalMetrics("{}")
                .modelPath("models/v2.joblib")
                .build();

        when(modelRegistryRepository.findByModelNameOrderByVersionDesc("confidence_model"))
                .thenReturn(List.of(v2, v1));
        when(modelRegistryRepository.findByModelNameAndVersion("confidence_model", 2))
                .thenReturn(Optional.of(v2));

        ModelVersionDto promoted = modelAdminService.promoteModel("confidence_model", 2);

        assertThat(promoted.version()).isEqualTo(2);
        assertThat(promoted.status()).isEqualTo("promoted");
        assertThat(v1.getStatus()).isEqualTo("retired");
        assertThat(v2.getStatus()).isEqualTo("promoted");
        verify(modelRegistryRepository, atLeastOnce()).save(any(ModelRegistryEntry.class));
    }

    @Test
    void getShadowStats_computesAgreementRateFromPredictions() {
        when(shadowPredictionRepository.countByModelName("confidence_model")).thenReturn(100L);
        when(shadowPredictionRepository.countByModelNameAndAgreedTrue("confidence_model")).thenReturn(92L);
        when(shadowPredictionRepository.countByModelNameAndAgreedFalse("confidence_model")).thenReturn(8L);
        when(shadowPredictionRepository.findTop20ByModelNameAndAgreedFalseOrderByCreatedAtDesc("confidence_model"))
                .thenReturn(List.of(
                        ShadowPrediction.builder()
                                .inputHash("hash123")
                                .ruleResult("\"Match\"")
                                .mlResult("\"Unmatch\"")
                                .build()
                ));

        ShadowStatsDto stats = modelAdminService.getShadowStats("confidence_model");

        assertThat(stats.modelName()).isEqualTo("confidence_model");
        assertThat(stats.totalPredictions()).isEqualTo(100L);
        assertThat(stats.agreementCount()).isEqualTo(92L);
        assertThat(stats.disagreementCount()).isEqualTo(8L);
        assertThat(stats.agreementRate()).isEqualTo(0.92);
        assertThat(stats.disagreements()).hasSize(1);
    }
}
