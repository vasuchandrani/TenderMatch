import pytest
from app.matching.operator_inference import OperatorInference


def test_ram_capacity_returns_gte():
    op = OperatorInference.infer("ram_capacity", is_numeric=True)
    assert op == ">="


def test_weight_returns_lte():
    op = OperatorInference.infer("weight", is_numeric=True)
    assert op == "<="


def test_ram_type_returns_exact():
    op = OperatorInference.infer("ram_type", is_numeric=False)
    assert op == "=="


def test_features_returns_contains():
    op = OperatorInference.infer("features", is_numeric=False)
    assert op == "contains"


def test_unknown_numeric_defaults_gte():
    op = OperatorInference.infer("custom_turbo_boost", is_numeric=True)
    assert op == ">="


def test_unknown_text_defaults_semantic():
    op = OperatorInference.infer("company_ethics_policy", is_numeric=False)
    assert op == "semantic"
