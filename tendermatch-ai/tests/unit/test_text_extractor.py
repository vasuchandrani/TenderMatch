import pytest
import numpy as np
from unittest.mock import MagicMock, patch
from app.pipeline.text_extractor import TextExtractor, ExtractionError, PageResult


@pytest.fixture
def extractor():
    return TextExtractor()


def test_extract_text_from_native_pdf(extractor):
    fake_pdf_bytes = b"%PDF-1.4 dummy native content"
    with patch.object(extractor, "_extract_fitz_pages") as mock_fitz:
        mock_fitz.return_value = [
            {"page_number": 1, "text": "Technical Specifications for Office Laptops\nProcessor: Intel Core i7", "tables": []}
        ]
        results = extractor.extract(fake_pdf_bytes)

    assert len(results) == 1
    assert results[0].page_number == 1
    assert "Intel Core i7" in results[0].text
    assert results[0].has_native_text is True


def test_extract_tables_from_pdf(extractor):
    fake_pdf_bytes = b"%PDF-1.4 table content"
    sample_table = [["Item", "Specification"], ["RAM", "16GB DDR5"]]
    with patch.object(extractor, "_extract_fitz_pages") as mock_fitz:
        mock_fitz.return_value = [
            {"page_number": 1, "text": "Specifications Table", "tables": [sample_table]}
        ]
        results = extractor.extract(fake_pdf_bytes)

    assert len(results[0].tables) == 1
    assert results[0].tables[0][1][1] == "16GB DDR5"


def test_fallback_to_ocr_when_text_sparse(extractor):
    fake_pdf_bytes = b"%PDF-1.4 scanned image"
    with patch.object(extractor, "_extract_fitz_pages") as mock_fitz, \
         patch.object(extractor, "_run_ocr_on_page") as mock_ocr:
        # Page with sparse text (< 50 chars)
        mock_fitz.return_value = [
            {"page_number": 1, "text": "Scanned", "tables": []}
        ]
        mock_ocr.return_value = "Recovered text via Tesseract OCR: Minimum RAM 32 GB"

        results = extractor.extract(fake_pdf_bytes)

    assert len(results) == 1
    assert "Minimum RAM 32 GB" in results[0].text
    assert results[0].has_native_text is False
    assert mock_ocr.called


def test_ocr_with_image_preprocessing(extractor):
    dummy_img = np.zeros((100, 100, 3), dtype=np.uint8)
    processed = extractor.preprocess_image_for_ocr(dummy_img)

    assert processed is not None
    assert len(processed.shape) == 2  # Grayscale/binarized image


def test_returns_page_metadata(extractor):
    fake_pdf_bytes = b"%PDF-1.4 content"
    with patch.object(extractor, "_extract_fitz_pages") as mock_fitz:
        mock_fitz.return_value = [
            {"page_number": 1, "text": "Detailed page content with sufficient characters to pass the threshold.", "tables": []},
            {"page_number": 2, "text": "Second page content also exceeding the 50 characters threshold limit.", "tables": []}
        ]
        results = extractor.extract(fake_pdf_bytes)

    assert len(results) == 2
    assert results[0].page_number == 1
    assert results[1].page_number == 2
    assert isinstance(results[0], PageResult)


def test_handles_corrupt_pdf_gracefully(extractor):
    corrupt_bytes = b"not a valid pdf header"
    with patch.object(extractor, "_extract_fitz_pages", side_effect=Exception("Invalid PDF")):
        with pytest.raises(ExtractionError):
            extractor.extract(corrupt_bytes)


def test_handles_empty_pdf(extractor):
    with patch.object(extractor, "_extract_fitz_pages", return_value=[]):
        results = extractor.extract(b"%PDF-1.4 empty")
        assert results == []
