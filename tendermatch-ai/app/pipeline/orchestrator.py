import logging
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field

from app.pipeline.text_extractor import TextExtractor, PageResult
from app.pipeline.doc_classifier import DocumentClassifier, ClassificationResult
from app.pipeline.spec_extractor import SpecExtractor, ExtractedProduct, ExtractedSpec
from app.pipeline.ner_crosscheck import NerCrossChecker
from app.pipeline.spec_normalizer import SpecNormalizer, NormalizedSpecKey
from app.pipeline.value_parser import ValueParser, ParsedValue
from app.pipeline.product_matcher import ProductMatcher

logger = logging.getLogger("tendermatch-ai.orchestrator")


class EnrichedSpec(BaseModel):
    raw_spec_name: str
    canonical_spec_name: str
    spec_value: str
    numeric_value: Optional[float] = None
    min_value: Optional[float] = None
    max_value: Optional[float] = None
    unit: Optional[str] = None
    is_numeric: bool = False
    raw_text: str = ""
    extraction_confidence: str = "high"
    extraction_source: str = "llm"
    normalization_confidence: str = "high"
    matched_tender_spec_id: Optional[str] = None


class EnrichedProduct(BaseModel):
    product_name: str
    product_category: str = "electronics"
    matched_tender_product_id: Optional[str] = None
    match_confidence: Optional[float] = None
    raw_text: str = ""
    specifications: List[EnrichedSpec] = Field(default_factory=list)


class PipelineResult(BaseModel):
    status: str
    doc_type: str
    classification_confidence: float
    page_count: int
    products: List[EnrichedProduct] = Field(default_factory=list)
    raw_text_sample: str = ""


class PdfPipelineOrchestrator:
    def __init__(self):
        self.text_extractor = TextExtractor()
        self.doc_classifier = DocumentClassifier()
        self.spec_extractor = SpecExtractor()
        self.ner_checker = NerCrossChecker()
        self.normalizer = SpecNormalizer()
        self.value_parser = ValueParser()
        self.product_matcher = ProductMatcher()

    def run(self,
            pdf_bytes: bytes,
            requirement_products: Optional[List[Dict[str, Any]]] = None) -> PipelineResult:
        if not pdf_bytes:
            return PipelineResult(
                status="EMPTY_DOCUMENT",
                doc_type="unknown",
                classification_confidence=0.0,
                page_count=0,
                products=[]
            )

        # 1. Text Extraction
        pages = self.text_extractor.extract(pdf_bytes)
        combined_text = "\n\n".join([p.text for p in pages if p.text])

        # 2. Document Classification (using first 2 pages)
        first_pages_text = "\n\n".join([p.text for p in pages[:2]])
        classification = self.doc_classifier.classify(first_pages_text or combined_text)

        # 3. LLM Spec Extraction
        raw_products = self.spec_extractor.extract(combined_text)

        # 4. NER Cross-Check
        checked_products = self.ner_checker.cross_check(raw_products, combined_text)

        # 5. Spec Normalization and Value Parsing
        enriched_products: List[EnrichedProduct] = []

        for p in checked_products:
            enriched_specs: List[EnrichedSpec] = []

            for s in p.specifications:
                # Normalization
                norm_res = self.normalizer.normalize(s.spec_name, original_value=s.spec_value)

                # Value & Unit Parsing
                parsed_val = self.value_parser.parse(s.spec_value, explicit_unit=s.unit)

                enriched_specs.append(EnrichedSpec(
                    raw_spec_name=s.spec_name,
                    canonical_spec_name=norm_res.canonical_name,
                    spec_value=norm_res.preserved_value or s.spec_value,
                    numeric_value=parsed_val.numeric_value if parsed_val else None,
                    min_value=parsed_val.min_value if parsed_val else None,
                    max_value=parsed_val.max_value if parsed_val else None,
                    unit=parsed_val.normalized_unit if parsed_val else s.unit,
                    is_numeric=parsed_val.is_numeric if parsed_val else False,
                    raw_text=s.raw_text,
                    extraction_confidence=s.extraction_confidence,
                    extraction_source=s.extraction_source,
                    normalization_confidence=norm_res.confidence
                ))

            # 6. Product Matching (if requirement products provided)
            matched_prod_name = None
            matched_prod_id = None
            match_conf = None

            if requirement_products:
                req_obj_list = [
                    ExtractedProduct(
                        product_name=rp.get("product_name", ""),
                        product_category=rp.get("product_category", "electronics")
                    )
                    for rp in requirement_products
                ]
                match_res = self.product_matcher.match_product(p, req_obj_list)
                if match_res:
                    matched_prod_name = match_res.matched_product_name
                    match_conf = match_res.confidence
                    for rp in requirement_products:
                        if rp.get("product_name") == matched_prod_name:
                            matched_prod_id = rp.get("id")
                            # Match specs within product
                            req_specs = rp.get("specifications", [])
                            for es in enriched_specs:
                                v_spec_dict = {
                                    "spec_name": es.raw_spec_name,
                                    "canonical_spec_name": es.canonical_spec_name
                                }
                                m_spec = self.product_matcher.match_spec(v_spec_dict, req_specs)
                                if m_spec:
                                    es.matched_tender_spec_id = m_spec.matched_spec_id
                            break

            enriched_products.append(EnrichedProduct(
                product_name=p.product_name,
                product_category=p.product_category,
                matched_tender_product_id=matched_prod_id,
                match_confidence=match_conf,
                raw_text=p.raw_text,
                specifications=enriched_specs
            ))

        return PipelineResult(
            status="SUCCESS",
            doc_type=classification.doc_type,
            classification_confidence=classification.confidence,
            page_count=len(pages),
            products=enriched_products,
            raw_text_sample=combined_text[:300] if combined_text else ""
        )
