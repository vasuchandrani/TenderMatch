from typing import Optional, Union

CONFIDENCE_MAP = {
    "high": 1.0,
    "medium": 0.70,
    "low": 0.30,
}


def map_conf_to_float(val: Union[str, float, None], default: float = 0.70) -> float:
    if val is None:
        return default
    if isinstance(val, (int, float)):
        return float(val)
    return CONFIDENCE_MAP.get(str(val).strip().lower(), default)


def calculate_confidence(
    comparison_method: str = "exact_text",
    comparison_confidence: float = 0.90,
    extraction_confidence: Union[str, float] = "high",
    normalization_confidence: Union[str, float] = "high",
    cross_encoder_score: Optional[float] = None,
    verdict: Optional[str] = None,
) -> float:
    """
    Combines extraction, normalization, and comparison signals into a single
    calibrated confidence score in [0.0, 1.0].
    """
    ext_val = map_conf_to_float(extraction_confidence, 0.85)
    norm_val = map_conf_to_float(normalization_confidence, 0.85)

    comp_val = comparison_confidence
    if comparison_method == "cross_encoder" and cross_encoder_score is not None:
        # Extreme scores (near 0 or near 1) indicate high certainty
        comp_val = abs(cross_encoder_score - 0.5) * 2.0
    elif comparison_method == "llm_judge" and verdict == "uncertain":
        comp_val = min(comp_val, 0.15)
    elif comparison_method == "missing_value":
        comp_val = 0.20

    weights = {
        "extraction": 0.20,
        "normalization": 0.10,
        "comparison": 0.70,
    }

    weighted_sum = (
        ext_val * weights["extraction"]
        + norm_val * weights["normalization"]
        + comp_val * weights["comparison"]
    )
    total_weights = sum(weights.values())
    final_score = weighted_sum / total_weights

    return round(max(0.0, min(1.0, float(final_score))), 3)
