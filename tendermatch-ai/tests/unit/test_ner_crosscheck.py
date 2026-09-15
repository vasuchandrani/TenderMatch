import pytest
from app.pipeline.ner_crosscheck import NerCrossChecker
from app.pipeline.spec_extractor import ExtractedProduct, ExtractedSpec


@pytest.fixture
def checker():
    return NerCrossChecker()


def test_ner_finds_spec_values_in_text(checker):
    text = "The machine includes 16GB DDR5 memory and 512GB NVMe SSD."
    entities = checker.extract_entities(text)

    values = [e["text"].lower() for e in entities if e["label"] == "SPEC_VALUE"]
    assert any("16gb" in v for v in values)
    assert any("512gb" in v for v in values)


def test_ner_finds_spec_names(checker):
    text = "RAM: 16GB DDR5. Storage: 512GB. Processor: Intel Core i7."
    entities = checker.extract_entities(text)

    names = [e["text"].lower() for e in entities if e["label"] == "SPEC_NAME"]
    assert "ram" in names
    assert "storage" in names
    assert "processor" in names


def test_flags_llm_hallucination(checker):
    raw_text = "System contains 16GB RAM."
    # LLM hallucinated a GPU spec not present in the document
    product = ExtractedProduct(
        product_name="Workstation",
        specifications=[
            ExtractedSpec(spec_name="RAM", spec_value="16GB", raw_text="16GB RAM"),
            ExtractedSpec(spec_name="GPU", spec_value="Nvidia RTX 4090", raw_text="Nvidia RTX 4090")
        ]
    )

    checked_products = checker.cross_check([product], raw_text)
    specs = checked_products[0].specifications

    ram_spec = next(s for s in specs if s.spec_name == "RAM")
    gpu_spec = next(s for s in specs if s.spec_name == "GPU")

    assert ram_spec.extraction_confidence == "high"
    assert gpu_spec.extraction_confidence == "low"


def test_adds_ner_only_specs(checker):
    raw_text = "Standard notebook. Battery: 65Wh Lithium-Ion. Weight: 1.4kg."
    # LLM only extracted Battery, missed Weight
    product = ExtractedProduct(
        product_name="Notebook",
        specifications=[
            ExtractedSpec(spec_name="Battery", spec_value="65Wh", raw_text="Battery: 65Wh")
        ]
    )

    checked_products = checker.cross_check([product], raw_text)
    specs = checked_products[0].specifications

    weight_spec = next((s for s in specs if "weight" in s.spec_name.lower()), None)
    assert weight_spec is not None
    assert weight_spec.extraction_source == "ner_only"


def test_confirms_both_sources(checker):
    raw_text = "RAM: 32GB DDR5. Screen: 15.6 inch FHD."
    product = ExtractedProduct(
        product_name="Laptop",
        specifications=[
            ExtractedSpec(spec_name="RAM", spec_value="32GB", raw_text="RAM: 32GB DDR5")
        ]
    )

    checked_products = checker.cross_check([product], raw_text)
    ram_spec = checked_products[0].specifications[0]

    assert ram_spec.extraction_confidence == "high"
    assert ram_spec.extraction_source in ["both", "llm"]
