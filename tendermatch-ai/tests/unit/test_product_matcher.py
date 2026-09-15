import pytest
from app.pipeline.product_matcher import ProductMatcher, MatchedProduct, MatchedSpec
from app.pipeline.spec_extractor import ExtractedProduct, ExtractedSpec


@pytest.fixture
def matcher():
    return ProductMatcher()


def test_match_identical_products(matcher):
    req_product = ExtractedProduct(product_name="Office Laptop", product_category="electronics")
    vendor_product = ExtractedProduct(product_name="Office Laptop", product_category="electronics")

    matched = matcher.match_product(vendor_product, [req_product])
    assert matched is not None
    assert matched.matched_product_name == "Office Laptop"
    assert matched.confidence >= 0.90


def test_match_similar_products(matcher):
    req_product = ExtractedProduct(product_name="Executive Laptop", product_category="electronics")
    vendor_product = ExtractedProduct(product_name="Lenovo ThinkPad Laptop", product_category="electronics")

    matched = matcher.match_product(vendor_product, [req_product])
    assert matched is not None
    assert matched.confidence >= 0.40


def test_no_match_unrelated(matcher):
    req_product = ExtractedProduct(product_name="Enterprise Laptop", product_category="electronics")
    vendor_product = ExtractedProduct(product_name="Ergonomic Mesh Chair", product_category="furniture")

    matched = matcher.match_product(vendor_product, [req_product], threshold=0.60)
    assert matched is None


def test_spec_matching_by_canonical(matcher):
    req_spec = {"spec_name": "Memory", "canonical_spec_name": "ram_capacity", "spec_value": "16GB"}
    vendor_spec = {"spec_name": "RAM", "canonical_spec_name": "ram_capacity", "spec_value": "32GB"}

    res = matcher.match_spec(vendor_spec, [req_spec])
    assert res is not None
    assert res.matched_spec_name == "Memory"
    assert res.confidence == 1.0


def test_spec_matching_embedding_fallback(matcher):
    req_spec = {"spec_name": "Fingerprint Reader Sensor", "canonical_spec_name": "biometric_fingerprint", "spec_value": "Yes"}
    vendor_spec = {"spec_name": "Biometric Fingerprint Scanner", "canonical_spec_name": "fingerprint_sensor", "spec_value": "Included"}

    res = matcher.match_spec(vendor_spec, [req_spec])
    assert res is not None
    assert res.confidence >= 0.50
