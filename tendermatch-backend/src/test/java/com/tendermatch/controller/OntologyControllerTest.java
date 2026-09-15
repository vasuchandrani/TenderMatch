package com.tendermatch.controller;

import com.tendermatch.dto.ontology.SpecOntologyDto;
import com.tendermatch.exception.ResourceNotFoundException;
import com.tendermatch.security.CustomUserDetailsService;
import com.tendermatch.security.JwtService;
import com.tendermatch.service.OntologyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OntologyController.class)
@AutoConfigureMockMvc(addFilters = false)
public class OntologyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OntologyService ontologyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void getOntology_returnsListOfSpecs() throws Exception {
        SpecOntologyDto spec = new SpecOntologyDto(
                UUID.randomUUID(), "ram_capacity", "RAM Capacity", "memory",
                "GB", ">=", 10, true, new String[]{"RAM", "Memory"},
                ZonedDateTime.now(), ZonedDateTime.now()
        );
        when(ontologyService.getOntologySpecs(null)).thenReturn(List.of(spec));

        mockMvc.perform(get("/api/ontology"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].canonicalName").value("ram_capacity"))
                .andExpect(jsonPath("$.data[0].displayName").value("RAM Capacity"))
                .andExpect(jsonPath("$.data[0].defaultUnit").value("GB"));
    }

    @Test
    void getOntologyWithCategory_returnsFilteredList() throws Exception {
        SpecOntologyDto spec = new SpecOntologyDto(
                UUID.randomUUID(), "storage_capacity", "Storage Capacity", "storage",
                "GB", ">=", 15, true, new String[]{"Storage", "SSD"},
                ZonedDateTime.now(), ZonedDateTime.now()
        );
        when(ontologyService.getOntologySpecs("storage")).thenReturn(List.of(spec));

        mockMvc.perform(get("/api/ontology?category=storage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].canonicalName").value("storage_capacity"))
                .andExpect(jsonPath("$.data[0].category").value("storage"));
    }

    @Test
    void getCategories_returnsListOfCategories() throws Exception {
        when(ontologyService.getCategories()).thenReturn(List.of("display", "memory", "processor", "storage"));

        mockMvc.perform(get("/api/ontology/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("display"))
                .andExpect(jsonPath("$.data[1]").value("memory"));
    }

    @Test
    void getSpecByCanonicalName_returnsSpec() throws Exception {
        SpecOntologyDto spec = new SpecOntologyDto(
                UUID.randomUUID(), "cpu_cores", "Processor Cores", "processor",
                null, ">=", 8, true, new String[]{"Cores"},
                ZonedDateTime.now(), ZonedDateTime.now()
        );
        when(ontologyService.getSpecByCanonicalName("cpu_cores")).thenReturn(spec);

        mockMvc.perform(get("/api/ontology/cpu_cores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.canonicalName").value("cpu_cores"))
                .andExpect(jsonPath("$.data.displayName").value("Processor Cores"));
    }

    @Test
    void getSpecByCanonicalName_notFound_returns404() throws Exception {
        when(ontologyService.getSpecByCanonicalName("non_existent"))
                .thenThrow(new ResourceNotFoundException("Specification 'non_existent' not found in ontology."));

        mockMvc.perform(get("/api/ontology/non_existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
