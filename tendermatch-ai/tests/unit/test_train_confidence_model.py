import os
import pytest
from app.training.training_data_extractor import TrainingDataExtractor
from app.training.train_confidence_model import train_confidence_model


def test_train_confidence_model_and_save_artifact(tmp_path):
    extractor = TrainingDataExtractor()
    df = extractor.generate_synthetic_confidence_data(n_samples=120)

    model_path = str(tmp_path / "confidence_model.joblib")
    result = train_confidence_model(df, artifact_path=model_path, cv_splits=3)

    assert "model" in result
    assert "metrics" in result
    assert "feature_importance" in result
    assert "artifact_path" in result

    # Check metrics
    metrics = result["metrics"]
    assert "cv_auc_mean" in metrics
    assert metrics["cv_auc_mean"] >= 0.55  # Quality check
    assert "train_accuracy" in metrics
    assert metrics["train_accuracy"] > 0.60

    # Check feature importance
    feat_imp = result["feature_importance"]
    assert "cross_encoder_score" in feat_imp
    assert "unit_match" in feat_imp

    # Verify file saved
    assert os.path.exists(model_path)
    assert os.path.getsize(model_path) > 0


def test_train_without_saving():
    extractor = TrainingDataExtractor()
    df = extractor.generate_synthetic_confidence_data(n_samples=60)
    result = train_confidence_model(df, artifact_path=None, cv_splits=2)
    assert result["artifact_path"] is None
    assert result["model"] is not None
    assert result["metrics"]["sample_count"] == 60
