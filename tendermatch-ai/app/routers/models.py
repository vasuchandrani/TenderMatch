import logging
from datetime import datetime, timezone
from typing import Dict, Any, List, Optional
from fastapi import APIRouter, HTTPException, BackgroundTasks
from pydantic import BaseModel

from app.training.training_data_extractor import TrainingDataExtractor
from app.training.train_confidence_model import train_confidence_model
from app.training.train_operator_model import train_operator_model
from app.training.train_doctype_model import train_doctype_model
from app.training.train_normalization_model import train_normalization_model
from app.training.train_ranking_model import train_ranking_model
from app.training.shadow_router import ShadowModelRouter
from app.training.retrain_scheduler import RetrainingScheduler

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", tags=["models"])

# In-memory Model Registry state
MODEL_REGISTRY: Dict[str, List[Dict[str, Any]]] = {
    "confidence_model": [
        {
            "model_name": "confidence_model",
            "version": 1,
            "status": "shadow",  # trained, shadow, promoted, retired
            "model_type": "GradientBoostingClassifier",
            "primary_metric": "cv_auc_mean",
            "primary_metric_value": 0.824,
            "metrics": {"cv_auc_mean": 0.824, "train_accuracy": 0.885, "sample_count": 600},
            "training_data_count": 600,
            "trained_at": "2026-03-01T10:00:00Z",
            "model_path": "models/confidence_model/v1/model.joblib",
        }
    ],
    "operator_model": [
        {
            "model_name": "operator_model",
            "version": 1,
            "status": "shadow",
            "model_type": "DecisionTreeClassifier",
            "primary_metric": "cv_accuracy_mean",
            "primary_metric_value": 0.912,
            "metrics": {"cv_accuracy_mean": 0.912, "train_accuracy": 0.960, "sample_count": 550},
            "training_data_count": 550,
            "trained_at": "2026-03-01T10:00:00Z",
            "model_path": "models/operator_model/v1/model.joblib",
        }
    ],
    "doctype_model": [
        {
            "model_name": "doctype_model",
            "version": 1,
            "status": "shadow",
            "model_type": "SGDClassifier",
            "primary_metric": "cv_accuracy_mean",
            "primary_metric_value": 0.935,
            "metrics": {"cv_accuracy_mean": 0.935, "train_accuracy": 0.978, "sample_count": 120},
            "training_data_count": 120,
            "trained_at": "2026-03-01T10:00:00Z",
            "model_path": "models/doctype_model/v1/model.joblib",
        }
    ],
    "normalization_model": [
        {
            "model_name": "normalization_model",
            "version": 1,
            "status": "shadow",
            "model_type": "LogisticRegression",
            "primary_metric": "cv_accuracy_mean",
            "primary_metric_value": 0.880,
            "metrics": {"cv_accuracy_mean": 0.880, "train_accuracy": 0.940, "sample_count": 250},
            "training_data_count": 250,
            "trained_at": "2026-03-01T10:00:00Z",
            "model_path": "models/normalization_model/v1/model.joblib",
        }
    ],
    "ranking_model": [
        {
            "model_name": "ranking_model",
            "version": 1,
            "status": "trained",
            "model_type": "RandomForestClassifier",
            "primary_metric": "train_accuracy",
            "primary_metric_value": 0.850,
            "metrics": {"train_accuracy": 0.850, "sample_count": 220},
            "training_data_count": 220,
            "trained_at": "2026-03-01T10:00:00Z",
            "model_path": "models/ranking_model/v1/model.joblib",
        }
    ],
}

