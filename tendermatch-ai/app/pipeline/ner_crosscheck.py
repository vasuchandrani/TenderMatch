import re
from typing import List, Dict, Any
from app.pipeline.spec_extractor import ExtractedProduct, ExtractedSpec


class NerCrossChecker:
    VALUE_PATTERNS = [
        r'\b\d+(?:[\.,]\d+)?\s*(?:gb|mb|tb|ghz|mhz|inch|inches|kg|g|mm|cm|wh|w|v|mah|ppm|nit|nits|fps|core|cores|threads)\b',
        r'\bddr[345]\b',
        r'\bnvme(?:\s*ssd)?\b',
        r'\bpcie(?:\s*gen\s*\d)?\b',
        r'\bwi-?fi\s*[567]e?\b',
        r'\bbluetooth\s*\d(?:\.\d)?\b',
    ]

    NAME_PATTERNS = [
        r'\b(?:ram|memory|storage|ssd|hdd|hard drive|internal storage|processor|cpu|gpu|graphics|graphics card|battery|display|screen|weight|dimensions|resolution|operating system|os|warranty)\b'
    ]

    def extract_entities(self, text: str) -> List[Dict[str, Any]]:
        entities = []
        text_lower = text.lower()

        # Find spec names
        for pattern in self.NAME_PATTERNS:
            for match in re.finditer(pattern, text_lower):
                entities.append({
                    "label": "SPEC_NAME",
                    "text": match.group(0),
                    "start": match.start(),
                    "end": match.end(),
                })

        # Find spec values
        for pattern in self.VALUE_PATTERNS:
            for match in re.finditer(pattern, text_lower):
                entities.append({
                    "label": "SPEC_VALUE",
                    "text": match.group(0),
                    "start": match.start(),
                    "end": match.end(),
                })

        return entities

    def cross_check(self, products: List[ExtractedProduct], raw_text: str) -> List[ExtractedProduct]:
        if not products or not raw_text:
            return products

        raw_text_lower = raw_text.lower()
        entities = self.extract_entities(raw_text)
        ner_values = {e["text"].lower() for e in entities if e["label"] == "SPEC_VALUE"}
        ner_names = {e["text"].lower() for e in entities if e["label"] == "SPEC_NAME"}

        for product in products:
            existing_spec_names = set()

            for spec in product.specifications:
                name_lower = spec.spec_name.strip().lower()
                val_lower = spec.spec_value.strip().lower()
                existing_spec_names.add(name_lower)

                # Check if value appears in raw_text or NER entities
                val_in_raw = (val_lower in raw_text_lower) or any(v in val_lower for v in ner_values) or (spec.raw_text and spec.raw_text.lower() in raw_text_lower)
                name_in_ner = name_lower in ner_names or any(n in name_lower for n in ner_names)

                if val_in_raw:
                    spec.extraction_confidence = "high"
                    if name_in_ner:
                        spec.extraction_source = "both"
                    else:
                        spec.extraction_source = "llm"
                else:
                    spec.extraction_confidence = "low"
                    spec.extraction_source = "llm"

            if len(products) == 1:
                # Check for NER specs not captured by LLM (only for single product documents)
                # e.g. "Weight: 1.4kg"
                line_pattern = r'([A-Za-z\s]{3,20}):\s*([0-9]+(?:[\.,][0-9]+)?\s*[A-Za-z]+)'
                for m in re.finditer(line_pattern, raw_text):
                    found_name = m.group(1).strip()
                    found_val = m.group(2).strip()
                    if found_name.lower() in ner_names and not any(found_name.lower() in ex for ex in existing_spec_names):
                        product.specifications.append(ExtractedSpec(
                            spec_name=found_name.capitalize(),
                            spec_value=found_val,
                            raw_text=m.group(0),
                            extraction_confidence="high",
                            extraction_source="ner_only"
                        ))
                        existing_spec_names.add(found_name.lower())

        return products
