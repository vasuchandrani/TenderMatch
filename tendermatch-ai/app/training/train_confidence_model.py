import os
import logging
from typing import Dict, Any, Optional
import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.model_selection import StratifiedKFold, cross_val_score
from sklearn.metrics import roc_auc_score, accuracy_score
import joblib

logger = logging.getLogger(__name__)

FEATURE_COLUMNS = [
    "cross_encoder_score",
    "is_numeric",
    "unit_match",
    "extraction_confidence_required",
    "extraction_confidence_vendor",
    "normalization_confidence",
    "value_ratio",
    "method_numeric_exact",
    "method_cross_encoder",
    "method_llm_judge",
    "method_exact_text",
]


def train_confidence_model(
    data: pd.DataFrame,
    artifact_path: Optional[str] = None,
    cv_splits: int = 5,
    n_estimators: int = 100,
    learning_rate: float = 0.1,
    max_depth: int = 4,
) -> Dict[str, Any]:
    """
    Trains a Gradient Boosting meta-model to predict probability of system verdict correctness.
    Saves the serialized model artifact using joblib.
    """
    X = data[FEATURE_COLUMNS]
    y = data["correct"].astype(int)

    # Instantiate model
    model = GradientBoostingClassifier(
        n_estimators=n_estimators,
        learning_rate=learning_rate,
        max_depth=max_depth,
        random_state=42,
    )

    # Cross-validation
    cv = StratifiedKFold(n_splits=cv_splits, shuffle=True, random_state=42)
    cv_scores = cross_val_score(model, X, y, cv=cv, scoring="roc_auc")
    cv_auc_mean = float(np.mean(cv_scores))
    cv_auc_std = float(np.std(cv_scores))

    # Fit on full data
    model.fit(X, y)
    y_pred = model.predict(X)
    train_acc = float(accuracy_score(y, y_pred))

    # Feature importance
    importances = model.feature_importances_
    feat_imp = {col: float(imp) for col, imp in zip(FEATURE_COLUMNS, importances)}

    # Save artifact
    if artifact_path:
        os.makedirs(os.path.dirname(os.path.abspath(artifact_path)), exist_ok=True)
        joblib.dump(model, artifact_path)
        logger.info(f"Saved confidence model artifact to {artifact_path}")

    return {
        "model": model,
        "metrics": {
            "cv_auc_mean": cv_auc_mean,
            "cv_auc_std": cv_auc_std,
            "train_accuracy": train_acc,
            "sample_count": len(data),
        },
        "feature_importance": feat_imp,
        "artifact_path": artifact_path,
    }
