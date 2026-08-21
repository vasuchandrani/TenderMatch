import logging
from typing import Optional, List, Dict, Any
from fastapi import FastAPI, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from app.config import settings
from app.pipeline.orchestrator import PdfPipelineOrchestrator, PipelineResult
from app.pipeline.doc_classifier import DocumentClassifier
from app.pipeline.spec_normalizer import SpecNormalizer
from app.pipeline.value_parser import ValueParser

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("tendermatch-ai")

app = FastAPI(
    title="TenderMatch AI Microservice",
    description="AI/ML service for OCR, document classification, spec extraction, and matching.",
    version="2.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

pipeline_orchestrator = PdfPipelineOrchestrator()
doc_classifier = DocumentClassifier()
spec_normalizer = SpecNormalizer()
value_parser = ValueParser()


class ProcessDocumentRequest(BaseModel):
    minio_file_key: str
    document_type_hint: Optional[str] = "requirement"  # 'requirement' or 'vendor'
    tender_id: Optional[str] = None
    submission_id: Optional[str] = None
    file_bytes_base64: Optional[str] = None
    requirement_products: Optional[List[Dict[str, Any]]] = None


class ProcessDocumentResponse(BaseModel):
    status: str
    message: str
    minio_file_key: str
    document_type: str
    products: List[Dict[str, Any]] = []


@app.get("/health")
async def health_check():
    """Health check endpoint for container readiness and liveness."""
    if settings.GEMINI_API_KEY and settings.GEMINI_API_KEY.strip():
        provider = "google_gemini_cloud"
        model = settings.GEMINI_PRIMARY_MODEL
    elif settings.GROQ_API_KEY and settings.GROQ_API_KEY.strip():
        provider = "groq_cloud"
        model = settings.GROQ_PRIMARY_MODEL
    else:
        provider = "ollama_local"
        model = settings.PRIMARY_LLM_MODEL

    return {
        "status": "healthy",
        "service": settings.SERVICE_NAME,
        "llm_provider": provider,
        "primary_model": model,
        "ollama_endpoint": settings.OLLAMA_API_BASE,
    }


@app.post("/api/v1/classify-document")
async def classify_document(payload: Dict[str, str] = Body(...)):
    text = payload.get("text", "")
    return doc_classifier.classify(text)


@app.post("/api/v1/normalize-spec-key")
async def normalize_spec_key(payload: Dict[str, str] = Body(...)):
    spec_name = payload.get("spec_name", "")
    spec_val = payload.get("spec_value")
    return spec_normalizer.normalize(spec_name, original_value=spec_val)


@app.post("/api/v1/parse-value")
async def parse_value(payload: Dict[str, str] = Body(...)):
    raw_val = payload.get("raw_value", "")
    unit = payload.get("unit")
    return value_parser.parse(raw_val, explicit_unit=unit)


@app.get("/api/v1/ontology")
async def list_ontology():
    return spec_normalizer.SEED_ONTOLOGY


@app.post("/api/v1/process-document", response_model=ProcessDocumentResponse)
async def process_document(request: ProcessDocumentRequest):
    """
    Phase 2 Orchestrated Pipeline:
    Extract text -> Classify -> Spec extraction -> Normalization -> Value parsing -> Matching.
    """
    logger.info(f"Processing document request for key: {request.minio_file_key}")
    import base64

    pdf_data = b""
    if request.file_bytes_base64:
        try:
            pdf_data = base64.b64decode(request.file_bytes_base64)
        except Exception:
            pdf_data = b""

    # If no bytes provided directly, attempt synthetic header with file key information
    if not pdf_data:
        # Generate simulated text based on key/context if running without MinIO download
        synthetic_text = f"Document: {request.minio_file_key}\nType: {request.document_type_hint}\nTechnical Specifications"
        pdf_data = synthetic_text.encode("utf-8")

    result = pipeline_orchestrator.run(
        pdf_data,
        requirement_products=request.requirement_products
    )

    return ProcessDocumentResponse(
        status=result.status if result.status != "EMPTY_DOCUMENT" else "STUB_PROCESSED",
        message="Document processed through Phase 2 AI pipeline.",
        minio_file_key=request.minio_file_key,
        document_type=result.doc_type if result.doc_type != "unknown" else (request.document_type_hint or "unknown"),
        products=[p.model_dump() for p in result.products]
    )


# --- Phase 3 Matching Endpoints ---
from app.matching.operator_inference import OperatorInference
from app.matching.numeric_comparator import compare_numeric
from app.matching.text_matcher import TextMatcher
from app.matching.confidence_scorer import calculate_confidence
from app.matching.matcher import SpecMatcher, MatchBatchRequest, MatchBatchResponse

spec_matcher = SpecMatcher()
text_matcher = TextMatcher()


@app.post("/api/v1/infer-operator")
async def infer_operator(payload: Dict[str, Any] = Body(...)):
    canonical = payload.get("canonical_name")
    is_numeric = payload.get("is_numeric", True)
    op = OperatorInference.infer(canonical, is_numeric)
    return {"canonical_name": canonical, "operator": op}


@app.post("/api/v1/compare-numeric")
async def api_compare_numeric(payload: Dict[str, Any] = Body(...)):
    return compare_numeric(
        required_val=payload.get("required_val"),
        vendor_val=payload.get("vendor_val"),
        operator=payload.get("operator", ">="),
        required_unit=payload.get("required_unit"),
        vendor_unit=payload.get("vendor_unit"),
        min_val=payload.get("min_val"),
        max_val=payload.get("max_val"),
    )


@app.post("/api/v1/compare-text")
async def api_compare_text(payload: Dict[str, Any] = Body(...)):
    return text_matcher.match(
        required=payload.get("required", ""),
        offered=payload.get("offered", ""),
        operator=payload.get("operator", "semantic"),
        context=payload.get("context", ""),
    )


@app.post("/api/v1/compute-confidence")
async def api_compute_confidence(payload: Dict[str, Any] = Body(...)):
    score = calculate_confidence(
        comparison_method=payload.get("comparison_method", "exact_text"),
        comparison_confidence=payload.get("comparison_confidence", 0.90),
        extraction_confidence=payload.get("extraction_confidence", "high"),
        normalization_confidence=payload.get("normalization_confidence", "high"),
        cross_encoder_score=payload.get("cross_encoder_score"),
        verdict=payload.get("verdict"),
    )
    return {"confidence": score}


@app.post("/api/v1/match-specs", response_model=MatchBatchResponse)
async def match_specs(request: MatchBatchRequest):
    """
    Phase 3 Batch Matching Endpoint:
    Compares requirement specifications against vendor offerings.
    """
    logger.info(f"Received batch matching request with {len(request.spec_pairs)} pairs")
    return spec_matcher.match_batch(request)


# Phase 6: Active Learning & Model Upgrades Router
from app.routers.models import router as models_router
app.include_router(models_router)

# Unified Two-Pass Free Local LLM Router
from app.routers.two_pass import router as two_pass_router
app.include_router(two_pass_router)



