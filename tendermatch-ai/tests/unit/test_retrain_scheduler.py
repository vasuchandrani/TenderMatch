import pytest
from datetime import datetime, timedelta, timezone
from app.training.retrain_scheduler import RetrainingScheduler


def test_below_min_data_returns_false():
    scheduler = RetrainingScheduler()
    # Confidence model requires min 500 total samples
    res = scheduler.check_model_retrain_needed(
        model_name="confidence_model",
        total_data_count=200,
        new_data_count=150,
        last_trained_at=datetime.now(timezone.utc) - timedelta(days=5),
    )
    assert res["retrain_needed"] is False
    assert "Insufficient total data" in res["reason"]


def test_enough_new_data_returns_true():
    scheduler = RetrainingScheduler()
    res = scheduler.check_model_retrain_needed(
        model_name="confidence_model",
        total_data_count=600,
        new_data_count=120,  # exceeds min_new_data (100)
        last_trained_at=datetime.now(timezone.utc) - timedelta(days=5),
    )
    assert res["retrain_needed"] is True
    assert "New data threshold met" in res["reason"]


def test_stale_model_returns_true():
    scheduler = RetrainingScheduler()
    res = scheduler.check_model_retrain_needed(
        model_name="confidence_model",
        total_data_count=600,
        new_data_count=10,
        last_trained_at=datetime.now(timezone.utc) - timedelta(days=35),  # exceeds 30 days
    )
    assert res["retrain_needed"] is True
    assert "Model is stale" in res["reason"]


def test_fresh_model_returns_false():
    scheduler = RetrainingScheduler()
    res = scheduler.check_model_retrain_needed(
        model_name="confidence_model",
        total_data_count=600,
        new_data_count=20,
        last_trained_at=datetime.now(timezone.utc) - timedelta(days=4),
    )
    assert res["retrain_needed"] is False
    assert "Model is current" in res["reason"]
