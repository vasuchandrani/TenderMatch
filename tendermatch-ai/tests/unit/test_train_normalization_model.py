import os
import pytest
from app.training.training_data_extractor import TrainingDataExtractor
from app.training.train_normalization_model import train_normalization_model


def test_train_normalization_model_and_save_artifact(tmp_path):
    extractor = TrainingDataExtractor()
    df = extractor.generate_synthetic_normalization_data(n_samples=50)

    model_path = str(tmp_path / "normalization_model.joblib")
    result = train_normalization_model(df, artifact_path=model_path, cv_splits=3)

    assert "model" in result
    assert "metrics" in result
    assert "artifact_path" in result

    # Check metrics
    metrics = result["metrics"]
    assert "cv_accuracy_mean" in metrics
    assert metrics["train_accuracy"] > 0.70

    # Check file saved
    assert os.path.exists(model_path)
    assert os.path.getsize(model_path) > 0
