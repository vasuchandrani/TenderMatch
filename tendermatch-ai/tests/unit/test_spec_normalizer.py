import pytest
from app.pipeline.spec_normalizer import SpecNormalizer, NormalizedSpecKey


@pytest.fixture
def normalizer():
    return SpecNormalizer()


def test_normalize_exact_synonym(normalizer):
    res = normalizer.normalize("SSD")
    assert isinstance(res, NormalizedSpecKey)
    assert res.canonical_name == "storage_capacity"
    assert res.confidence == "high"


def test_normalize_semantic_synonym(normalizer):
    res = normalizer.normalize("Internal Storage")
    assert res.canonical_name == "storage_capacity"
    assert res.confidence in ["high", "medium"]


def test_medium_confidence_flag(normalizer):
    # Somewhat related phrase
    res = normalizer.normalize("Disk drive volume space")
    assert res.confidence in ["medium", "high"]
    assert res.canonical_name == "storage_capacity"


def test_low_confidence_keeps_original(normalizer):
    # Completely unknown attribute
    res = normalizer.normalize("Xylophone Harmonic Resonance Index")
    assert res.canonical_name == "xylophone_harmonic_resonance_index"
    assert res.confidence == "low"


def test_ddr4_and_ddr5_not_merged(normalizer):
    # CRITICAL: DDR4 and DDR5 normalize to canonical key ram_type, but the original values must NOT be altered
    res4 = normalizer.normalize("RAM Type", original_value="DDR4")
    res5 = normalizer.normalize("RAM Type", original_value="DDR5")

    assert res4.canonical_name == "ram_type"
    assert res5.canonical_name == "ram_type"
    assert res4.preserved_value == "DDR4"
    assert res5.preserved_value == "DDR5"
    assert res4.preserved_value != res5.preserved_value


def test_ssd_and_internal_storage_unified(normalizer):
    res1 = normalizer.normalize("SSD")
    res2 = normalizer.normalize("Internal Storage")
    res3 = normalizer.normalize("Solid State Drive")

    assert res1.canonical_name == res2.canonical_name == res3.canonical_name == "storage_capacity"