# Simulated Shadow Stats
SHADOW_STATS: Dict[str, Dict[str, Any]] = {
    "confidence_model": {
        "model_name": "confidence_model",
        "total_predictions": 142,
        "agreement_count": 131,
        "disagreement_count": 11,
        "agreement_rate": 0.9225,
        "disagreements": [
            {
                "input_hash": "a1b2c3d4",
                "rule_result": "Match",
                "ml_result": "Unmatch",
                "input_sample": "Spec: RAM 16GB vs 16GB, method: cross_encoder, score: 0.71",
            },
            {
                "input_hash": "e5f6g7h8",
                "rule_result": "AI Can't Predict",
                "ml_result": "Match",
                "input_sample": "Spec: Processor Intel Core i7 vs AMD Ryzen 7, method: llm_judge",
            },
        ],
    },
    "operator_model": {
        "model_name": "operator_model",
        "total_predictions": 180,
        "agreement_count": 171,
        "disagreement_count": 9,
        "agreement_rate": 0.9500,
        "disagreements": [
            {
                "input_hash": "op12345",
                "rule_result": ">=",
                "ml_result": "==",
                "input_sample": "Spec: Ports Count, unit: none, value: 4",
            }
        ],
    },
    "doctype_model": {
        "model_name": "doctype_model",
        "total_predictions": 95,
        "agreement_count": 91,
        "disagreement_count": 4,
        "agreement_rate": 0.9579,
        "disagreements": [],
    },
    "normalization_model": {
        "model_name": "normalization_model",
        "total_predictions": 110,
        "agreement_count": 102,
        "disagreement_count": 8,
        "agreement_rate": 0.9273,
        "disagreements": [],
    },
    "ranking_model": {
        "model_name": "ranking_model",
        "total_predictions": 45,
        "agreement_count": 41,
        "disagreement_count": 4,
        "agreement_rate": 0.9111,
        "disagreements": [],
    },
}

retrain_scheduler = RetrainingScheduler()
extractor = TrainingDataExtractor()


@router.get("/models")
async def list_models():
    """Lists all models with their current active/latest version summary."""
    summaries = []
    for model_name, versions in MODEL_REGISTRY.items():
        if not versions:
            continue
        # Find promoted version, else latest version
        promoted = next((v for v in versions if v["status"] == "promoted"), None)
        active = promoted if promoted else versions[-1]
        summaries.append(active)
    return summaries


@router.get("/models/{model_name}/versions")
async def get_model_versions(model_name: str):
    """Returns the full version history for a given model."""
    if model_name not in MODEL_REGISTRY:
        raise HTTPException(status_code=404, detail=f"Model '{model_name}' not found")
    return sorted(MODEL_REGISTRY[model_name], key=lambda x: x["version"], reverse=True)


@router.post("/models/{model_name}/{version}/promote")
async def promote_model(model_name: str, version: int):
    """Promotes a trained/shadow model to active 'promoted' status, retiring the previous active version."""
    if model_name not in MODEL_REGISTRY:
        raise HTTPException(status_code=404, detail=f"Model '{model_name}' not found")

    target_ver = next((v for v in MODEL_REGISTRY[model_name] if v["version"] == version), None)
    if not target_ver:
        raise HTTPException(status_code=404, detail=f"Version {version} for model '{model_name}' not found")

    # Demote existing promoted versions to retired
    for v in MODEL_REGISTRY[model_name]:
        if v["status"] == "promoted":
            v["status"] = "retired"
            v["retired_at"] = datetime.now(timezone.utc).isoformat()

    target_ver["status"] = "promoted"
    target_ver["promoted_at"] = datetime.now(timezone.utc).isoformat()
    return target_ver


@router.post("/models/{model_name}/{version}/rollback")
async def rollback_model(model_name: str, version: int):
    """Rolls back a promoted model to shadow or trained status."""
    if model_name not in MODEL_REGISTRY:
        raise HTTPException(status_code=404, detail=f"Model '{model_name}' not found")

    target_ver = next((v for v in MODEL_REGISTRY[model_name] if v["version"] == version), None)
    if not target_ver:
        raise HTTPException(status_code=404, detail=f"Version {version} for model '{model_name}' not found")

    target_ver["status"] = "shadow"
    target_ver["retired_at"] = datetime.now(timezone.utc).isoformat()

    # Find highest prior version if any and reactivate
    prior_versions = [v for v in MODEL_REGISTRY[model_name] if v["version"] < version]
    if prior_versions:
        prior_ver = max(prior_versions, key=lambda x: x["version"])
        prior_ver["status"] = "promoted"
        prior_ver["promoted_at"] = datetime.now(timezone.utc).isoformat()

    return target_ver


