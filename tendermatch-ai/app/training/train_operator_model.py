import os
import logging
from typing import Dict, Any, Optional
import numpy as np
import pandas as pd
from sklearn.tree import DecisionTreeClassifier, export_text
from sklearn.model_selection import cross_val_score
from sklearn.metrics import accuracy_score
import joblib

logger = logging.getLogger(__name__)


def train_operator_model(
    data: pd.DataFrame,
    artifact_path: Optional[str] = None,
    cv_splits: int = 5,
    max_depth: int = 8,
) -> Dict[str, Any]:
    """
    Trains a DecisionTreeClassifier to infer the appropriate comparison operator
    (>=, <=, ==, contains, semantic) based on specification features.
    Exports human-readable decision rules for explainability.
    """
    df = data.copy()
    y = df["correct_operator"].astype(str)

    # Feature engineering: One-hot encode unit_type and value_type
    feature_df = pd.get_dummies(
        df[["unit_type", "value_type", "is_numeric", "feature_spec_length"]],
        columns=["unit_type", "value_type"],
        drop_first=False,
    )
    feature_names = list(feature_df.columns)
    X = feature_df.values

    model = DecisionTreeClassifier(
        max_depth=max_depth,
        min_samples_leaf=2,
        class_weight="balanced",
        random_state=42,
    )

    # Cross-validation
    cv_scores = cross_val_score(model, X, y, cv=cv_splits, scoring="accuracy")
    cv_acc_mean = float(np.mean(cv_scores))
    cv_acc_std = float(np.std(cv_scores))

    # Fit full model
    model.fit(X, y)
    y_pred = model.predict(X)
    train_acc = float(accuracy_score(y, y_pred))

    # Export text rules
    exported_rules = export_text(model, feature_names=feature_names)

    # Serialize
    bundle = {
        "model": model,
        "feature_names": feature_names,
    }
    if artifact_path:
        os.makedirs(os.path.dirname(os.path.abspath(artifact_path)), exist_ok=True)
        joblib.dump(bundle, artifact_path)
        logger.info(f"Saved operator model artifact to {artifact_path}")

    return {
        "model": model,
        "metrics": {
            "cv_accuracy_mean": cv_acc_mean,
            "cv_accuracy_std": cv_acc_std,
            "train_accuracy": train_acc,
            "sample_count": len(data),
        },
        "exported_rules": exported_rules,
        "artifact_path": artifact_path,
    }
