import re
from typing import List
from pydantic import BaseModel


class ClassificationResult(BaseModel):
    doc_type: str  # 'requirement_tender', 'vendor_response', 'unknown'
    confidence: float
    matched_keywords: List[str] = []


class DocumentClassifier:
    REQUIREMENT_KEYWORDS = [
        "request for proposal", "rfp", "tender notice", "invitation to bid",
        "technical requirements", "minimum specifications", "scope of work",
        "terms of reference", "must meet", "shall comply", "mandatory requirements",
        "bid evaluation criteria", "eligibility criteria"
    ]

    VENDOR_KEYWORDS = [
        "technical offer", "bid response", "commercial proposal",
        "compliance matrix", "we propose", "our solution", "quotation",
        "part number", "model:", "datasheet", "manufacturer authorization",
        "authorized partner", "bidder submission"
    ]

    def classify(self, text: str) -> ClassificationResult:
        if not text:
            return ClassificationResult(doc_type="unknown", confidence=0.0, matched_keywords=[])

        text_lower = text.lower()

        req_matches = [kw for kw in self.REQUIREMENT_KEYWORDS if re.search(r'\b' + re.escape(kw) + r'\b', text_lower)]
        vendor_matches = [kw for kw in self.VENDOR_KEYWORDS if re.search(r'\b' + re.escape(kw) + r'\b', text_lower)]

        req_score = len(req_matches)
        vendor_score = len(vendor_matches)

        if req_score == 0 and vendor_score == 0:
            return ClassificationResult(doc_type="unknown", confidence=0.1, matched_keywords=[])

        if req_score >= vendor_score:
            # Scale confidence between 0.5 and 0.95 based on keyword count
            confidence = min(0.95, 0.50 + (req_score * 0.12))
            return ClassificationResult(
                doc_type="requirement_tender",
                confidence=round(confidence, 2),
                matched_keywords=req_matches
            )
        else:
            confidence = min(0.95, 0.50 + (vendor_score * 0.12))
            return ClassificationResult(
                doc_type="vendor_response",
                confidence=round(confidence, 2),
                matched_keywords=vendor_matches
            )
