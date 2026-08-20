import logging
from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field

from app.matching.operator_inference import OperatorInference
from app.matching.numeric_comparator import compare_numeric
from app.matching.text_matcher import TextMatcher
from app.matching.confidence_scorer import calculate_confidence
from app.matching.verdict_assigner import assign_verdict

logger = logging.getLogger("tendermatch-ai.matcher")


class MatchRequestItem(BaseModel):
    pair_id: str
    requirement: Dict[str, Any]
    vendor: Optional[Dict[str, Any]] = None


class MatchBatchRequest(BaseModel):
    tender_id: Optional[str] = None
    vendor_submission_id: Optional[str] = None
    spec_pairs: List[MatchRequestItem] = Field(default_factory=list)


class MatchResultItem(BaseModel):
    pair_id: str
    operator: str
    method: str
    verdict: str  # "Match", "Unmatch", "AI Err"
    confidence: float
    details: str
    cross_encoder_score: Optional[float] = None
    llm_verdict: Optional[str] = None
    llm_reasoning: Optional[str] = None
    numeric_comparison: Optional[Dict[str, Any]] = None
    confidence_features: Optional[Dict[str, Any]] = None


class MatchBatchResponse(BaseModel):
    tender_id: Optional[str] = None
    vendor_submission_id: Optional[str] = None
    results: List[MatchResultItem] = Field(default_factory=list)


class SpecMatcher:
    def __init__(self):
        self.text_matcher = TextMatcher()

    def match_pair(self, item: MatchRequestItem) -> MatchResultItem:
        req = item.requirement
        ven = item.vendor

        # 1. Missing Vendor Spec Case
        if ven is None:
            return MatchResultItem(
                pair_id=item.pair_id,
                operator="none",
                method="missing",
                verdict="Unmatch",
                confidence=1.0,
                details="Requirement omitted / not offered by vendor",
                confidence_features={"method": "missing", "provided": False},
            )

        canonical = req.get("canonical_name") or ven.get("canonical_name")
        req_is_numeric = req.get("is_numeric", False) or req.get("numeric_value") is not None
        ven_is_numeric = ven.get("is_numeric", False) or ven.get("numeric_value") is not None

        # 2. Numeric Comparison Flow
        if req_is_numeric and (ven_is_numeric or ven.get("numeric_value") is not None):
            op = OperatorInference.infer(canonical, is_numeric=True)
            num_res = compare_numeric(
                required_val=req.get("numeric_value"),
                vendor_val=ven.get("numeric_value"),
                operator=op,
                required_unit=req.get("unit"),
                vendor_unit=ven.get("unit"),
                min_val=req.get("min_value"),
                max_val=req.get("max_value"),
            )

            conf = calculate_confidence(
                comparison_method=num_res["method"],
                comparison_confidence=num_res["confidence"],
                extraction_confidence=ven.get("extraction_confidence", "high"),
                normalization_confidence=ven.get("normalization_confidence", "high"),
                verdict=num_res["verdict"],
            )

            v_val_str = f"{ven.get('numeric_value')} {ven.get('unit') or ''}".strip()
            r_val_str = f"{req.get('numeric_value')} {req.get('unit') or ''}".strip()
            is_match = num_res["verdict"] == "match"

            if num_res["method"] == "incompatible_units":
                details_str = num_res["details"].get(
                    "error",
                    f"Incompatible physical units: required '{r_val_str}' vs offered '{v_val_str}'."
                )
            elif op == ">=":
                details_str = (
                    f"Offered {v_val_str} satisfies minimum requirement of {r_val_str}."
                    if is_match
                    else f"Offered {v_val_str} is below the minimum required {r_val_str}."
                )
            elif op == "<=":
                details_str = (
                    f"Offered {v_val_str} meets maximum limit of {r_val_str}."
                    if is_match
                    else f"Offered {v_val_str} exceeds maximum allowed limit of {r_val_str}."
                )
            elif op == "==":
                details_str = (
                    f"Offered {v_val_str} matches target {r_val_str}."
                    if is_match
                    else f"Offered {v_val_str} does not match target {r_val_str}."
                )
            else:
                details_str = f"Offered {v_val_str} compared against required {r_val_str}."

            badge = assign_verdict(num_res["verdict"], conf)

            return MatchResultItem(
                pair_id=item.pair_id,
                operator=op,
                method=num_res["method"],
                verdict=badge,
                confidence=conf,
                details=details_str,
                numeric_comparison=num_res["details"],
                confidence_features={"is_numeric": True, "raw_conf": num_res["confidence"]},
            )

        # 3. Free-Text / Categorical Flow
        op = OperatorInference.infer(canonical, is_numeric=False)
        req_val = str(req.get("value") or req.get("raw_text") or "")
        ven_val = str(ven.get("value") or ven.get("raw_text") or "")

        text_res = self.text_matcher.match(
            required=req_val,
            offered=ven_val,
            operator=op,
            context=f"Spec: {canonical or req.get('spec_name')}",
        )

        conf = calculate_confidence(
            comparison_method=text_res["method"],
            comparison_confidence=text_res["confidence"],
            extraction_confidence=ven.get("extraction_confidence", "high"),
            normalization_confidence=ven.get("normalization_confidence", "high"),
            cross_encoder_score=text_res.get("cross_encoder_score"),
            verdict=text_res["verdict"],
        )

        badge = assign_verdict(text_res["verdict"], conf)
        details_str = text_res.get("reasoning") or f"Compared '{req_val}' with '{ven_val}'"

        return MatchResultItem(
            pair_id=item.pair_id,
            operator=op,
            method=text_res["method"],
            verdict=badge,
            confidence=conf,
            details=details_str,
            cross_encoder_score=text_res.get("cross_encoder_score"),
            llm_verdict=text_res.get("verdict") if text_res["method"] == "llm_judge" else None,
            llm_reasoning=text_res.get("reasoning") if text_res["method"] == "llm_judge" else None,
            confidence_features={"is_numeric": False, "method": text_res["method"]},
        )

    def match_batch(self, request: MatchBatchRequest) -> MatchBatchResponse:
        results = [self.match_pair(item) for item in request.spec_pairs]
        return MatchBatchResponse(
            tender_id=request.tender_id,
            vendor_submission_id=request.vendor_submission_id,
            results=results,
        )
