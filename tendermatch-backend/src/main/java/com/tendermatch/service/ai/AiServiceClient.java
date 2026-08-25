package com.tendermatch.service.ai;

import com.tendermatch.dto.pipeline.ProcessDocumentAiRequest;
import com.tendermatch.dto.pipeline.ProcessDocumentAiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public AiServiceClient(RestTemplateBuilder builder,
                           @Value("${app.ai-service.url:http://localhost:8000}") String aiServiceUrl) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(180))
                .build();
        this.aiServiceUrl = aiServiceUrl;
    }

    public ProcessDocumentAiResponse processDocument(ProcessDocumentAiRequest request) {
        String endpoint = aiServiceUrl + "/api/v1/process-document";
        log.info("Calling AI service at: {} for fileKey: {}", endpoint, request.getMinioFileKey());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProcessDocumentAiRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ProcessDocumentAiResponse> response = restTemplate.postForEntity(
                    endpoint,
                    entity,
                    ProcessDocumentAiResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("AI service call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI service call failed: " + e.getMessage(), e);
        }
    }

    public com.tendermatch.dto.matching.MatchSpecsAiResponse matchSpecs(com.tendermatch.dto.matching.MatchSpecsAiRequest request) {
        String endpoint = aiServiceUrl + "/api/v1/match-specs";
        log.info("Calling AI service match-specs at: {} with {} pairs", endpoint, request.getSpecPairs().size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<com.tendermatch.dto.matching.MatchSpecsAiRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<com.tendermatch.dto.matching.MatchSpecsAiResponse> response = restTemplate.postForEntity(
                    endpoint,
                    entity,
                    com.tendermatch.dto.matching.MatchSpecsAiResponse.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("AI service match-specs call failed: {}", e.getMessage(), e);
            throw new RuntimeException("AI service match-specs call failed: " + e.getMessage(), e);
        }
    }

    public com.tendermatch.dto.admin.ModelSummaryDto[] listModels() {
        String endpoint = aiServiceUrl + "/api/v1/models";
        try {
            return restTemplate.getForObject(endpoint, com.tendermatch.dto.admin.ModelSummaryDto[].class);
        } catch (Exception e) {
            log.warn("Failed to fetch models from AI service: {}", e.getMessage());
            return new com.tendermatch.dto.admin.ModelSummaryDto[0];
        }
    }

    public com.tendermatch.dto.admin.ModelVersionDto[] getModelVersions(String modelName) {
        String endpoint = aiServiceUrl + "/api/v1/models/" + modelName + "/versions";
        try {
            return restTemplate.getForObject(endpoint, com.tendermatch.dto.admin.ModelVersionDto[].class);
        } catch (Exception e) {
            log.warn("Failed to fetch model versions from AI service: {}", e.getMessage());
            return new com.tendermatch.dto.admin.ModelVersionDto[0];
        }
    }

    public com.tendermatch.dto.admin.ModelVersionDto promoteModel(String modelName, Integer version) {
        String endpoint = aiServiceUrl + "/api/v1/models/" + modelName + "/" + version + "/promote";
        try {
            return restTemplate.postForObject(endpoint, null, com.tendermatch.dto.admin.ModelVersionDto.class);
        } catch (Exception e) {
            log.error("Failed to promote model in AI service: {}", e.getMessage());
            throw new RuntimeException("Model promotion failed: " + e.getMessage(), e);
        }
    }

    public com.tendermatch.dto.admin.ModelVersionDto rollbackModel(String modelName, Integer version) {
        String endpoint = aiServiceUrl + "/api/v1/models/" + modelName + "/" + version + "/rollback";
        try {
            return restTemplate.postForObject(endpoint, null, com.tendermatch.dto.admin.ModelVersionDto.class);
        } catch (Exception e) {
            log.error("Failed to rollback model in AI service: {}", e.getMessage());
            throw new RuntimeException("Model rollback failed: " + e.getMessage(), e);
        }
    }

    public com.tendermatch.dto.admin.ShadowStatsDto getShadowStats(String modelName) {
        String endpoint = aiServiceUrl + "/api/v1/models/" + modelName + "/shadow-stats";
        try {
            return restTemplate.getForObject(endpoint, com.tendermatch.dto.admin.ShadowStatsDto.class);
        } catch (Exception e) {
            log.warn("Failed to fetch shadow stats from AI service: {}", e.getMessage());
            return null;
        }
    }

    public com.tendermatch.dto.admin.RetrainCheckDto[] checkRetrain() {
        String endpoint = aiServiceUrl + "/api/v1/training/retrain-check";
        try {
            return restTemplate.getForObject(endpoint, com.tendermatch.dto.admin.RetrainCheckDto[].class);
        } catch (Exception e) {
            log.warn("Failed to check retraining conditions: {}", e.getMessage());
            return new com.tendermatch.dto.admin.RetrainCheckDto[0];
        }
    }

    public com.tendermatch.dto.admin.ModelVersionDto triggerTrain(String modelName) {
        String endpoint = aiServiceUrl + "/api/v1/training/train/" + modelName;
        try {
            return restTemplate.postForObject(endpoint, null, com.tendermatch.dto.admin.ModelVersionDto.class);
        } catch (Exception e) {
            log.error("Failed to trigger model training in AI service: {}", e.getMessage());
            throw new RuntimeException("Model training failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> reverifyWithReflection(java.util.Map<String, Object> request) {
        String endpoint = aiServiceUrl + "/api/v1/two-pass/reverify-scores";
        log.info("Calling AI service Two-Pass reflection at: {}", endpoint);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<java.util.Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<java.util.Map> response = restTemplate.postForEntity(endpoint, entity, java.util.Map.class);
            return (java.util.Map<String, Object>) response.getBody();
        } catch (Exception e) {
            log.error("Failed to run Two-Pass reflection reverification: {}", e.getMessage(), e);
            throw new RuntimeException("Two-Pass reflection reverification failed: " + e.getMessage(), e);
        }
    }
}

