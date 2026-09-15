import pytest
from app.matching.verdict_assigner import assign_verdict


def test_high_confidence_match_returns_match():
    verdict = assign_verdict(raw_verdict="match", confidence=0.85)
    assert verdict == "Match"


def test_high_confidence_unmatch_returns_unmatch():
    verdict = assign_verdict(raw_verdict="unmatch", confidence=0.85)
    assert verdict == "Unmatch"


def test_low_confidence_returns_ai_err():
    verdict = assign_verdict(raw_verdict="match", confidence=0.35)
    assert verdict == "AI Err"


def test_uncertain_verdict_returns_ai_err():
    verdict = assign_verdict(raw_verdict="uncertain", confidence=0.75)
    assert verdict == "AI Err"
