package com.tendermatch.service;

import com.tendermatch.dto.scoring.SpecWeightDto;
import com.tendermatch.dto.scoring.WeightTemplateDto;
import com.tendermatch.model.ScoringWeight;
import com.tendermatch.model.Tender;
import com.tendermatch.model.TenderSpec;
import com.tendermatch.model.User;
import com.tendermatch.repository.ScoringWeightRepository;
import com.tendermatch.repository.TenderRepository;
import com.tendermatch.repository.TenderSpecRepository;
import com.tendermatch.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class WeightService {

    private final ScoringWeightRepository scoringWeightRepository;
    private final TenderRepository tenderRepository;
    private final TenderSpecRepository tenderSpecRepository;
    private final UserRepository userRepository;
    private final ScoringService scoringService;

    public WeightService(ScoringWeightRepository scoringWeightRepository,
                         TenderRepository tenderRepository,
                         TenderSpecRepository tenderSpecRepository,
                         UserRepository userRepository,
                         ScoringService scoringService) {
        this.scoringWeightRepository = scoringWeightRepository;
        this.tenderRepository = tenderRepository;
        this.tenderSpecRepository = tenderSpecRepository;
        this.userRepository = userRepository;
        this.scoringService = scoringService;
    }

    @Transactional(readOnly = true)
    public List<SpecWeightDto> getWeightsForTender(UUID tenderId) {
        tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found: " + tenderId));

        List<ScoringWeight> saved = scoringWeightRepository.findByTenderId(tenderId);
        if (!saved.isEmpty()) {
            return saved.stream()
                    .map(sw -> new SpecWeightDto(sw.getCanonicalSpecName(), sw.getWeightCategory(), sw.getWeightValue()))
                    .collect(Collectors.toList());
        }

        // Return default weights for all specs of this tender
        List<TenderSpec> specs = tenderSpecRepository.findByTenderId(tenderId);
        Set<String> canonicalNames = specs.stream()
                .map(s -> s.getCanonicalSpecName() != null ? s.getCanonicalSpecName() : s.getRawSpecName())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return canonicalNames.stream()
                .map(name -> new SpecWeightDto(name, "medium", 1.0))
                .collect(Collectors.toList());
    }

    public List<SpecWeightDto> updateWeights(UUID tenderId, String userEmail, List<SpecWeightDto> weights) {
        User user = userEmail != null ? userRepository.findByEmail(userEmail).orElse(null) : null;
        UUID userId = user != null ? user.getId() : UUID.randomUUID();
        return updateWeights(tenderId, userId, weights);
    }

    public List<SpecWeightDto> updateWeights(UUID tenderId, UUID userId, List<SpecWeightDto> weights) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found: " + tenderId));
        User user = userRepository.findById(userId).orElse(null);

        scoringWeightRepository.deleteByTenderId(tenderId);

        List<ScoringWeight> entities = weights.stream().map(dto -> ScoringWeight.builder()
                .tender(tender)
                .user(user)
                .canonicalSpecName(dto.canonicalSpecName())
                .weightCategory(dto.weightCategory() != null ? dto.weightCategory() : "medium")
                .weightValue(dto.weightValue() != null ? dto.weightValue() : 1.0)
                .build()
        ).collect(Collectors.toList());

        entities.forEach(scoringWeightRepository::save);

        // Recalculate scores with newly updated weights
        scoringService.calculateScores(tenderId);

        return weights;
    }

    public WeightTemplateDto saveTemplate(String userEmail, String templateName, List<SpecWeightDto> weights) {
        User user = userEmail != null ? userRepository.findByEmail(userEmail).orElse(null) : null;
        UUID userId = user != null ? user.getId() : UUID.randomUUID();
        return saveTemplate(userId, templateName, weights);
    }

    public WeightTemplateDto saveTemplate(UUID userId, String templateName, List<SpecWeightDto> weights) {
        User user = userRepository.findById(userId).orElse(null);

        scoringWeightRepository.deleteByTemplateName(templateName);

        for (SpecWeightDto dto : weights) {
            ScoringWeight sw = ScoringWeight.builder()
                    .templateName(templateName)
                    .user(user)
                    .canonicalSpecName(dto.canonicalSpecName())
                    .weightCategory(dto.weightCategory() != null ? dto.weightCategory() : "medium")
                    .weightValue(dto.weightValue() != null ? dto.weightValue() : 1.0)
                    .build();
            scoringWeightRepository.save(sw);
        }

        return new WeightTemplateDto(templateName, weights);
    }

    @Transactional(readOnly = true)
    public List<WeightTemplateDto> listTemplates() {
        List<String> templateNames = scoringWeightRepository.findDistinctTemplateNames();
        List<WeightTemplateDto> result = new ArrayList<>();

        for (String name : templateNames) {
            List<ScoringWeight> weights = scoringWeightRepository.findByTemplateName(name);
            List<SpecWeightDto> dtoList = weights.stream()
                    .map(sw -> new SpecWeightDto(sw.getCanonicalSpecName(), sw.getWeightCategory(), sw.getWeightValue()))
                    .collect(Collectors.toList());
            result.add(new WeightTemplateDto(name, dtoList));
        }

        return result;
    }

    public List<SpecWeightDto> applyTemplate(UUID tenderId, String userEmail, String templateName) {
        User user = userEmail != null ? userRepository.findByEmail(userEmail).orElse(null) : null;
        UUID userId = user != null ? user.getId() : UUID.randomUUID();
        return applyTemplate(tenderId, userId, templateName);
    }

    public List<SpecWeightDto> applyTemplate(UUID tenderId, UUID userId, String templateName) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found: " + tenderId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        List<ScoringWeight> templateWeights = scoringWeightRepository.findByTemplateName(templateName);
        if (templateWeights.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found: " + templateName);
        }

        scoringWeightRepository.deleteByTenderId(tenderId);

        List<SpecWeightDto> appliedDtos = new ArrayList<>();
        for (ScoringWeight tw : templateWeights) {
            ScoringWeight sw = ScoringWeight.builder()
                    .tender(tender)
                    .user(user)
                    .canonicalSpecName(tw.getCanonicalSpecName())
                    .weightCategory(tw.getWeightCategory())
                    .weightValue(tw.getWeightValue())
                    .build();
            scoringWeightRepository.save(sw);
            appliedDtos.add(new SpecWeightDto(sw.getCanonicalSpecName(), sw.getWeightCategory(), sw.getWeightValue()));
        }

        // Recalculate scores
        scoringService.calculateScores(tenderId);

        return appliedDtos;
    }
}
