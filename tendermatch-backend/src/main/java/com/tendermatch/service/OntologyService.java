package com.tendermatch.service;

import com.tendermatch.dto.ontology.SpecOntologyDto;
import com.tendermatch.exception.ResourceNotFoundException;
import com.tendermatch.model.SpecOntology;
import com.tendermatch.repository.SpecOntologyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OntologyService {

    private final SpecOntologyRepository specOntologyRepository;

    public OntologyService(SpecOntologyRepository specOntologyRepository) {
        this.specOntologyRepository = specOntologyRepository;
    }

    public List<SpecOntologyDto> getOntologySpecs(String category) {
        List<SpecOntology> entities;
        if (category != null && !category.trim().isEmpty()) {
            entities = specOntologyRepository.findByCategoryOrderByCanonicalNameAsc(category.trim());
        } else {
            entities = specOntologyRepository.findAllByOrderByCanonicalNameAsc();
        }
        return entities.stream()
                .map(SpecOntologyDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<String> getCategories() {
        return specOntologyRepository.findDistinctCategories();
    }

    public SpecOntologyDto getSpecByCanonicalName(String canonicalName) {
        SpecOntology entity = specOntologyRepository.findByCanonicalNameIgnoreCase(canonicalName)
                .orElseThrow(() -> new ResourceNotFoundException("Specification '" + canonicalName + "' not found in ontology."));
        return SpecOntologyDto.fromEntity(entity);
    }
}
