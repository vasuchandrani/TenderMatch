package com.tendermatch.controller;

import com.tendermatch.dto.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of(
                        "status", "UP",
                        "service", "tendermatch-backend",
                        "timestamp", java.time.Instant.now().toString()
                ),
                "Backend service is healthy"
        ));
    }
}
