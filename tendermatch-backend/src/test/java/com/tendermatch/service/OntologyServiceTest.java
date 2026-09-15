package com.tendermatch.service;

import com.tendermatch.dto.ontology.SpecOntologyDto;
import com.tendermatch.exception.ResourceNotFoundException;
import com.tendermatch.model.SpecOntology;
import com.tendermatch.repository.SpecOntologyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OntologyServiceTest {

    @Mock
    private SpecOntologyRepository specOntologyRepository;

    @InjectMocks
    private OntologyService ontologyService;

    private SpecOntology ramSpec;
    private SpecOntology storageSpec;

    @BeforeEach
    void setUp() {
        ramSpec = SpecOntology.builder()
                .id(UUID.randomUUID())
                .canonicalName("ram_capacity")
                .displayName("RAM Capacity")
                .category("memory")
                .defaultUnit("GB")
                .defaultOperator(">=")
                .usageCount(15)
                .humanVerified(true)
                .synonyms(new String[]{"RAM", "Memory"})
                .build();

        storageSpec = SpecOntology.builder()
                .id(UUID.randomUUID())
                .canonicalName("storage_capacity")
                .displayName("Storage Capacity")
                .category("storage")
                .defaultUnit("GB")
                .defaultOperator(">=")
                .usageCount(20)
                .humanVerified(true)
                .synonyms(new String[]{"Storage", "SSD"})
                .build();
    }

    @Test
    void shouldGetAllOntologySpecsWhenNoCategorySpecified() {
        when(specOntologyRepository.findAllByOrderByCanonicalNameAsc()).thenReturn(List.of(ramSpec, storageSpec));

        List<SpecOntologyDto> results = ontologyService.getOntologySpecs(null);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getCanonicalName()).isEqualTo("ram_capacity");
        assertThat(results.get(1).getCanonicalName()).isEqualTo("storage_capacity");
        verify(specOntologyRepository).findAllByOrderByCanonicalNameAsc();
    }

    @Test
    void shouldGetOntologySpecsFilteredByCategory() {
        when(specOntologyRepository.findByCategoryOrderByCanonicalNameAsc("memory")).thenReturn(List.of(ramSpec));

        List<SpecOntologyDto> results = ontologyService.getOntologySpecs("memory");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCanonicalName()).isEqualTo("ram_capacity");
        assertThat(results.get(0).getCategory()).isEqualTo("memory");
        verify(specOntologyRepository).findByCategoryOrderByCanonicalNameAsc("memory");
    }

    @Test
    void shouldGetDistinctCategories() {
        when(specOntologyRepository.findDistinctCategories()).thenReturn(List.of("display", "memory", "processor", "storage"));

        List<String> categories = ontologyService.getCategories();

        assertThat(categories).containsExactly("display", "memory", "processor", "storage");
        verify(specOntologyRepository).findDistinctCategories();
    }

    @Test
    void shouldGetSpecByCanonicalNameSuccessfully() {
        when(specOntologyRepository.findByCanonicalNameIgnoreCase("ram_capacity")).thenReturn(Optional.of(ramSpec));

        SpecOntologyDto result = ontologyService.getSpecByCanonicalName("ram_capacity");

        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("RAM Capacity");
        assertThat(result.getDefaultUnit()).isEqualTo("GB");
        assertThat(result.getDefaultOperator()).isEqualTo(">=");
        verify(specOntologyRepository).findByCanonicalNameIgnoreCase("ram_capacity");
    }

    @Test
    void shouldThrowWhenSpecNotFound() {
        when(specOntologyRepository.findByCanonicalNameIgnoreCase("unknown_spec")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ontologyService.getSpecByCanonicalName("unknown_spec"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown_spec");
    }
}
