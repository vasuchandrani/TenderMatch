import pytest
from app.pipeline.two_pass_service import two_pass_service


def test_two_pass_service_pass1():
    reqs = [
        {"id": "req-1", "raw_spec_name": "RAM", "canonical_spec_name": "RAM", "spec_value": "32 GB", "numeric_value": 32, "unit": "GB"},
        {"id": "req-2", "raw_spec_name": "Storage", "canonical_spec_name": "Storage", "spec_value": "1 TB", "numeric_value": 1000, "unit": "GB"},
    ]
    vens = [
        {"id": "ven-1", "raw_spec_name": "RAM", "canonical_spec_name": "RAM", "spec_value": "64 GB", "numeric_value": 64, "unit": "GB"},
        {"id": "ven-2", "raw_spec_name": "Storage", "canonical_spec_name": "Storage", "spec_value": "512 GB", "numeric_value": 512, "unit": "GB"},
    ]

    results = two_pass_service.execute_pass_1(reqs, vens)
    assert len(results) == 2
    assert results[0]["spec_name"] == "RAM"
    assert results[0]["system_verdict"] == "Match"  # 64 >= 32
    assert results[1]["spec_name"] == "Storage"
    assert results[1]["system_verdict"] == "Unmatch"  # 512 < 1000


def test_two_pass_service_pass2_reflection():
    reviewed = [
        {
            "id": "match-1",
            "spec_name": "RAM",
            "system_verdict": "Unmatch",
            "human_verdict": "Match",
            "required_value": "32 GB DDR5",
            "offered_value": "32 GB DDR5 ECC",
            "reviewer_comment": "ECC memory exceeds standard specification requirements",
        }
    ]

    certified = two_pass_service.execute_pass_2_reflection(reviewed)
    assert len(certified) == 1
    item = certified[0]
    assert item["certified_verdict"] == "Match"
    assert item["is_reflection_certified"] is True
    assert item["is_override"] is True
    assert "Verified human override" in item["reflection_audit_note"] or "certified" in item["reflection_audit_note"].lower()


def test_two_pass_criticality_reasoning():
    specs = ["RAM Capacity", "Processor Cores", "Chassis Color", "Weight"]
    criticality = two_pass_service.evaluate_spec_criticality(specs)

    assert "RAM Capacity" in criticality
    assert "Chassis Color" in criticality
    # Critical specs have higher weight than cosmetic specs
    assert criticality["RAM Capacity"]["weight"] > criticality["Chassis Color"]["weight"]
    assert criticality["RAM Capacity"]["category"] == "CRITICAL"
    assert criticality["Chassis Color"]["category"] == "COSMETIC"

