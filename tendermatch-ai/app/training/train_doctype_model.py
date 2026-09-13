import os
import logging
from typing import Dict, Any, Optional
import numpy as np
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import SGDClassifier
from sklearn.pipeline import Pipeline
from sklearn.model_selection import cross_val_score
from sklearn.metrics import accuracy_score
import joblib

logger = logging.getLogger(__name__)


def train_doctype_model(
    data: pd.DataFrame,
    artifact_path: Optional[str] = None,
    cv_splits: int = 3,
) -> Dict[str, Any]:
    """
    Trains a TF-IDF + SGDClassifier (linear SVM / logistic loss) pipeline
    to classify document texts into tender requirements, vendor bids, datasheets, etc.
    """
    X = data["text"].astype(str).tolist()
    y = data["doc_type"].astype(str).tolist()

    pipeline = Pipeline(
        [
            ("tfidf", TfidfVectorizer(max_features=2500, ngram_range=(1, 2))),
            ("clf", SGDClassifier(loss="log_loss", alpha=1e-4, max_iter=1000, random_state=42)),
        ]
    )

    # Cross-validation
    cv_scores = cross_val_score(pipeline, X, y, cv=cv_splits, scoring="accuracy")
    cv_acc_mean = float(np.mean(cv_scores))
    cv_acc_std = float(np.std(cv_scores))

    # Fit on all data
    pipeline.fit(X, y)
    y_pred = pipeline.predict(X)
    train_acc = float(accuracy_score(y, y_pred))

    if artifact_path:
        os.makedirs(os.path.dirname(os.path.abspath(artifact_path)), exist_ok=True)
        joblib.dump(pipeline, artifact_path)
        logger.info(f"Saved document type classifier artifact to {artifact_path}")

    return {
        "pipeline": pipeline,
        "metrics": {
            "cv_accuracy_mean": cv_acc_mean,
            "cv_accuracy_std": cv_acc_std,
            "train_accuracy": train_acc,
            "sample_count": len(data),
        },
        "artifact_path": artifact_path,
    }
