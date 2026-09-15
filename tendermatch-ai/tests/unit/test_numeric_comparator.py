import pytest
from app.matching.numeric_comparator import compare_numeric


def test_gte_vendor_exceeds_returns_match():
    res = compare_numeric(required_val=16.0, vendor_val=32.0, operator=">=")
    assert res["verdict"] == "match"
    assert res["method"] == "numeric_exact"
    assert res["confidence"] >= 0.90


def test_gte_vendor_below_returns_unmatch():
    res = compare_numeric(required_val=16.0, vendor_val=8.0, operator=">=")
    assert res["verdict"] == "unmatch"
    assert res["method"] == "numeric_exact"


def test_lte_vendor_below_returns_match():
    res = compare_numeric(required_val=2.0, vendor_val=1.5, operator="<=")
    assert res["verdict"] == "match"


def test_lte_vendor_exceeds_returns_unmatch():
    res = compare_numeric(required_val=2.0, vendor_val=3.0, operator="<=")
    assert res["verdict"] == "unmatch"


def test_eq_exact_match():
    res = compare_numeric(required_val=16.0, vendor_val=16.0, operator="==")
    assert res["verdict"] == "match"


def test_eq_not_equal_returns_unmatch():
    res = compare_numeric(required_val=16.0, vendor_val=32.0, operator="==")
    assert res["verdict"] == "unmatch"


def test_missing_vendor_value_returns_ai_err():
    res = compare_numeric(required_val=16.0, vendor_val=None, operator=">=")
    assert res["verdict"] == "uncertain"
    assert res["confidence"] <= 0.30
    assert res["method"] == "missing_value"


def test_unit_conversion_mb_to_gb():
    res = compare_numeric(
        required_val=16.0,
        vendor_val=16384.0,
        operator=">=",
        required_unit="GB",
        vendor_unit="MB",
    )
    assert res["verdict"] == "match"
    assert res["details"]["normalized_vendor_val"] == 16.0


def test_range_spec_within_returns_match():
    res = compare_numeric(
        required_val=None,
        vendor_val=12.0,
        operator=">=",
        min_val=8.0,
        max_val=16.0,
    )
    assert res["verdict"] == "match"


def test_confidence_high_for_clear_pass():
    res = compare_numeric(required_val=16.0, vendor_val=64.0, operator=">=")
    assert res["verdict"] == "match"
    assert res["confidence"] >= 0.90
