import pytest
import pandas as pd
from app.training.training_data_extractor import (
    TrainingDataExtractor,
    InsufficientDataError,
)


def test_extract_confidence_training_data():
    extractor = TrainingDataExtractor()
    raw_logs = [
        {
            "system_verdict": "Match",
            "human_verdict": "Match",
            "cross_encoder_score": 0.95,
            "comparison_method": "cross_encoder",
            "is_numeric": False,
            "unit_match": True,
            "extraction_confidence_required": "HIGH",
            "extraction_confidence_vendor": "HIGH",
            "normalization_confidence": "HIGH",
            "value_ratio": 1.0,
            "product_category": "electronics",
        },
        {
            "system_verdict": "Match",
            "human_verdict": "Unmatch",
            "cross_encoder_score": 0.65,
            "comparison_method": "numeric_exact",
            "is_numeric": True,
            "unit_match": False,
            "extraction_confidence_required": "MEDIUM",
            "extraction_confidence_vendor": "LOW",
            "normalization_confidence": "HIGH",
            "value_ratio": 0.5,
            "product_category": "mechanical",
        },
    ] * 30  # 60 records

    df = extractor.extract_confidence_training_data(raw_data=raw_logs, min_corrections=50)
    assert isinstance(df, pd.DataFrame)
    assert len(df) == 60
    assert "correct" in df.columns
    assert "cross_encoder_score" in df.columns
    assert "is_numeric" in df.columns
    assert "unit_match" in df.columns
    # Check targets: first should be 1 (Match == Match), second should be 0 (Match != Unmatch)
    assert df["correct"].iloc[0] == 1
    assert df["correct"].iloc[1] == 0


def test_confidence_minimum_data_threshold_gate():
    extractor = TrainingDataExtractor()
    small_data = [{"system_verdict": "Match", "human_verdict": "Match"}] * 10
    with pytest.raises(InsufficientDataError):
        extractor.extract_confidence_training_data(raw_data=small_data, min_corrections=50)


def test_extract_operator_training_data():
    extractor = TrainingDataExtractor()
    df = extractor.generate_synthetic_operator_data(n_samples=60)
    assert isinstance(df, pd.DataFrame)
    assert len(df) == 60
    assert "correct_operator" in df.columns
    assert set(df["correct_operator"].unique()).issubset({">=", "<=", "==", "contains", "semantic"})


def test_extract_doctype_training_data():
    extractor = TrainingDataExtractor()
    df = extractor.generate_synthetic_doctype_data(n_samples=30)
    assert isinstance(df, pd.DataFrame)
    assert len(df) == 30
    assert "text" in df.columns
    assert "doc_type" in df.columns


def test_extract_ranking_training_data():
    extractor = TrainingDataExtractor()
    df = extractor.generate_synthetic_ranking_data(n_tenders=10, vendors_per_tender=3)
    assert isinstance(df, pd.DataFrame)
    assert len(df) == 30
    assert "tender_id" in df.columns
    assert "relevance_label" in df.columns
    assert "overall_match_ratio" in df.columns
    # Target values: 2 for winner, 1 for runner-up, 0 for third
    assert set(df["relevance_label"].unique()).issubset({0, 1, 2})
