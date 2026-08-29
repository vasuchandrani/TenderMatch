package com.tendermatch.controller;

import com.tendermatch.dto.common.ApiResponse;
import com.tendermatch.dto.ontology.SpecOntologyDto;
import com.tendermatch.service.OntologyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ontology")
public class OntologyController {

    private final OntologyService ontologyService;

    public OntologyController(OntologyService ontologyService) {
        this.ontologyService = ontologyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SpecOntologyDto>>> getOntology(
            @RequestParam(required = false) String category) {
        List<SpecOntologyDto> specs = ontologyService.getOntologySpecs(category);
        return ResponseEntity.ok(ApiResponse.ok(specs));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = ontologyService.getCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @GetMapping("/{canonicalName}")
    public ResponseEntity<ApiResponse<SpecOntologyDto>> getSpecByCanonicalName(
            @PathVariable String canonicalName) {
        SpecOntologyDto spec = ontologyService.getSpecByCanonicalName(canonicalName);
        return ResponseEntity.ok(ApiResponse.ok(spec));
    }
}
