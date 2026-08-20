from typing import Union

CONFIDENCE_THRESHOLD = 0.50


def assign_verdict(raw_verdict: Union[str, bool], confidence: float) -> str:
    """
    Assigns user-facing verdict badge: 'Match', 'Unmatch', or 'AI Err'.
    """
    if confidence < CONFIDENCE_THRESHOLD:
        return "AI Err"

    v_str = str(raw_verdict).strip().lower()
    if v_str in ("uncertain", "ai err", "error"):
        return "AI Err"

    if v_str in ("match", "true"):
        return "Match"

    return "Unmatch"
