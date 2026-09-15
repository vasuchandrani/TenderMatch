import pytest
from unittest.mock import patch, MagicMock
from app.matching.text_matcher import TextMatcher


@pytest.fixture
def matcher():
    m = TextMatcher()
    m._cross_encoder = MagicMock()
    return m


def test_high_cross_encoder_score_returns_match(matcher):
    with patch.object(matcher, "score_cross_encoder", return_value=0.92) as mock_ce:
        with patch.object(matcher, "call_llm_judge") as mock_llm:
            res = matcher.match(
                required="24/7 on-site technical support",
                offered="Round-the-clock on-site support team",
                operator="semantic",
            )
            assert res["verdict"] == "match"
            assert res["method"] == "cross_encoder"
            assert res["confidence"] >= 0.85
            mock_llm.assert_not_called()


def test_low_cross_encoder_score_returns_unmatch(matcher):
    with patch.object(matcher, "score_cross_encoder", return_value=0.12) as mock_ce:
        with patch.object(matcher, "call_llm_judge") as mock_llm:
            res = matcher.match(
                required="Linux Ubuntu 22.04 LTS pre-installed",
                offered="Microsoft Windows 11 Home edition",
                operator="semantic",
            )
            assert res["verdict"] == "unmatch"
            assert res["method"] == "cross_encoder"
            assert res["confidence"] >= 0.80
            mock_llm.assert_not_called()


def test_ambiguous_score_routes_to_llm(matcher):
    with patch.object(matcher, "score_cross_encoder", return_value=0.52):
        with patch.object(
            matcher,
            "call_llm_judge",
            return_value={"verdict": "match", "reasoning": "Vendor terms meet intent"},
        ) as mock_llm:
            res = matcher.match(
                required="Enterprise grade durability with spill protection",
                offered="Commercial durable chassis with sealed keyboard",
                operator="semantic",
            )
            assert res["verdict"] == "match"
            assert res["method"] == "llm_judge"
            assert "Vendor terms meet intent" in res["reasoning"]
            mock_llm.assert_called_once()


def test_llm_judge_match_verdict(matcher):
    with patch.object(matcher, "score_cross_encoder", return_value=0.50):
        with patch.object(
            matcher,
            "call_llm_judge",
            return_value={"verdict": "match", "reasoning": "Compatible standard"},
        ):
            res = matcher.match("MIL-STD 810G", "MIL-STD 810H certified", operator="semantic")
            assert res["verdict"] == "match"
            assert res["method"] == "llm_judge"
            assert res["confidence"] >= 0.85


def test_llm_judge_uncertain_returns_ai_err(matcher):
    with patch.object(matcher, "score_cross_encoder", return_value=0.50):
        with patch.object(
            matcher,
            "call_llm_judge",
            return_value={"verdict": "uncertain", "reasoning": "Insufficient specifications"},
        ):
            res = matcher.match("Custom proprietary warranty", "Standard depot", operator="semantic")
            assert res["verdict"] == "uncertain"
            assert res["confidence"] <= 0.35


def test_exact_text_match_ddr5(matcher):
    with patch.object(matcher, "score_cross_encoder") as mock_ce:
        res = matcher.match("DDR5", "ddr5", operator="==")
        assert res["verdict"] == "match"
        assert res["method"] == "exact_text"
        assert res["confidence"] >= 0.95
        mock_ce.assert_not_called()


def test_exact_text_mismatch_ddr4_ddr5(matcher):
    with patch.object(matcher, "score_cross_encoder") as mock_ce:
        res = matcher.match("DDR5", "DDR4", operator="==")
        assert res["verdict"] == "unmatch"
        assert res["method"] == "exact_text"
        assert res["confidence"] >= 0.95
        mock_ce.assert_not_called()


def test_contains_match(matcher):
    with patch.object(matcher, "score_cross_encoder") as mock_ce:
        res = matcher.match(
            required="ISO 9001:2015",
            offered="ISO 9001:2015, ISO 14001, CE Certified",
            operator="contains",
        )
        assert res["verdict"] == "match"
        assert res["method"] == "exact_text"
        mock_ce.assert_not_called()
