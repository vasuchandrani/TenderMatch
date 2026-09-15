import json
import pytest
from unittest.mock import patch, MagicMock
from app.pipeline.spec_extractor import SpecExtractor, ExtractedProduct, ExtractedSpec


@pytest.fixture
def extractor():
    return SpecExtractor()


def test_extract_products_from_text(extractor):
    sample_llm_json = json.dumps({
        "products": [
            {
                "product_name": "Standard Office Laptop",
                "product_category": "electronics",
                "specifications": [
                    {"spec_name": "RAM", "spec_value": "16GB", "unit": "GB", "raw_text": "RAM: 16GB DDR5"},
                    {"spec_name": "Storage", "spec_value": "512GB", "unit": "GB", "raw_text": "SSD 512GB NVMe"}
                ]
            }
        ]
    })

    with patch.object(extractor, "_call_llm", return_value=sample_llm_json):
        products = extractor.extract("Sample text for laptop RFP...")

    assert len(products) == 1
    assert isinstance(products[0], ExtractedProduct)
    assert products[0].product_name == "Standard Office Laptop"
    assert products[0].product_category == "electronics"
    assert len(products[0].specifications) == 2


def test_extract_specs_per_product(extractor):
    sample_llm_json = json.dumps({
        "products": [
            {
                "product_name": "Laser Printer",
                "product_category": "electronics",
                "specifications": [
                    {"spec_name": "Print Speed", "spec_value": "40", "unit": "ppm", "raw_text": "Print Speed: 40 ppm"}
                ]
            }
        ]
    })

    with patch.object(extractor, "_call_llm", return_value=sample_llm_json):
        products = extractor.extract("Printer specs")

    spec = products[0].specifications[0]
    assert isinstance(spec, ExtractedSpec)
    assert spec.spec_name == "Print Speed"
    assert spec.spec_value == "40"
    assert spec.unit == "ppm"


def test_handles_malformed_llm_json(extractor):
    bad_json = "Here is your response: ```json\n{'products': [{'product_name': 'Server'}]}\n```"
    fixed_json = json.dumps({"products": [{"product_name": "Server", "specifications": []}]})

    # First call returns bad formatting, clean/retry returns fixed JSON
    with patch.object(extractor, "_call_llm", side_effect=[bad_json, fixed_json]):
        products = extractor.extract("Server spec text")

    assert len(products) == 1
    assert products[0].product_name == "Server"


def test_chunks_long_documents(extractor):
    # Long text exceeding chunk size
    long_text = "Page content. " * 5000
    mock_response = json.dumps({"products": [{"product_name": "Item A", "specifications": []}]})

    with patch.object(extractor, "_call_llm", return_value=mock_response) as mock_call:
        products = extractor.extract(long_text, max_chunk_chars=4000)

    assert mock_call.call_count > 1
    assert len(products) >= 1


def test_preserves_raw_text_snippets(extractor):
    sample_llm_json = json.dumps({
        "products": [
            {
                "product_name": "Laptop",
                "specifications": [
                    {"spec_name": "Battery", "spec_value": "65", "unit": "Wh", "raw_text": "Battery capacity must be at least 65Wh"}
                ]
            }
        ]
    })

    with patch.object(extractor, "_call_llm", return_value=sample_llm_json):
        products = extractor.extract("Battery capacity must be at least 65Wh")

    assert products[0].specifications[0].raw_text == "Battery capacity must be at least 65Wh"


def test_llm_timeout_triggers_fallback(extractor):
    fallback_json = json.dumps({"products": [{"product_name": "Fallback Laptop", "specifications": []}]})
    extractor.fallback_model = "test-fallback-model"

    def mock_call(prompt, model):
        if model == extractor.primary_model:
            raise TimeoutError("Ollama primary model timed out")
        return fallback_json

    with patch("app.pipeline.spec_extractor.settings.GEMINI_API_KEY", ""), \
         patch("app.pipeline.spec_extractor.settings.GROQ_API_KEY", ""), \
         patch.object(extractor, "_invoke_model", side_effect=mock_call):
        products = extractor.extract("Specs...")

    assert len(products) == 1
    assert products[0].product_name == "Fallback Laptop"


def test_empty_text_returns_empty_products(extractor):
    products = extractor.extract("")
    assert products == []
