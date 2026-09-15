import pytest
from app.matching.matcher import SpecMatcher, MatchRequestItem, MatchBatchRequest


@pytest.fixture
def matcher():
    return SpecMatcher()


def test_numeric_specs_batch_matching(matcher):
    pairs = [
        MatchRequestItem(
            pair_id="p1",
            requirement={
                "spec_name": "RAM",
                "canonical_name": "ram_capacity",
                "value": "16",
                "numeric_value": 16.0,
                "unit": "GB",
                "is_numeric": True,
            },
            vendor={
                "spec_name": "RAM",
                "canonical_name": "ram_capacity",
                "value": "32",
                "numeric_value": 32.0,
                "unit": "GB",
                "is_numeric": True,
            },
        ),
        MatchRequestItem(
            pair_id="p2",
            requirement={
                "spec_name": "Weight",
                "canonical_name": "weight",
                "value": "2.0",
                "numeric_value": 2.0,
                "unit": "kg",
                "is_numeric": True,
            },
            vendor={
                "spec_name": "Weight",
                "canonical_name": "weight",
                "value": "1.5",
                "numeric_value": 1.5,
                "unit": "kg",
                "is_numeric": True,
            },
        ),
        MatchRequestItem(
            pair_id="p3",
            requirement={
                "spec_name": "Storage",
                "canonical_name": "storage_capacity",
                "value": "512",
                "numeric_value": 512.0,
                "unit": "GB",
                "is_numeric": True,
            },
            vendor={
                "spec_name": "SSD",
                "canonical_name": "storage_capacity",
                "value": "256",
                "numeric_value": 256.0,
                "unit": "GB",
                "is_numeric": True,
            },
        ),
    ]

    request = MatchBatchRequest(
        tender_id="t1",
        vendor_submission_id="v1",
        spec_pairs=pairs,
    )

    response = matcher.match_batch(request)
    assert len(response.results) == 3

    r1 = next(r for r in response.results if r.pair_id == "p1")
    assert r1.verdict == "Match"
    assert r1.operator == ">="
    assert r1.method == "numeric_exact"

    r2 = next(r for r in response.results if r.pair_id == "p2")
    assert r2.verdict == "Match"
    assert r2.operator == "<="

    r3 = next(r for r in response.results if r.pair_id == "p3")
    assert r3.verdict == "Unmatch"


def test_mixed_spec_types_matching(matcher):
    pairs = [
        # Numeric
        MatchRequestItem(
            pair_id="m1",
            requirement={"spec_name": "CPU Speed", "canonical_name": "processor_speed", "numeric_value": 2.5, "is_numeric": True, "unit": "GHz"},
            vendor={"spec_name": "Base Clock", "canonical_name": "processor_speed", "numeric_value": 3.0, "is_numeric": True, "unit": "GHz"},
        ),
        # Exact Text (DDR4 vs DDR5 - must remain unmatch)
        MatchRequestItem(
            pair_id="m2",
            requirement={"spec_name": "Memory Type", "canonical_name": "ram_type", "value": "DDR5", "is_numeric": False},
            vendor={"spec_name": "RAM Standard", "canonical_name": "ram_type", "value": "DDR4", "is_numeric": False},
        ),
        # Contains Operator
        MatchRequestItem(
            pair_id="m3",
            requirement={"spec_name": "Compliance", "canonical_name": "features", "value": "TPM 2.0", "is_numeric": False},
            vendor={"spec_name": "Security", "canonical_name": "features", "value": "Integrated TPM 2.0 and Fingerprint Reader", "is_numeric": False},
        ),
    ]

    request = MatchBatchRequest(tender_id="t1", vendor_submission_id="v1", spec_pairs=pairs)
    response = matcher.match_batch(request)

    res_map = {r.pair_id: r for r in response.results}
    assert res_map["m1"].verdict == "Match"
    assert res_map["m2"].verdict == "Unmatch"
    assert res_map["m2"].operator == "=="
    assert res_map["m3"].verdict == "Match"


def test_missing_specs_handled(matcher):
    pairs = [
        MatchRequestItem(
            pair_id="miss1",
            requirement={"spec_name": "Dedicated GPU", "canonical_name": "gpu", "value": "RTX 4060", "is_numeric": False},
            vendor=None,  # Vendor completely omitted this requirement
        )
    ]

    request = MatchBatchRequest(tender_id="t1", vendor_submission_id="v1", spec_pairs=pairs)
    response = matcher.match_batch(request)

    res = response.results[0]
    assert res.verdict == "Unmatch"
    assert res.method == "missing"
    assert res.confidence == 1.0
