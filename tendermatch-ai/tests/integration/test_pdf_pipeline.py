import json
import pytest
from unittest.mock import patch
from app.pipeline.orchestrator import PdfPipelineOrchestrator, PipelineResult


@pytest.fixture
def orchestrator():
    return PdfPipelineOrchestrator()


def test_full_pipeline_text_pdf(orchestrator):
    sample_text = (
        "REQUEST FOR PROPOSAL (RFP) - IT HARDWARE TENDER\n"
        "Technical Requirements:\n"
        "Product: Enterprise Ultrabook\n"
        "RAM: 32GB DDR5\n"
        "Storage: 1TB NVMe SSD\n"
        "Weight: 1.3kg"
    )

    llm_output = json.dumps({
        "products": [
            {
                "product_name": "Enterprise Ultrabook",
                "product_category": "electronics",
                "specifications": [
                    {"spec_name": "RAM", "spec_value": "32GB", "unit": "GB", "raw_text": "RAM: 32GB DDR5"},
                    {"spec_name": "Storage", "spec_value": "1TB", "unit": "TB", "raw_text": "Storage: 1TB NVMe SSD"}
                ]
            }
        ]
    })

    with patch.object(orchestrator.text_extractor, "_extract_fitz_pages") as mock_extract, \
         patch.object(orchestrator.spec_extractor, "_call_llm", return_value=llm_output):

        mock_extract.return_value = [
            {"page_number": 1, "text": sample_text, "tables": []}
        ]

        result = orchestrator.run(b"%PDF-1.4 sample")

    assert isinstance(result, PipelineResult)
    assert result.status == "SUCCESS"
    assert result.doc_type == "requirement_tender"
    assert len(result.products) == 1

    prod = result.products[0]
    assert prod.product_name == "Enterprise Ultrabook"

    ram_spec = next((s for s in prod.specifications if s.raw_spec_name == "RAM"), None)
    assert ram_spec is not None
    assert ram_spec.canonical_spec_name == "ram_capacity"
    assert ram_spec.numeric_value == 32.0
    assert ram_spec.unit == "GB"
    assert ram_spec.is_numeric is True


def test_full_pipeline_scanned_pdf(orchestrator):
    # Text is sparse initially, OCR recovers text
    with patch.object(orchestrator.text_extractor, "_extract_fitz_pages") as mock_extract, \
         patch.object(orchestrator.text_extractor, "_run_ocr_on_page") as mock_ocr, \
         patch.object(orchestrator.spec_extractor, "_call_llm") as mock_llm:

        mock_extract.return_value = [
            {"page_number": 1, "text": "Scanned RFP", "tables": []}
        ]
        mock_ocr.return_value = "REQUEST FOR PROPOSAL - Technical Offer Laptop: RAM 16GB"
        mock_llm.return_value = json.dumps({
            "products": [
                {
                    "product_name": "Laptop",
                    "product_category": "electronics",
                    "specifications": [
                        {"spec_name": "RAM", "spec_value": "16GB", "unit": "GB", "raw_text": "RAM 16GB"}
                    ]
                }
            ]
        })

        result = orchestrator.run(b"%PDF-1.4 scanned")

    assert result.status == "SUCCESS"
    assert len(result.products) == 1
    assert result.products[0].specifications[0].numeric_value == 16.0


def test_pipeline_error_recovery(orchestrator):
    # Empty bytes input
    empty_res = orchestrator.run(b"")
    assert empty_res.status == "EMPTY_DOCUMENT"
    assert len(empty_res.products) == 0
