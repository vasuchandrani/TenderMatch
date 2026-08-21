import logging
from typing import Dict, Any, List, Optional
from fastapi import APIRouter, Body
from pydantic import BaseModel

from app.pipeline.two_pass_service import two_pass_service

logger = logging.getLogger("tendermatch-ai.two_pass_router")
router = APIRouter(prefix="/api/v1/two-pass", tags=["Two-Pass Free Local LLM"])


class InitialEvaluateRequest(BaseModel):
    tender_id: Optional[str] = None
    vendor_submission_id: Optional[str] = None
    requirement_specs: List[Dict[str, Any]] = []
    vendor_specs: List[Dict[str, Any]] = []


class ReverifyReflectionRequest(BaseModel):
    tender_id: Optional[str] = None
    vendor_submission_id: Optional[str] = None
    reviewed_specs: List[Dict[str, Any]] = []
    human_notes: Optional[str] = None


@router.post("/initial-evaluate")
async def initial_evaluate(request: InitialEvaluateRequest):
    """
    Pass 1: Preliminary comparison of requirement specs against vendor specs.
    Extracts/normalizes values and produces preliminary Match/Unmatch/AI Err verdicts.
    """
    logger.info(f"Executing Pass 1 for tender {request.tender_id}, vendor {request.vendor_submission_id}")
    results = two_pass_service.execute_pass_1(
        requirement_specs=request.requirement_specs,
        vendor_specs=request.vendor_specs,
    )
    return {
        "status": "PASS_1_COMPLETED",
        "tender_id": request.tender_id,
        "vendor_submission_id": request.vendor_submission_id,
        "results": results,
    }


@router.post("/reverify-scores")
async def reverify_scores(request: ReverifyReflectionRequest):
    """
    Pass 2: Reflection & Contextual Re-Verification.
    Accepts human reviewer overrides as absolute ground truth,
    evaluates secondary clauses, cognitive spec criticality, and certifies final verdicts.
    """
    logger.info(f"Executing Pass 2 Reflection for tender {request.tender_id}, vendor {request.vendor_submission_id}")
    certified_results, meta = two_pass_service.execute_pass_2_reflection(
        reviewed_specs=request.reviewed_specs,
        human_notes=request.human_notes,
        return_metadata=True,
    )
    return {
        "status": "PASS_2_REFLECTION_CERTIFIED",
        "tender_id": request.tender_id,
        "vendor_submission_id": request.vendor_submission_id,
        "certified_results": certified_results,
        "criticality_weights": meta.get("criticality_weights", {}),
        "reflection_summary": meta.get("reflection_summary", ""),
    }
