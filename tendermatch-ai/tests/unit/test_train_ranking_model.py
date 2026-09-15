import os
import pytest
from app.training.training_data_extractor import TrainingDataExtractor
from app.training.train_ranking_model import train_ranking_model


def test_train_ranking_model_and_save_artifact(tmp_path):
    extractor = TrainingDataExtractor()
    df = extractor.generate_synthetic_ranking_data(n_tenders=12, vendors_per_tender=3)

    model_path = str(tmp_path / "ranking_model.joblib")
    result = train_ranking_model(df, artifact_path=model_path)

    assert "model" in result
    assert "metrics" in result
    assert "artifact_path" in result

    # Check metrics
    metrics = result["metrics"]
    assert "train_accuracy" in metrics
    assert metrics["train_accuracy"] > 0.50

    # Check file saved
    assert os.path.exists(model_path)
    assert os.path.getsize(model_path) > 0