@router.get("/models/{model_name}/shadow-stats")
async def get_shadow_stats(model_name: str):
    """Returns shadow mode agreement metrics and recent disagreements."""
    if model_name not in SHADOW_STATS:
        raise HTTPException(status_code=404, detail=f"No shadow stats for '{model_name}'")
    return SHADOW_STATS[model_name]


@router.get("/training/retrain-check")
async def check_all_models_retrain_needed():
    """Checks whether any models meet retraining thresholds based on data volume or staleness."""
    results = []
    for model_name, versions in MODEL_REGISTRY.items():
        latest = versions[-1] if versions else None
        total_data = latest["training_data_count"] if latest else 0
        new_data = 35  # mock new corrections
        trained_at_str = latest.get("trained_at") if latest else None
        trained_at = datetime.fromisoformat(trained_at_str.replace("Z", "+00:00")) if trained_at_str else None

        check = retrain_scheduler.check_model_retrain_needed(
            model_name=model_name,
            total_data_count=total_data,
            new_data_count=new_data,
            last_trained_at=trained_at,
        )
        results.append(check)

    return results


@router.post("/training/train/{model_name}")
async def trigger_train_model(model_name: str):
    """Executes model training pipeline, generates evaluation metrics, and registers new model version."""
    if model_name not in MODEL_REGISTRY:
        raise HTTPException(status_code=404, detail=f"Model '{model_name}' not supported")

    versions = MODEL_REGISTRY[model_name]
    next_ver = len(versions) + 1
    artifact_path = f"models/{model_name}/v{next_ver}/model.joblib"

    if model_name == "confidence_model":
        df = extractor.generate_synthetic_confidence_data(n_samples=150)
        res = train_confidence_model(df, artifact_path=artifact_path, cv_splits=3)
        metrics = res["metrics"]
        primary_metric = "cv_auc_mean"
        primary_val = metrics["cv_auc_mean"]
        mtype = "GradientBoostingClassifier"

    elif model_name == "operator_model":
        df = extractor.generate_synthetic_operator_data(n_samples=100)
        res = train_operator_model(df, artifact_path=artifact_path, cv_splits=3)
        metrics = res["metrics"]
        primary_metric = "cv_accuracy_mean"
        primary_val = metrics["cv_accuracy_mean"]
        mtype = "DecisionTreeClassifier"

    elif model_name == "doctype_model":
        df = extractor.generate_synthetic_doctype_data(n_samples=60)
        res = train_doctype_model(df, artifact_path=artifact_path, cv_splits=3)
        metrics = res["metrics"]
        primary_metric = "cv_accuracy_mean"
        primary_val = metrics["cv_accuracy_mean"]
        mtype = "SGDClassifier"

    elif model_name == "normalization_model":
        df = extractor.generate_synthetic_normalization_data(n_samples=60)
        res = train_normalization_model(df, artifact_path=artifact_path, cv_splits=3)
        metrics = res["metrics"]
        primary_metric = "cv_accuracy_mean"
        primary_val = metrics["cv_accuracy_mean"]
        mtype = "LogisticRegression"

    elif model_name == "ranking_model":
        df = extractor.generate_synthetic_ranking_data(n_tenders=15, vendors_per_tender=3)
        res = train_ranking_model(df, artifact_path=artifact_path)
        metrics = res["metrics"]
        primary_metric = "train_accuracy"
        primary_val = metrics["train_accuracy"]
        mtype = "RandomForestClassifier"

    new_version_entry = {
        "model_name": model_name,
        "version": next_ver,
        "status": "trained",
        "model_type": mtype,
        "primary_metric": primary_metric,
        "primary_metric_value": primary_val,
        "metrics": metrics,
        "training_data_count": metrics.get("sample_count", 100),
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "model_path": artifact_path,
    }
    versions.append(new_version_entry)

    return new_version_entry
