import io
import logging
from typing import List, Dict, Any, Optional
from pydantic import BaseModel
import numpy as np

logger = logging.getLogger("tendermatch-ai.text_extractor")


class ExtractionError(Exception):
    """Raised when PDF extraction encounters an unrecoverable failure."""
    pass


class PageResult(BaseModel):
    page_number: int
    text: str
    has_native_text: bool = True
    tables: List[List[List[Optional[str]]]] = []


class TextExtractor:
    def __init__(self, min_char_threshold: int = 50):
        self.min_char_threshold = min_char_threshold

    def extract(self, pdf_bytes: bytes) -> List[PageResult]:
        if not pdf_bytes:
            return []

        try:
            raw_pages = self._extract_fitz_pages(pdf_bytes)
        except Exception as e:
            logger.error(f"Failed to parse PDF bytes: {e}")
            raise ExtractionError(f"Corrupt or invalid PDF: {e}") from e

        results: List[PageResult] = []
        for page_data in raw_pages:
            page_num = page_data["page_number"]
            text = (page_data.get("text") or "").strip()
            tables = page_data.get("tables", [])

            if len(text) < self.min_char_threshold:
                ocr_text = self._run_ocr_on_page(page_data)
                if ocr_text:
                    text = ocr_text.strip()
                    has_native = False
                else:
                    has_native = len(text) > 0
            else:
                has_native = True

            results.append(PageResult(
                page_number=page_num,
                text=text,
                has_native_text=has_native,
                tables=tables
            ))

        return results

    def _extract_fitz_pages(self, pdf_bytes: bytes) -> List[Dict[str, Any]]:
        """Extract pages and tables using PyMuPDF (fitz) if installed, or fallback."""
        if not pdf_bytes.startswith(b"%PDF"):
            text = pdf_bytes.decode("utf-8", errors="ignore")
            return [{
                "page_number": 1,
                "text": text,
                "tables": []
            }]

        try:
            import fitz  # PyMuPDF
            doc = fitz.open(stream=pdf_bytes, filetype="pdf")
            pages = []
            for i, page in enumerate(doc):
                text = page.get_text("text")
                tables = []
                try:
                    tabs = page.find_tables()
                    if tabs and tabs.tables:
                        for tab in tabs.tables:
                            raw_table = tab.extract()
                            clean_table = [
                                [str(cell).strip() if cell is not None else "" for cell in row]
                                for row in raw_table
                            ]
                            tables.append(clean_table)
                except Exception:
                    pass

                pages.append({
                    "page_number": i + 1,
                    "text": text,
                    "tables": tables,
                    "_page_obj": page,
                })
            return pages
        except ImportError:
            # Fallback using pypdf if fitz is not yet installed
            try:
                import pypdf
                reader = pypdf.PdfReader(io.BytesIO(pdf_bytes))
                pages = []
                for i, p in enumerate(reader.pages):
                    pages.append({
                        "page_number": i + 1,
                        "text": p.extract_text() or "",
                        "tables": [],
                    })
                return pages
            except Exception as pe:
                logger.warning(f"pypdf extraction fallback error: {pe}")
                raw_text = pdf_bytes.decode("latin-1", errors="ignore")
                return [{
                    "page_number": 1,
                    "text": raw_text[:2000] if len(raw_text) > 50 else "",
                    "tables": []
                }]

    def preprocess_image_for_ocr(self, img_array: np.ndarray) -> np.ndarray:
        """Preprocess page image using OpenCV (grayscale, Otsu binarization)."""
        try:
            import cv2
            if len(img_array.shape) == 3:
                gray = cv2.cvtColor(img_array, cv2.COLOR_BGR2GRAY)
            else:
                gray = img_array
            _, binarized = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
            return binarized
        except Exception as e:
            logger.warning(f"Image preprocessing failed: {e}")
            if len(img_array.shape) == 3:
                return np.mean(img_array, axis=2).astype(np.uint8)
            return img_array

    def _run_ocr_on_page(self, page_data: Dict[str, Any]) -> str:
        """Render page and run OCR using pytesseract."""
        try:
            import pytesseract
            page_obj = page_data.get("_page_obj")
            if page_obj:
                pix = page_obj.get_pixmap(dpi=300)
                img = np.frombuffer(pix.samples, dtype=np.uint8).reshape(pix.h, pix.w, pix.n)
                processed = self.preprocess_image_for_ocr(img)
                return pytesseract.image_to_string(processed)
        except Exception as e:
            logger.info(f"Tesseract OCR fallback skipped or unavailable: {e}")
        return ""
