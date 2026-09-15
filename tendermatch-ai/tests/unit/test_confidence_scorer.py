import pytest
from app.matching.confidence_scorer import calculate_confidence


def test_numeric_exact_high_confidence():
    conf = calculate_confidence(
        comparison_method="numeric_exact",
        comparison_confidence=0.95,
        extraction_confidence="high",
        normalization_confidence="high",
    )
    assert conf >= 0.90


def test_cross_encoder_extreme_high_confidence():
    conf = calculate_confidence(
        comparison_method="cross_encoder",
        comparison_confidence=0.95,
        extraction_confidence="high",
        normalization_confidence="high",
        cross_encoder_score=0.95,
    )
    assert conf >= 0.85


def test_cross_encoder_ambiguous_low_confidence():
    conf = calculate_confidence(
        comparison_method="cross_encoder",
        comparison_confidence=0.50,
        extraction_confidence="medium",
        normalization_confidence="medium",
        cross_encoder_score=0.50,
    )
    assert conf < 0.65


def test_llm_uncertain_very_low_confidence():
    conf = calculate_confidence(
        comparison_method="llm_judge",
        comparison_confidence=0.30,
        verdict="uncertain",
        extraction_confidence="medium",
        normalization_confidence="medium",
    )
    assert conf <= 0.35


def test_extraction_confidence_weighted():
    high_ext_conf = calculate_confidence(
        comparison_method="numeric_exact",
        comparison_confidence=0.90,
        extraction_confidence="high",
        normalization_confidence="high",
    )
    low_ext_conf = calculate_confidence(
        comparison_method="numeric_exact",
        comparison_confidence=0.90,
        extraction_confidence="low",
        normalization_confidence="low",
    )
    assert low_ext_conf < high_ext_conf


def test_confidence_returns_0_to_1():
    conf = calculate_confidence(
        comparison_method="exact_text",
        comparison_confidence=1.0,
        extraction_confidence="high",
        normalization_confidence="high",
    )
    assert 0.0 <= conf <= 1.0
