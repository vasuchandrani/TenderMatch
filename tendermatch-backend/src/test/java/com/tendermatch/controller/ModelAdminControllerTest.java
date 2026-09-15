package com.tendermatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tendermatch.dto.admin.*;
import com.tendermatch.security.CustomUserDetailsService;
import com.tendermatch.security.JwtService;
import com.tendermatch.service.ModelAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModelAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class ModelAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModelAdminService modelAdminService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void listModels_returnsOkWithList() throws Exception {
        ModelSummaryDto summary = new ModelSummaryDto(
                "confidence_model", 1, "shadow", "GradientBoostingClassifier",
                "cv_auc_mean", 0.824, Map.of("cv_auc_mean", 0.824), 600,
                "2026-03-01T10:00:00Z", "models/v1.joblib"
        );
        when(modelAdminService.listModels()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/admin/models")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].modelName").value("confidence_model"))
                .andExpect(jsonPath("$.data[0].status").value("shadow"));
    }

    @Test
    void getTrainingDataStats_returnsOkWithCounts() throws Exception {
        TrainingDataStatsResponse stats = new TrainingDataStatsResponse(
                550, 75, 300, 1000, 180, true, true, false
        );
        when(modelAdminService.getTrainingDataStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/training-data-stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCorrectionLogs").value(550))
                .andExpect(jsonPath("$.data.confidenceModelReady").value(true))
                .andExpect(jsonPath("$.data.rankingModelReady").value(false));
    }

    @Test
    void promoteModel_returnsOk() throws Exception {
        ModelVersionDto promoted = new ModelVersionDto(
                "confidence_model", 2, "promoted", "GradientBoostingClassifier",
                "cv_auc_mean", 0.850, Map.of("cv_auc_mean", 0.850), 700,
                "2026-03-01T10:00:00Z", "2026-03-02T10:00:00Z", null, "models/v2.joblib"
        );
        when(modelAdminService.promoteModel("confidence_model", 2)).thenReturn(promoted);

        mockMvc.perform(post("/api/admin/models/confidence_model/2/promote")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("promoted"));
    }

    @Test
    void rollbackModel_returnsOk() throws Exception {
        ModelVersionDto rolledBack = new ModelVersionDto(
                "confidence_model", 2, "shadow", "GradientBoostingClassifier",
                "cv_auc_mean", 0.850, Map.of("cv_auc_mean", 0.850), 700,
                "2026-03-01T10:00:00Z", null, "2026-03-03T10:00:00Z", "models/v2.joblib"
        );
        when(modelAdminService.rollbackModel("confidence_model", 2)).thenReturn(rolledBack);

        mockMvc.perform(post("/api/admin/models/confidence_model/2/rollback")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("shadow"));
    }

    @Test
    void getShadowStats_returnsOk() throws Exception {
        ShadowStatsDto stats = new ShadowStatsDto(
                "confidence_model", 100, 92, 8, 0.92, List.of()
        );
        when(modelAdminService.getShadowStats("confidence_model")).thenReturn(stats);

        mockMvc.perform(get("/api/admin/models/confidence_model/shadow-stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.agreementRate").value(0.92));
    }

    @Test
    void triggerRetrain_returnsOk() throws Exception {
        ModelVersionDto trained = new ModelVersionDto(
                "operator_model", 2, "trained", "DecisionTreeClassifier",
                "cv_accuracy_mean", 0.930, Map.of("cv_accuracy_mean", 0.930), 600,
                "2026-03-02T10:00:00Z", null, null, "models/operator_model/v2.joblib"
        );
        when(modelAdminService.triggerRetrain("operator_model")).thenReturn(trained);

        mockMvc.perform(post("/api/admin/retrain/operator_model")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.version").value(2));
    }
}
