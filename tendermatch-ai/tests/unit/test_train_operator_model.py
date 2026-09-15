import os
import pytest
from app.training.training_data_extractor import TrainingDataExtractor
from app.training.train_operator_model import train_operator_model


def test_train_operator_model_and_export_rules(tmp_path):
    extractor = TrainingDataExtractor()
    df = extractor.generate_synthetic_operator_data(n_samples=80)

    model_path = str(tmp_path / "operator_model.joblib")
    result = train_operator_model(df, artifact_path=model_path, cv_splits=3)

    assert "model" in result
    assert "metrics" in result
    assert "exported_rules" in result
    assert "artifact_path" in result

    # Check metrics
    metrics = result["metrics"]
    assert "cv_accuracy_mean" in metrics
    assert metrics["train_accuracy"] > 0.60

    # Check human-readable rules export
    rules_text = result["exported_rules"]
    assert isinstance(rules_text, str)
    assert len(rules_text) > 0

    # Check file saved
    assert os.path.exists(model_path)
    assert os.path.getsize(model_path) > 0
