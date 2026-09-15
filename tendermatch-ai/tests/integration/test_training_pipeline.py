import pytest
from app.training.training_data_extractor import TrainingDataExtractor
from app.training.train_confidence_model import train_confidence_model
from app.training.shadow_router import ShadowModelRouter


class RuleConfidenceEngine:
    def predict(self, input_data):
        # Baseline rule: high if score > 0.8
        score = input_data.get("cross_encoder_score", 0.5)
        return 1 if score > 0.75 else 0


class MlConfidenceWrapper:
    def __init__(self, trained_model):
        self.model = trained_model

    def predict(self, input_data):
        import pandas as pd
        df = pd.DataFrame([input_data])
        from app.training.train_confidence_model import FEATURE_COLUMNS
        X = df[FEATURE_COLUMNS]
        return int(self.model.predict(X)[0])


def test_full_confidence_training_and_shadow_lifecycle(tmp_path):
    # 1. Extract training data
    extractor = TrainingDataExtractor()
    df = extractor.generate_synthetic_confidence_data(n_samples=150)
    assert len(df) == 150

    # 2. Train model and serialize
    model_file = str(tmp_path / "model.joblib")
    train_res = train_confidence_model(df, artifact_path=model_file, cv_splits=3)
    trained_model = train_res["model"]
    assert train_res["metrics"]["cv_auc_mean"] > 0.50

    # 3. Initialize Shadow Router
    rule_engine = RuleConfidenceEngine()
    ml_model = MlConfidenceWrapper(trained_model)
    router = ShadowModelRouter(
        rule_engine=rule_engine,
        ml_model=ml_model,
        model_name="confidence_model",
        shadow_mode=True,
    )

    # 4. Run test predictions in shadow mode
    test_sample = df.iloc[0].to_dict()
    res = router.predict(test_sample)
    assert res in [0, 1]

    # Verify shadow stats
    stats = router.get_shadow_stats()
    assert stats["total_predictions"] == 1

    # 5. Promote model to active serving
    router.shadow_mode = False
    active_res = router.predict(test_sample)
    assert active_res in [0, 1]
