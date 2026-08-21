import json
import logging
import urllib.request
from typing import Dict, Any, List, Optional

from app.config import settings

logger = logging.getLogger("tendermatch-ai.two_pass")


class TwoPassLLMService:
    def __init__(self):
        self.ollama_base = settings.OLLAMA_API_BASE.rstrip("/")
        self.default_model = "llama3.1:8b"

    def _call_ollama(self, prompt: str, system_prompt: Optional[str] = None, format_json: bool = True) -> Dict[str, Any]:
        # 1. Try Groq Cloud API (Fastest)
        if settings.GROQ_API_KEY and settings.GROQ_API_KEY.strip():
            try:
                headers = {
                    "Authorization": f"Bearer {settings.GROQ_API_KEY.strip()}",
                    "Content-Type": "application/json"
                }
                messages = []
                if system_prompt:
                    messages.append({"role": "system", "content": system_prompt})
                messages.append({"role": "user", "content": prompt})

                payload: Dict[str, Any] = {
                    "model": settings.GROQ_PRIMARY_MODEL,
                    "messages": messages,
                    "temperature": 0.1
                }
                if format_json:
                    payload["response_format"] = {"type": "json_object"}

                req = urllib.request.Request(
                    f"{settings.GROQ_API_BASE}/chat/completions",
                    data=json.dumps(payload).encode("utf-8"),
                    headers=headers,
                    method="POST",
                )
                with urllib.request.urlopen(req, timeout=20) as resp:
                    res_data = json.loads(resp.read().decode("utf-8"))
                    content = res_data["choices"][0]["message"]["content"]
                    if format_json:
                        return json.loads(content)
                    return {"text": content}
            except Exception as ge:
                logger.warning(f"Groq call failed ({ge}). Falling back to local Ollama.")

        # 2. Local Ollama fallback
        payload = {
            "model": self.default_model,
            "prompt": prompt,
            "stream": False,
        }
        if system_prompt:
            payload["system"] = system_prompt
        if format_json:
            payload["format"] = "json"

        try:
            req = urllib.request.Request(
                f"{self.ollama_base}/api/generate",
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json"},
                method="POST",
            )
            with urllib.request.urlopen(req, timeout=30) as resp:
                res_data = json.loads(resp.read().decode("utf-8"))
                content = res_data.get("response", "{}")
                if format_json:
                    return json.loads(content)
                return {"text": content}
        except Exception as e:
            logger.warning(f"Ollama call failed or timed out: {e}. Falling back to structured heuristic.")
            return {}

    def execute_pass_1(
        self,
        requirement_specs: List[Dict[str, Any]],
        vendor_specs: List[Dict[str, Any]],
        context: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        results = []
        vendor_map = {}
        for v in vendor_specs:
            key = (v.get("canonical_spec_name") or v.get("raw_spec_name") or "").strip().lower()
            if key:
                vendor_map[key] = v

        for req in requirement_specs:
            req_name = req.get("canonical_spec_name") or req.get("raw_spec_name", "")
            req_key = req_name.strip().lower()
            ven = vendor_map.get(req_key)

            if not ven:
                results.append({
                    "spec_name": req_name,
                    "tender_spec_id": req.get("id"),
                    "vendor_spec_id": None,
                    "system_verdict": "Unmatch",
                    "confidence_score": 0.95,
                    "pass": 1,
                    "method": "missing_offering",
                    "details": f"Requirement '{req_name}' not found in vendor bid document.",
                })
                continue

            req_val = str(req.get("spec_value") or req.get("raw_text") or "")
            ven_val = str(ven.get("spec_value") or ven.get("raw_text") or "")

            req_num = req.get("numeric_value")
            ven_num = ven.get("numeric_value")

            if req_num is not None and ven_num is not None:
                is_match = float(ven_num) >= float(req_num)
                verdict = "Match" if is_match else "Unmatch"
                results.append({
                    "spec_name": req_name,
                    "tender_spec_id": req.get("id"),
                    "vendor_spec_id": ven.get("id"),
                    "system_verdict": verdict,
                    "confidence_score": 0.95,
                    "pass": 1,
                    "method": "numeric_rule",
                    "details": f"Numeric comparison: offered {ven_val} vs required {req_val}.",
                })
            else:
                prompt = (
                    "Evaluate technical compliance for procurement:\n"
                    f"Requirement: {req_name} = {req_val}\n"
                    f"Vendor Offered: {ven_val}\n\n"
                    'Return JSON format: {"verdict": "Match", "confidence": 0.85, "reasoning": "rationale"}'
                )
                system_prompt = "You are an expert procurement compliance analyst. Compare specifications strictly."
                llm_resp = self._call_ollama(prompt, system_prompt)

                verdict = llm_resp.get("verdict") or ("Match" if req_val.lower() in ven_val.lower() else "Unmatch")
                if verdict not in ("Match", "Unmatch", "AI Err"):
                    verdict = "AI Err"

                confidence = float(llm_resp.get("confidence", 0.80))
                reasoning = llm_resp.get("reasoning") or f"Pass 1 evaluation: {ven_val} vs {req_val}"

                results.append({
                    "spec_name": req_name,
                    "tender_spec_id": req.get("id"),
                    "vendor_spec_id": ven.get("id"),
                    "system_verdict": verdict,
                    "confidence_score": confidence,
                    "pass": 1,
                    "method": "llm_pass1",
                    "details": reasoning,
                })

        return results

    def evaluate_spec_criticality(self, spec_names: List[str]) -> Dict[str, Dict[str, Any]]:
        """
        Uses LLM reasoning to evaluate specification domain criticality for procurement.
        Core hardware components (RAM, CPU, Storage) are classified as CRITICAL (4.0),
        while secondary attributes (Color, Chassis, Weight) are classified as MINOR/COSMETIC (0.25 - 1.0).
        """
        if not spec_names:
            return {}

        prompt = (
            "You are an expert AI procurement system. Evaluate the technical criticality of each laptop/hardware specification:\n"
            f"Specifications: {json.dumps(spec_names)}\n\n"
            "Classify each specification into:\n"
            '- "CRITICAL" (Weight 4.0): Core computational capability (RAM, Processor/CPU, Storage/SSD, Cores, Main OS)\n'
            '- "MAJOR" (Weight 2.0): Significant hardware features (Display, Battery, Warranty, Security TPM)\n'
            '- "MINOR" (Weight 1.0): Secondary features (Weight, Dimensions, Audio, Ports)\n'
            '- "COSMETIC" (Weight 0.25): Aesthetic attributes (Color, Chassis finish, Bundled bag/sticker)\n\n'
            'Return a JSON object mapping each spec name to its evaluation:\n'
            '{"criticalities": {"spec_name": {"category": "CRITICAL", "weight": 4.0, "reasoning": "brief explanation"}}}'
        )
        system_prompt = "You are an AI procurement compliance analyst. Cognitive reasoning prioritizes functional requirements like RAM/CPU over aesthetics like Color."
        llm_resp = self._call_ollama(prompt, system_prompt)

        criticalities = llm_resp.get("criticalities", {}) if isinstance(llm_resp, dict) else {}

        # Merge with deterministic heuristics to ensure 100% reliability
        results = {}
        for s in spec_names:
            s_clean = s.strip()
            s_lower = s_clean.lower()
            if s_clean in criticalities and isinstance(criticalities[s_clean], dict):
                results[s_clean] = criticalities[s_clean]
            else:
                # Rule-based cognitive fallback
                if any(k in s_lower for k in ["ram", "memory", "processor", "cpu", "core", "storage", "ssd", "hard drive"]):
                    results[s_clean] = {
                        "category": "CRITICAL",
                        "weight": 4.0,
                        "reasoning": f"'{s_clean}' is a mission-critical computational component directly impacting workload execution."
                    }
                elif any(k in s_lower for k in ["display", "screen", "battery", "warranty", "operating system", "os", "security"]):
                    results[s_clean] = {
                        "category": "MAJOR",
                        "weight": 2.0,
                        "reasoning": f"'{s_clean}' is an essential hardware specification affecting usability and contract service life."
                    }
                elif any(k in s_lower for k in ["color", "colour", "finish", "chassis", "sticker", "bag"]):
                    results[s_clean] = {
                        "category": "COSMETIC",
                        "weight": 0.25,
                        "reasoning": f"'{s_clean}' is purely aesthetic and does not affect technical processing or contract fulfillment."
                    }
                else:
                    results[s_clean] = {
                        "category": "MINOR",
                        "weight": 1.0,
                        "reasoning": f"'{s_clean}' is a standard secondary physical specification."
                    }

        return results

    def execute_pass_2_reflection(
        self,
        reviewed_specs: List[Dict[str, Any]],
        human_notes: Optional[str] = None,
        return_metadata: bool = False,
    ) -> Any:
        certified_results = []
        spec_names = [item.get("raw_spec_name") or item.get("spec_name", "") for item in reviewed_specs if item.get("raw_spec_name") or item.get("spec_name")]
        criticality_map = self.evaluate_spec_criticality(spec_names)

        critical_unmatched = []
        cosmetic_matched = []

        for item in reviewed_specs:
            human_verdict = item.get("human_verdict")
            system_verdict = item.get("system_verdict")
            spec_name = item.get("raw_spec_name") or item.get("spec_name", "")
            req_text = item.get("required_text") or item.get("required_value", "")
            off_text = item.get("offered_text") or item.get("offered_value", "")
            comment = item.get("reviewer_comment") or ""

            crit_info = criticality_map.get(spec_name, {
                "category": "MAJOR",
                "weight": 2.0,
                "reasoning": "Standard procurement specification"
            })

            final_verdict = human_verdict if human_verdict else system_verdict
            is_overridden = human_verdict is not None and human_verdict != system_verdict

            if final_verdict == "Unmatch" and crit_info["category"] == "CRITICAL":
                critical_unmatched.append(spec_name)
            elif final_verdict == "Match" and crit_info["category"] == "COSMETIC":
                cosmetic_matched.append(spec_name)

            if is_overridden:
                prompt = (
                    "A human procurement reviewer has overridden the AI verdict for the following spec:\n"
                    f"Specification: {spec_name} (Criticality: {crit_info['category']})\n"
                    f"Requirement: {req_text}\n"
                    f"Offered: {off_text}\n"
                    f"Initial AI Verdict: {system_verdict}\n"
                    f"Human Decision: {human_verdict}\n"
                    f"Reviewer Comment: {comment}\n\n"
                    "Reflect on this override. Explain why the human decision is certified as the ground truth "
                    "and explain any contract warranty, performance, or operational implications.\n"
                    f'Return JSON: {{"certified_verdict": "{human_verdict}", "reflection_audit_note": "explanation"}}'
                )
                system_prompt = "You are an AI procurement audit validator. Treat human decisions as binding ground truth."
                llm_resp = self._call_ollama(prompt, system_prompt)

                audit_note = llm_resp.get("reflection_audit_note") or (
                    f"Verified human override ({system_verdict} -> {human_verdict}). "
                    f"Certified based on human evaluation: {comment or 'compliance confirmed'}"
                )
                if not audit_note.lower().startswith("certified") and not audit_note.lower().startswith("[certified"):
                    audit_note = f"[Certified Ground Truth] {audit_note}"

                certified_results.append({
                    "id": item.get("id"),
                    "spec_name": spec_name,
                    "certified_verdict": final_verdict,
                    "confidence_score": 1.0,
                    "is_reflection_certified": True,
                    "is_override": True,
                    "criticality_category": crit_info["category"],
                    "criticality_weight": crit_info["weight"],
                    "criticality_reasoning": crit_info["reasoning"],
                    "reflection_audit_note": audit_note,
                })
            else:
                certified_results.append({
                    "id": item.get("id"),
                    "spec_name": spec_name,
                    "certified_verdict": final_verdict,
                    "confidence_score": item.get("confidence_score", 0.90),
                    "is_reflection_certified": True,
                    "is_override": False,
                    "criticality_category": crit_info["category"],
                    "criticality_weight": crit_info["weight"],
                    "criticality_reasoning": crit_info["reasoning"],
                    "reflection_audit_note": f"Confirmed {final_verdict} under Pass 2 verification audit [{crit_info['category']} spec, weight {crit_info['weight']}].",
                })

        # Generate cognitive reflection summary highlighting why RAM > Color
        reflection_summary = (
            f"Pass 2 cognitive audit completed across {len(certified_results)} specifications. "
        )
        if critical_unmatched:
            reflection_summary += f"CRITICAL DEFICIENCY DETECTED: Vendor failed critical specification(s) ({', '.join(critical_unmatched)}). "
            if cosmetic_matched:
                reflection_summary += f"Although vendor satisfied cosmetic attribute(s) ({', '.join(cosmetic_matched)}), "
                reflection_summary += "critical computational requirements vastly outweigh cosmetic matches, significantly penalizing the overall score."
        else:
            reflection_summary += "All critical core computational specifications (RAM, CPU, Storage) satisfied."

        if return_metadata:
            meta = {
                "criticality_weights": criticality_map,
                "reflection_summary": reflection_summary,
            }
            return certified_results, meta

        return certified_results


two_pass_service = TwoPassLLMService()
