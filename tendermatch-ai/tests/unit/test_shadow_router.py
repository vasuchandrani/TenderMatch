import pytest
from app.training.shadow_router import ShadowModelRouter


class MockRuleEngine:
    def __init__(self, output="Match"):
        self.output = output

    def predict(self, input_data):
        return self.output


class MockMlModel:
    def __init__(self, output="Match"):
        self.output = output

    def predict(self, input_data):
        return self.output


def test_shadow_mode_returns_rule_result():
    rule = MockRuleEngine(output="Match")
    ml = MockMlModel(output="Unmatch")
    router = ShadowModelRouter(rule_engine=rule, ml_model=ml, model_name="confidence_model", shadow_mode=True)

    result = router.predict({"spec": "RAM"})
    assert result == "Match"  # Rule output served in shadow mode
    assert len(router.prediction_history) == 1
    assert router.prediction_history[0]["rule_result"] == "Match"
    assert router.prediction_history[0]["ml_result"] == "Unmatch"
    assert router.prediction_history[0]["agreed"] is False


def test_promoted_mode_returns_ml_result():
    rule = MockRuleEngine(output="Match")
    ml = MockMlModel(output="Unmatch")
    router = ShadowModelRouter(rule_engine=rule, ml_model=ml, model_name="confidence_model", shadow_mode=False)

    result = router.predict({"spec": "RAM"})
    assert result == "Unmatch"  # ML output served when promoted


def test_agreement_rate_and_disagreements():
    rule = MockRuleEngine()
    ml = MockMlModel()
    router = ShadowModelRouter(rule_engine=rule, ml_model=ml, model_name="operator_model", shadow_mode=True)

    # 8 agreements
    for _ in range(8):
        rule.output = ">="
        ml.output = ">="
        router.predict({"test": 1})

    # 2 disagreements
    for _ in range(2):
        rule.output = ">="
        ml.output = "=="
        router.predict({"test": 2})

    stats = router.get_shadow_stats()
    assert stats["total_predictions"] == 10
    assert stats["agreement_count"] == 8
    assert stats["disagreement_count"] == 2
    assert stats["agreement_rate"] == 0.80
    assert len(stats["disagreements"]) == 2
