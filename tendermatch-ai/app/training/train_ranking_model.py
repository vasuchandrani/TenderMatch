import os
import logging
from typing import Dict, Any, Optional
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score
import joblib

logger = logging.getLogger(__name__)

RANKING_FEATURES = [
    "overall_match_ratio",
    "critical_spec_match_ratio",
    "high_spec_match_ratio",
    "ai_err_ratio",
    "override_ratio",
    "confidence_mean",
    "confidence_std",
]


def train_ranking_model(
    data: pd.DataFrame,
    artifact_path: Optional[str] = None,
    n_estimators: int = 50,
) -> Dict[str, Any]:
    """
    Trains a meta-ranking model on vendor match ratios and human override metrics
    to predict tender award relevance labels (2=winner, 1=runner-up, 0=non-winner).
    """
    X = data[RANKING_FEATURES]
    y = data["relevance_label"].astype(int)

    model = RandomForestClassifier(
        n_estimators=n_estimators,
        max_depth=5,
        random_state=42,
    )

    model.fit(X, y)
    y_pred = model.predict(X)
    train_acc = float(accuracy_score(y, y_pred))

    feat_imp = {
        col: float(imp) for col, imp in zip(RANKING_FEATURES, model.feature_importances_)
    }

    if artifact_path:
        os.makedirs(os.path.dirname(os.path.abspath(artifact_path)), exist_ok=True)
        joblib.dump(model, artifact_path)
        logger.info(f"Saved ranking model artifact to {artifact_path}")

    return {
        "model": model,
        "metrics": {
            "train_accuracy": train_acc,
            "sample_count": len(data),
        },
        "feature_importance": feat_imp,
        "artifact_path": artifact_path,
    }
