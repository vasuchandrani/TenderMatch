import json
import logging
import re
from typing import List, Optional
from pydantic import BaseModel, Field
import httpx
from app.config import settings

logger = logging.getLogger("tendermatch-ai.spec_extractor")


class ExtractedSpec(BaseModel):
    spec_name: str
    spec_value: str
    unit: Optional[str] = None
    raw_text: str = ""
    extraction_confidence: str = "high"
    extraction_source: str = "llm"


class ExtractedProduct(BaseModel):
    product_name: str
    product_category: str = "electronics"
    specifications: List[ExtractedSpec] = Field(default_factory=list)
    raw_text: str = ""


class SpecExtractor:
    def __init__(self,
                 primary_model: Optional[str] = None,
                 fallback_model: Optional[str] = None,
                 ollama_api_base: Optional[str] = None):
        self.primary_model = primary_model or settings.PRIMARY_LLM_MODEL
        self.fallback_model = fallback_model or settings.FALLBACK_LLM_MODEL
        self.ollama_api_base = ollama_api_base or settings.OLLAMA_API_BASE

    def extract(self, text: str, max_chunk_chars: int = 15000) -> List[ExtractedProduct]:
        if not text or not text.strip():
            return []

        chunks = self._chunk_text(text, max_chunk_chars)
        all_products: List[ExtractedProduct] = []

        for chunk in chunks:
            prompt = self._build_prompt(chunk)
            response_json = self._call_llm(prompt)
            products = self._parse_llm_response(response_json)
            all_products.extend(products)

        # Fallback to deterministic regex/table extraction if LLM is offline or returned empty
        if not all_products:
            logger.info("LLM extraction returned empty; using deterministic heuristic/table extractor")
            all_products = self._extract_heuristic(text)

        return self._merge_products(all_products)

    def _extract_heuristic(self, text: str) -> List[ExtractedProduct]:
        from pathlib import Path
        data_path = Path(__file__).resolve().parent.parent.parent / "data" / "ontology_seed.json"
        canonicals = {}
        if data_path.exists():
            try:
                with open(data_path, "r", encoding="utf-8") as f:
                    seed_data = json.load(f)
                canonicals = {item["canonical_name"]: item for item in seed_data}
            except Exception as e:
                logger.warning(f"Failed to load ontology seed data: {e}")

        lines = [l.strip() for l in text.splitlines() if l.strip()]
        products: List[ExtractedProduct] = []
        current_product_name = "Enterprise Hardware"
        current_specs: List[ExtractedSpec] = []

        prod_title_patterns = [
            re.compile(r"(?:Item\s*\d+|Lot\s*\d+|Product\s*\d+|Line Item\s*\d+)[:\s—–-]+([A-Za-z0-9\s\.\-_/]+)", re.IGNORECASE),
            re.compile(r"(?:Proposed Model|Offered Model|Product Model|Technical Offering|Offered Product|Device Model)[:\s—–-]+([A-Za-z0-9\s\.\-_/]+)", re.IGNORECASE),
            re.compile(r"(?:Technical Compliance Matrix\s*[-—]\s*)(.+)", re.IGNORECASE),
            re.compile(r"^\d{1,2}\.\s*([A-Za-z0-9\s\.\-_/]+?)\s*[-—:]\s*\d+\s*(?:No|Nos|nos)\.?", re.IGNORECASE),
        ]

        bullet_spec_pattern = re.compile(
            r"^[\s•\-\*\d\.]*([A-Za-z0-9\s\(\)/]+?)\s*:\s*([^;,\n]+?)(?:;|\n|$)",
            re.IGNORECASE
        )

        known_units = ["GB", "TB", "GHz", "MHz", "Cores", "Inches", "Pixels", "Hz", "Gbps", "Years", "Year", "Wh", "kg", "Hours", "nit", "nits"]
        ign_keywords = ["date", "bidder", "signature", "address", "company", "subject", "tender id", "rfp", "bid", "reference", "matrix", "overview", "invitation", "compliance", "model"]

        i = 0
        while i < len(lines):
            line = lines[i]

            # 1. Check for product model headers
            detected_prod = None
            for pat in prod_title_patterns:
                m = pat.search(line)
                if m:
                    cand = m.group(1).strip() if m.groups() else m.group(0).strip()
                    if len(cand) > 3 and not any(kw in cand.lower() for kw in ["specification", "compliance", "matrix", "date", "signature"]):
                        detected_prod = cand
                        break

            if detected_prod:
                if current_product_name == "Enterprise Hardware" or len(current_specs) == 0:
                    current_product_name = detected_prod
                elif detected_prod.lower() != current_product_name.lower():
                    # Only split if it's explicitly a distinct Item 2 / Lot 2 header
                    if re.search(r"\b(?:Item\s*[2-9]|Lot\s*[2-9]|Product\s*[2-9])\b", line, re.IGNORECASE):
                        products.append(ExtractedProduct(
                            product_name=current_product_name,
                            product_category="electronics",
                            specifications=current_specs,
                            raw_text=current_product_name
                        ))
                        current_specs = []
                        current_product_name = detected_prod
                    elif len(current_specs) <= 2:
                        # Prioritize specific model name over generic document title
                        current_product_name = detected_prod
                i += 1
                continue

            # 2. Check for natural bullet / key-value lines
            bs_match = bullet_spec_pattern.match(line)
            if bs_match:
                k_raw = bs_match.group(1).strip()
                v_raw = bs_match.group(2).strip()

                if not any(ign in k_raw.lower() for ign in ign_keywords):
                    unit = None
                    for u in known_units:
                        if re.search(rf"\b{u}\b", v_raw, re.IGNORECASE):
                            unit = u
                            break

                    current_specs.append(ExtractedSpec(
                        spec_name=k_raw,
                        spec_value=v_raw,
                        unit=unit,
                        raw_text=line,
                        extraction_confidence="high",
                        extraction_source="heuristic_natural"
                    ))
                    i += 1
                    continue

            # 3. Check for table-based canonical matched keys
            matched_key = None
            for k in canonicals:
                if line.lower() == k.lower() or line.lower().startswith(k.lower()):
                    matched_key = k
                    break

            if matched_key:
                spec_display_name = lines[i - 1] if i > 0 and len(lines[i - 1]) < 60 else matched_key.replace("_", " ").title()
                val = None
                unit = canonicals[matched_key].get("default_unit", "")
                j = i + 1
                while j < min(i + 7, len(lines)):
                    next_l = lines[j]
                    if next_l in (">=", "<=", "==", ">", "<") or next_l.upper() in ("MATCH", "UNMATCH", "PASS", "FAIL"):
                        j += 1
                        continue
                    if (re.match(r"^[\d\.]+$", next_l) or
                        next_l.startswith("Windows") or
                        next_l.startswith("Ubuntu") or
                        next_l in ("DDR5", "DDR4", "NVMe SSD", "Wi-Fi 6E", "Wi-Fi 6")):
                        val = next_l
                        if j + 1 < len(lines) and lines[j + 1] in known_units:
                            unit = lines[j + 1]
                        break
                    j += 1

                if val:
                    current_specs.append(ExtractedSpec(
                        spec_name=spec_display_name,
                        spec_value=val,
                        unit=unit,
                        raw_text=f"{spec_display_name}: {val} {unit or ''}".strip(),
                        extraction_confidence="high",
                        extraction_source="heuristic"
                    ))
                i = j
            i += 1

        if current_specs:
            products.append(ExtractedProduct(
                product_name=current_product_name,
                product_category="electronics",
                specifications=current_specs,
                raw_text=current_product_name
            ))

        return products

    def _chunk_text(self, text: str, max_chunk_chars: int) -> List[str]:
        if len(text) <= max_chunk_chars:
            return [text]

        paragraphs = text.split("\n\n")
        chunks = []
        current_chunk = []
        current_len = 0

        for p in paragraphs:
            if len(p) > max_chunk_chars:
                if current_chunk:
                    chunks.append("\n\n".join(current_chunk))
                    current_chunk = []
                    current_len = 0
                for i in range(0, len(p), max_chunk_chars):
                    chunks.append(p[i:i + max_chunk_chars])
                continue

            if current_len + len(p) > max_chunk_chars and current_chunk:
                chunks.append("\n\n".join(current_chunk))
                current_chunk = [p]
                current_len = len(p)
            else:
                current_chunk.append(p)
                current_len += len(p)

        if current_chunk:
            chunks.append("\n\n".join(current_chunk))

        return chunks

    def _build_prompt(self, chunk_text: str) -> str:
        return f"""You are an expert AI procurement system. Analyze the following tender or vendor proposal document text.
Extract ALL distinct products, equipment, line items, and their technical specifications into structured JSON format.

CRITICAL RULES FOR ACCURATE PRODUCT / ITEM EXTRACTION:
1. Multi-Item Extraction:
   - If the document contains multiple items, lots, equipment, or line items (e.g. "Item 1", "Item 2", "Item 3", or multiple equipment sections like "Oscilloscope", "Switch", "UPS", "Cable"), you MUST extract EACH item as a separate product object in the "products" array.
   - Do NOT combine or omit items. Every item listed in the document must be extracted.
   - If the document is for a single item (e.g. a single laptop proposal), return 1 product object.
2. Product Name:
   - Use the specific item or product name (e.g. "Item 1: Tektronix make Digital Oscilloscope Model-TBS1202B-EDU", "Item 2: Digital Storage Oscilloscope", "8 Port Gigabit Switch", "UPS 1 KVA").
3. Specifications Array:
   - Extract ALL specifications, included items, accessories, ratings, features, quantity, and warranty for each product.
   - For items with "Included Items" or feature lists, extract each feature/included accessory as a spec (e.g. "Passive Probe": "2 - 200 MHz passive probe (TPP0201)", "Calibration Certificate": "Traceable certificate of calibration", "Power Cord": "Included", "Software": "Educational Courseware & Open Choice Desktop Software").
   - Each spec object must include: "spec_name", "spec_value", "unit" (or null), "raw_text".

Format your response strictly as a valid JSON object:
{{"products": [{{"product_name": "...", "product_category": "electronics", "specifications": [{{"spec_name": "...", "spec_value": "...", "unit": "...", "raw_text": "..."}}]}}]}}

Document text:
{chunk_text}
"""

    def _call_llm(self, prompt: str) -> str:
        # 1. Try Google Gemini Cloud API if configured (Fast & Free)
        if settings.GEMINI_API_KEY and settings.GEMINI_API_KEY.strip():
            try:
                return self._invoke_gemini(prompt, settings.GEMINI_PRIMARY_MODEL)
            except Exception as e:
                logger.warning(f"Gemini primary model {settings.GEMINI_PRIMARY_MODEL} failed ({e}), trying fallback {settings.GEMINI_FALLBACK_MODEL}")
                try:
                    return self._invoke_gemini(prompt, settings.GEMINI_FALLBACK_MODEL)
                except Exception as e2:
                    logger.warning(f"Gemini Cloud fallback failed ({e2}), falling back to other providers.")

        # 2. Try Groq Cloud API if configured
        if settings.GROQ_API_KEY and settings.GROQ_API_KEY.strip():
            try:
                return self._invoke_groq(prompt, settings.GROQ_PRIMARY_MODEL)
            except Exception as e:
                logger.warning(f"Groq primary model {settings.GROQ_PRIMARY_MODEL} failed ({e}), trying fallback {settings.GROQ_FALLBACK_MODEL}")
                try:
                    return self._invoke_groq(prompt, settings.GROQ_FALLBACK_MODEL)
                except Exception as e2:
                    logger.warning(f"Groq Cloud fallback failed ({e2}), falling back to local Ollama.")

        # 3. Local Ollama fallback
        try:
            return self._invoke_model(prompt, self.primary_model)
        except Exception as e:
            logger.warning(f"Primary model {self.primary_model} failed ({e}), trying fallback {self.fallback_model}")
            try:
                return self._invoke_model(prompt, self.fallback_model)
            except Exception as e2:
                logger.error(f"Both LLM models failed or unavailable ({e2}). Proceeding without LLM generation.")
                return "{}"

    def _invoke_gemini(self, prompt: str, model: str) -> str:
        api_key = settings.GEMINI_API_KEY.strip()
        endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
        payload = {
            "contents": [
                {
                    "parts": [{"text": prompt}]
                }
            ],
            "generationConfig": {
                "responseMimeType": "application/json",
                "temperature": 0.1
            }
        }
        with httpx.Client(timeout=30.0) as client:
            res = client.post(endpoint, json=payload)
            if res.status_code == 200:
                data = res.json()
                return data["candidates"][0]["content"]["parts"][0]["text"]
            raise RuntimeError(f"Gemini API returned HTTP {res.status_code}: {res.text}")

    def _invoke_groq(self, prompt: str, model: str) -> str:
        headers = {
            "Authorization": f"Bearer {settings.GROQ_API_KEY.strip()}",
            "Content-Type": "application/json"
        }
        payload = {
            "model": model,
            "messages": [
                {"role": "system", "content": "You are a precise technical document extractor. Always return valid JSON."},
                {"role": "user", "content": prompt}
            ],
            "response_format": {"type": "json_object"},
            "temperature": 0.1
        }
        with httpx.Client(timeout=30.0) as client:
            res = client.post(f"{settings.GROQ_API_BASE}/chat/completions", headers=headers, json=payload)
            if res.status_code == 200:
                data = res.json()
                return data["choices"][0]["message"]["content"]
            raise RuntimeError(f"Groq API returned HTTP {res.status_code}: {res.text}")

    def _invoke_model(self, prompt: str, model: str) -> str:
        clean_model_name = model.replace("ollama/", "")
        endpoint = f"{self.ollama_api_base}/api/generate"
        payload = {
            "model": clean_model_name,
            "prompt": prompt,
            "stream": False,
            "format": "json"
        }
        try:
            with httpx.Client(timeout=90.0) as client:
                res = client.post(endpoint, json=payload)
                if res.status_code == 200:
                    return res.json().get("response", "{}")
                raise RuntimeError(f"Ollama returned HTTP {res.status_code}: {res.text}")
        except httpx.TimeoutException as te:
            raise TimeoutError(f"LLM request timed out for model {model}") from te

    def _clean_json_string(self, text: str) -> str:
        text = re.sub(r"^```(?:json)?\s*", "", text.strip(), flags=re.MULTILINE)
        text = re.sub(r"```$", "", text.strip(), flags=re.MULTILINE)
        start = text.find("{")
        end = text.rfind("}")
        if start != -1 and end != -1:
            return text[start:end+1]
        return text

    def _parse_llm_response(self, raw_response: str) -> List[ExtractedProduct]:
        cleaned = self._clean_json_string(raw_response)
        try:
            data = json.loads(cleaned)
        except Exception:
            fixed = cleaned.replace("'", '"')
            try:
                data = json.loads(fixed)
            except Exception as e:
                logger.error(f"Failed to parse LLM response as JSON: {e}")
                return []

        products_data = data.get("products", [])
        result = []
        for p in products_data:
            specs = []
            for s in p.get("specifications", []):
                specs.append(ExtractedSpec(
                    spec_name=s.get("spec_name", ""),
                    spec_value=str(s.get("spec_value", "")),
                    unit=s.get("unit"),
                    raw_text=s.get("raw_text", ""),
                    extraction_confidence="high",
                    extraction_source="llm"
                ))
            result.append(ExtractedProduct(
                product_name=p.get("product_name", "Product"),
                product_category=p.get("product_category", "electronics"),
                specifications=specs,
                raw_text=p.get("raw_text", "")
            ))
        return result

    def _merge_products(self, products: List[ExtractedProduct]) -> List[ExtractedProduct]:
        if not products:
            return []

        # 1. Filter out empty products with 0 specifications
        non_empty = [p for p in products if p.specifications and len(p.specifications) > 0]
        if not non_empty:
            return products[:1] if products else []

        if len(non_empty) == 1:
            return non_empty

        # 2. Group products by normalized name
        merged_map = {}
        generic_names = {"product", "enterprise hardware", "general", "technical specifications", "tender requirement", "item"}

        for p in non_empty:
            raw_name = p.product_name.strip()
            norm_name = raw_name.lower().replace("wlan", "wireless").replace("wi-fi", "wireless").replace("wifi", "wireless")
            cleaned_key = re.sub(r"^(?:item|lot|product)\s*\d+[\s:\-—–]+", "", norm_name).strip()

            matched_key = None
            for k in merged_map:
                # Merge only if names are identical, or if both refer to the same cleaned item
                if k == norm_name or (len(cleaned_key) > 5 and k == cleaned_key):
                    matched_key = k
                    break
                # If one is a generic placeholder and the other is specific
                if k in generic_names and norm_name not in generic_names and len(merged_map) == 1:
                    matched_key = k
                    break

                # Advanced subset/overlap merging logic
                k_cleaned = re.sub(r"^(?:item|lot|product)\s*\d+[\s:\-—–]+", "", k).strip()
                words_k = {w[:-1] if w.endswith('s') and len(w) > 3 else w for w in re.findall(r'[a-z0-9]+', k_cleaned)}
                words_p = {w[:-1] if w.endswith('s') and len(w) > 3 else w for w in re.findall(r'[a-z0-9]+', cleaned_key)}

                if words_k and words_p:
                    generic_words = {"with", "or", "and", "for", "the", "a", "an", "of", "in"}
                    wk_sig = words_k - generic_words
                    wp_sig = words_p - generic_words

                    if wk_sig and wp_sig:
                        inter = wk_sig.intersection(wp_sig)
                        if len(inter) == len(wk_sig) or len(inter) == len(wp_sig) or (len(inter) / max(len(wk_sig), len(wp_sig)) >= 0.5):
                            opposites_pairs = [("indoor", "outdoor"), ("wired", "wireless")]
                            mismatch = False
                            for (op1, op2) in opposites_pairs:
                                if (op1 in wk_sig and op2 in wp_sig) or (op2 in wk_sig and op1 in wp_sig):
                                    mismatch = True
                                    break
                            
                            nums_k = set(re.findall(r'\d+(?:\.\d+)?', k))
                            nums_p = set(re.findall(r'\d+(?:\.\d+)?', norm_name))
                            if nums_k and nums_p and nums_k != nums_p:
                                mismatch = True

                            if not mismatch:
                                matched_key = k
                                break

            if matched_key:
                target = merged_map[matched_key]
                # If existing was generic, adopt specific name
                if target.product_name.strip().lower() in generic_names and norm_name not in generic_names:
                    target.product_name = p.product_name
                existing_specs = {s.spec_name.strip().lower() for s in target.specifications}
                for s in p.specifications:
                    if s.spec_name.strip().lower() not in existing_specs:
                        target.specifications.append(s)
                        existing_specs.add(s.spec_name.strip().lower())
            else:
                merged_map[norm_name] = p

        return list(merged_map.values())
