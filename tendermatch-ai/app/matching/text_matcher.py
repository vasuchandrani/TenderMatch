import logging
from typing import Dict, Any, Optional
import numpy as np

logger = logging.getLogger("tendermatch-ai.text_matcher")


class TextMatcher:
    """
    Two-tiered semantic matching:
    1. Exact / normalized equality for '==' and contains operators.
    2. Cross-encoder scoring for semantic specs.
    3. LLM judge routing for ambiguous cases (0.25 < score < 0.80).
    """

    def __init__(self):
        self._cross_encoder = None

    def _get_cross_encoder(self):
        if self._cross_encoder is None:
            try:
                from sentence_transformers import CrossEncoder
                self._cross_encoder = CrossEncoder("cross-encoder/ms-marco-MiniLM-L-6-v2")
            except Exception as e:
                logger.warning(f"Could not load cross-encoder model: {e}. Using token overlap fallback.")
                self._cross_encoder = "FALLBACK"
        return self._cross_encoder

    def score_cross_encoder(self, required: str, offered: str) -> float:
        """Returns semantic score between 0.0 and 1.0."""
        ce = self._get_cross_encoder()
        if ce != "FALLBACK" and ce is not None:
            try:
                raw_score = ce.predict([(required, offered)])[0]
                # Sigmoid transform raw logits
                return float(1.0 / (1.0 + np.exp(-raw_score)))
            except Exception as e:
                logger.warning(f"Cross-encoder prediction failed: {e}")

        # Lightweight fallback based on token Jaccard similarity
        r_tokens = set(required.lower().split())
        o_tokens = set(offered.lower().split())
        if not r_tokens or not o_tokens:
            return 0.0
        intersection = len(r_tokens & o_tokens)
        union = len(r_tokens | o_tokens)
        return float(intersection / union)

    def call_llm_judge(self, required: str, offered: str, context: str = "") -> Dict[str, str]:
        """
        Escalated evaluation for ambiguous text specs using Cloud LLM (Gemini/Groq) or local Ollama.
        """
        try:
            import httpx
            import json
            from app.config import settings

            prompt = f"""You are an expert enterprise procurement specification compliance judge.
Evaluate whether the offered specification meets, exceeds, or complies with the tender requirement.

REQUIREMENT: {required}
OFFERED: {offered}
SPEC CONTEXT: {context}

Evaluation Guidelines:
- If the offered specification equals, meets, exceeds, or provides an equivalent/better modern standard (e.g. DDR5 vs DDR4 required, Windows 11 Pro vs Windows 11 Pro with dual boot, i9 vs i7 required, RTX 4080 vs RTX 4060 required, 64GB vs 32GB RAM, 3 Years Premier vs 3 Years Onsite), return "match".
- If the offered specification falls short, is missing essential features, or provides a lower/incompatible tier (e.g. Windows Home vs Windows Pro required, Integrated GPU vs Dedicated RTX required, DDR4 when DDR5 required, 1 Year warranty when 3 Years required), return "unmatch".
- Return "uncertain" only if the information is completely ambiguous or conflicting.

Format your entire response strictly as a valid JSON object:
{{"verdict": "match"|"unmatch"|"uncertain", "reasoning": "<clear concise 1-sentence explanation of compliance or failure>"}}
"""

            # 1. Try Google Gemini Cloud API (Fast & Free)
            if settings.GEMINI_API_KEY and settings.GEMINI_API_KEY.strip():
                api_key = settings.GEMINI_API_KEY.strip()
                for model in [settings.GEMINI_PRIMARY_MODEL, settings.GEMINI_FALLBACK_MODEL]:
                    try:
                        endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
                        payload = {
                            "contents": [{"parts": [{"text": prompt}]}],
                            "generationConfig": {
                                "responseMimeType": "application/json",
                                "temperature": 0.1
                            }
                        }
                        with httpx.Client(timeout=15.0) as client:
                            resp = client.post(endpoint, json=payload)
                            if resp.status_code == 200:
                                content = resp.json()["candidates"][0]["content"]["parts"][0]["text"]
                                data = json.loads(content)
                                return {
                                    "verdict": str(data.get("verdict", "uncertain")).strip().lower(),
                                    "reasoning": data.get("reasoning", "Evaluated by Google Gemini Cloud"),
                                }
                    except Exception as gme:
                        logger.warning(f"Gemini judge with model {model} failed: {gme}")

            # 2. Try Groq Cloud API
            if settings.GROQ_API_KEY and settings.GROQ_API_KEY.strip():
                try:
                    headers = {
                        "Authorization": f"Bearer {settings.GROQ_API_KEY.strip()}",
                        "Content-Type": "application/json"
                    }
                    payload = {
                        "model": settings.GROQ_PRIMARY_MODEL,
                        "messages": [
                            {"role": "system", "content": "You are a strict procurement specification compliance judge. Always respond with valid JSON."},
                            {"role": "user", "content": prompt}
                        ],
                        "response_format": {"type": "json_object"},
                        "temperature": 0.1
                    }
                    with httpx.Client(timeout=15.0) as client:
                        resp = client.post(f"{settings.GROQ_API_BASE}/chat/completions", headers=headers, json=payload)
                        if resp.status_code == 200:
                            content = resp.json()["choices"][0]["message"]["content"]
                            data = json.loads(content)
                            return {
                                "verdict": str(data.get("verdict", "uncertain")).strip().lower(),
                                "reasoning": data.get("reasoning", "Evaluated by Groq Cloud LLM"),
                            }
                except Exception as ge:
                    logger.warning(f"Groq judge evaluation fallback to Ollama: {ge}")

            # 3. Local Ollama fallback
            resp = httpx.post(
                f"{settings.OLLAMA_API_BASE}/api/generate",
                json={
                    "model": settings.PRIMARY_LLM_MODEL,
                    "prompt": prompt,
                    "stream": False,
                    "format": "json"
                },
                timeout=25.0
            )
            if resp.status_code == 200:
                raw_json = resp.json().get("response", "{}")
                data = json.loads(raw_json)
                return {
                    "verdict": str(data.get("verdict", "uncertain")).strip().lower(),
                    "reasoning": data.get("reasoning", "Evaluated by local LLM"),
                }
            return {
                "verdict": "uncertain",
                "reasoning": f"LLM returned HTTP {resp.status_code}",
            }
        except Exception as e:
            logger.info(f"LLM judge evaluation fell back: {e}")
            return {
                "verdict": "uncertain",
                "reasoning": "LLM evaluation unavailable or ambiguous",
            }

    def match(
        self,
        required: str,
        offered: str,
        operator: str = "semantic",
        context: str = "",
    ) -> Dict[str, Any]:
        """
        Main text matching method handling exact equality, contains, and cross-encoder / LLM judge.
        """
        if not required or not offered:
            return {
                "verdict": "uncertain",
                "confidence": 0.20,
                "method": "missing_value",
                "reasoning": "Missing required or offered text value",
            }

        req_clean = required.strip().lower()
        off_clean = offered.strip().lower()

        # 1. Exact Match Rule (e.g. DDR4 vs DDR5 — NEVER fuzzy match)
        if operator == "==":
            matched = req_clean == off_clean
            reasoning_text = (
                f"Offered '{offered}' matches the required criteria."
                if matched
                else f"Offered '{offered}' differs from required '{required}'."
            )
            return {
                "verdict": "match" if matched else "unmatch",
                "confidence": 0.95,
                "method": "exact_text",
                "reasoning": reasoning_text,
            }

        # 2. Contains Operator
        if operator == "contains":
            matched = req_clean in off_clean
            reasoning_text = (
                f"Required term '{required}' is present in offered '{offered}'."
                if matched
                else f"Required term '{required}' was not found in offered '{offered}'."
            )
            return {
                "verdict": "match" if matched else "unmatch",
                "confidence": 0.95 if matched else 0.85,
                "method": "exact_text",
                "reasoning": reasoning_text,
            }

        # 3. Semantic / Free-text Matching
        ce_score = self.score_cross_encoder(required, offered)
        ce = self._get_cross_encoder()

        if ce_score >= 0.80:
            return {
                "verdict": "match",
                "confidence": round(float(ce_score), 3),
                "method": "cross_encoder",
                "cross_encoder_score": round(float(ce_score), 3),
                "reasoning": f"High cross-encoder semantic score: {ce_score:.2f}",
            }
        elif ce_score <= 0.25:
            if ce == "FALLBACK" and ce_score > 0.0:
                # Token overlap alone cannot distinguish hardware tiers like RTX 4080 vs RTX 4060
                pass
            else:
                return {
                    "verdict": "unmatch",
                    "confidence": round(float(1.0 - ce_score), 3),
                    "method": "cross_encoder",
                    "cross_encoder_score": round(float(ce_score), 3),
                    "reasoning": f"Low cross-encoder semantic score: {ce_score:.2f}",
                }

        # Route to LLM Judge for intelligent semantic & hardware compliance evaluation
        llm_result = self.call_llm_judge(required, offered, context)
        verdict = str(llm_result.get("verdict", "uncertain")).strip().lower()
        confidence = 0.92 if verdict in ("match", "unmatch") else 0.30

        return {
            "verdict": verdict,
            "confidence": confidence,
            "method": "llm_judge",
            "cross_encoder_score": round(float(ce_score), 3),
            "reasoning": llm_result.get("reasoning", "Evaluated by AI LLM judge"),
        }
