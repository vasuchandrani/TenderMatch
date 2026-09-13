import os
import logging
from typing import Dict, Any, Optional
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import cross_val_score
from sklearn.metrics import accuracy_score
import joblib

logger = logging.getLogger(__name__)


def train_normalization_model(
    data: pd.DataFrame,
    artifact_path: Optional[str] = None,
    cv_splits: int = 3,
) -> Dict[str, Any]:
    """
    Trains a LogisticRegression classifier to map 384-dimensional spec key embedding vectors
    directly to canonical specification categories in the ontology.
    """
    X = np.array(data["embedding"].tolist())
    y = np.array(data["canonical_name"].tolist())

    model = LogisticRegression(
        max_iter=1000,
        random_state=42,
    )

    # Cross-validation
    cv_scores = cross_val_score(model, X, y, cv=cv_splits, scoring="accuracy")
    cv_acc_mean = float(np.mean(cv_scores))
    cv_acc_std = float(np.std(cv_scores))

    # Fit full
    model.fit(X, y)
    y_pred = model.predict(X)
    train_acc = float(accuracy_score(y, y_pred))

    if artifact_path:
        os.makedirs(os.path.dirname(os.path.abspath(artifact_path)), exist_ok=True)
        joblib.dump(model, artifact_path)
        logger.info(f"Saved spec normalization classifier artifact to {artifact_path}")

    return {
        "model": model,
        "metrics": {
            "cv_accuracy_mean": cv_acc_mean,
            "cv_accuracy_std": cv_acc_std,
            "train_accuracy": train_acc,
            "sample_count": len(data),
        },
        "artifact_path": artifact_path,
    }
