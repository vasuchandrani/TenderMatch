package com.tendermatch.service;

import com.tendermatch.dto.scoring.SpecWeightDto;
import com.tendermatch.dto.scoring.WeightTemplateDto;
import com.tendermatch.model.ScoringWeight;
import com.tendermatch.model.Tender;
import com.tendermatch.model.TenderProduct;
import com.tendermatch.model.TenderSpec;
import com.tendermatch.model.User;
import com.tendermatch.repository.ScoringWeightRepository;
import com.tendermatch.repository.TenderRepository;
import com.tendermatch.repository.TenderSpecRepository;
import com.tendermatch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeightServiceTest {

    @Mock
    private ScoringWeightRepository scoringWeightRepository;

    @Mock
    private TenderRepository tenderRepository;

    @Mock
    private TenderSpecRepository tenderSpecRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ScoringService scoringService;

    @InjectMocks
    private WeightService weightService;

    private UUID tenderId;
    private UUID userId;
    private Tender tender;
    private User user;

    @BeforeEach
    void setUp() {
        tenderId = UUID.randomUUID();
        userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("admin@example.com")
                .name("Admin")
                .role("ADMIN")
                .build();

        tender = Tender.builder()
                .id(tenderId)
                .name("IT Procurement")
                .user(user)
                .build();
    }

    @Test
    void shouldReturnDefaultWeightsWhenNoneSaved() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(scoringWeightRepository.findByTenderId(tenderId)).thenReturn(Collections.emptyList());

        TenderSpec spec1 = new TenderSpec();
        spec1.setCanonicalSpecName("ram_capacity");
        TenderSpec spec2 = new TenderSpec();
        spec2.setCanonicalSpecName("cpu_speed");

        when(tenderSpecRepository.findByTenderId(tenderId)).thenReturn(List.of(spec1, spec2));

        List<SpecWeightDto> weights = weightService.getWeightsForTender(tenderId);

        assertThat(weights).hasSize(2);
        assertThat(weights).allMatch(w -> w.weightCategory().equals("medium") && w.weightValue() == 1.0);
    }

    @Test
    void shouldReturnSavedWeightsForTender() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));

        ScoringWeight sw1 = ScoringWeight.builder()
                .canonicalSpecName("ram_capacity")
                .weightCategory("critical")
                .weightValue(3.0)
                .build();

        when(scoringWeightRepository.findByTenderId(tenderId)).thenReturn(List.of(sw1));

        List<SpecWeightDto> weights = weightService.getWeightsForTender(tenderId);

        assertThat(weights).hasSize(1);
        assertThat(weights.get(0).canonicalSpecName()).isEqualTo("ram_capacity");
        assertThat(weights.get(0).weightCategory()).isEqualTo("critical");
        assertThat(weights.get(0).weightValue()).isEqualTo(3.0);
    }

    @Test
    void shouldSaveCustomWeightsAndRecalculate() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        List<SpecWeightDto> newWeights = List.of(
                new SpecWeightDto("ram_capacity", "critical", 3.0),
                new SpecWeightDto("display_size", "low", 0.5)
        );

        weightService.updateWeights(tenderId, userId, newWeights);

        verify(scoringWeightRepository).deleteByTenderId(tenderId);
        verify(scoringWeightRepository, times(2)).save(any(ScoringWeight.class));
        verify(scoringService).calculateScores(tenderId);
    }

    @Test
    void shouldSaveWeightTemplate() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        List<SpecWeightDto> templateWeights = List.of(
                new SpecWeightDto("ram_capacity", "critical", 3.0)
        );

        weightService.saveTemplate(userId, "Government Laptop Standard", templateWeights);

        verify(scoringWeightRepository).deleteByTemplateName("Government Laptop Standard");
        verify(scoringWeightRepository).save(any(ScoringWeight.class));
    }

    @Test
    void shouldListDistinctTemplates() {
        when(scoringWeightRepository.findDistinctTemplateNames()).thenReturn(List.of("Hardware Standard", "Software Standard"));

        ScoringWeight w1 = ScoringWeight.builder()
                .templateName("Hardware Standard")
                .canonicalSpecName("ram_capacity")
                .weightCategory("critical")
                .weightValue(3.0)
                .build();

        when(scoringWeightRepository.findByTemplateName("Hardware Standard")).thenReturn(List.of(w1));
        when(scoringWeightRepository.findByTemplateName("Software Standard")).thenReturn(Collections.emptyList());

        List<WeightTemplateDto> templates = weightService.listTemplates();

        assertThat(templates).hasSize(2);
        assertThat(templates.get(0).templateName()).isEqualTo("Hardware Standard");
        assertThat(templates.get(0).weights()).hasSize(1);
    }

    @Test
    void shouldApplyTemplateToTender() {
        when(tenderRepository.findById(tenderId)).thenReturn(Optional.of(tender));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ScoringWeight templateWeight = ScoringWeight.builder()
                .templateName("Hardware Standard")
                .canonicalSpecName("ram_capacity")
                .weightCategory("critical")
                .weightValue(3.0)
                .build();

        when(scoringWeightRepository.findByTemplateName("Hardware Standard")).thenReturn(List.of(templateWeight));

        weightService.applyTemplate(tenderId, userId, "Hardware Standard");

        verify(scoringWeightRepository).deleteByTenderId(tenderId);
        verify(scoringWeightRepository).save(any(ScoringWeight.class));
        verify(scoringService).calculateScores(tenderId);
    }
}
