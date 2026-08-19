import re
from typing import Optional, Dict, List
from pydantic import BaseModel
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity


class NormalizedSpecKey(BaseModel):
    canonical_name: str
    display_name: str
    category: str
    confidence: str  # 'high', 'medium', 'low'
    similarity_score: float
    preserved_value: Optional[str] = None


class SpecNormalizer:
    SEED_ONTOLOGY = [
        {
            "canonical": "ram_capacity",
            "display": "RAM Capacity",
            "category": "memory",
            "synonyms": ["ram", "memory", "system memory", "random access memory", "installed ram", "main memory"]
        },
        {
            "canonical": "ram_type",
            "display": "RAM Type",
            "category": "memory",
            "synonyms": ["ram type", "memory type", "ddr type", "ddr4", "ddr5", "lpddr5", "memory technology"]
        },
        {
            "canonical": "storage_capacity",
            "display": "Storage Capacity",
            "category": "storage",
            "synonyms": [
                "storage", "ssd", "solid state drive", "internal storage", "hdd",
                "hard drive", "disk capacity", "drive capacity", "rom", "storage drive",
                "disk drive volume space"
            ]
        },
        {
            "canonical": "processor_model",
            "display": "Processor Model",
            "category": "processing",
            "synonyms": ["processor", "cpu", "chipset", "cpu model", "processor model", "cpu type"]
        },
        {
            "canonical": "processor_clock_speed",
            "display": "Clock Speed",
            "category": "processing",
            "synonyms": ["clock speed", "base clock", "turbo boost", "frequency", "cpu speed", "ghz"]
        },
        {
            "canonical": "display_size",
            "display": "Display Size",
            "category": "display",
            "synonyms": ["screen size", "display size", "monitor size", "diagonal", "screen"]
        },
        {
            "canonical": "display_resolution",
            "display": "Display Resolution",
            "category": "display",
            "synonyms": ["resolution", "display resolution", "screen resolution", "pixels", "fhd", "uhd"]
        },
        {
            "canonical": "battery_capacity",
            "display": "Battery Capacity",
            "category": "power",
            "synonyms": ["battery", "battery size", "battery capacity", "watt hours", "whr", "mah"]
        },
        {
            "canonical": "device_weight",
            "display": "Weight",
            "category": "dimensions",
            "synonyms": ["weight", "item weight", "mass", "starting weight"]
        },
        {
            "canonical": "operating_system",
            "display": "Operating System",
            "category": "software",
            "synonyms": ["os", "operating system", "software platform", "preloaded os"]
        },
        {
            "canonical": "warranty_period",
            "display": "Warranty",
            "category": "service",
            "synonyms": ["warranty", "support period", "service agreement", "on-site warranty"]
        }
    ]

    def __init__(self):
        self.synonym_map: Dict[str, dict] = {}
        self.corpus: List[str] = []
        self.corpus_canonical: List[dict] = []

        for entry in self.SEED_ONTOLOGY:
            # Map canonical itself
            self.synonym_map[entry["canonical"].lower()] = entry
            self.synonym_map[entry["display"].lower()] = entry

            for syn in entry["synonyms"]:
                syn_clean = syn.strip().lower()
                self.synonym_map[syn_clean] = entry
                self.corpus.append(syn_clean)
                self.corpus_canonical.append(entry)

        # Build TF-IDF vectorizer for semantic search over ontology
        self.vectorizer = TfidfVectorizer(analyzer="char_wb", ngram_range=(3, 5))
        self.tfidf_matrix = self.vectorizer.fit_transform(self.corpus)

    def normalize(self, spec_name: str, original_value: Optional[str] = None) -> NormalizedSpecKey:
        if not spec_name or not spec_name.strip():
            return NormalizedSpecKey(
                canonical_name="unknown",
                display_name="Unknown",
                category="other",
                confidence="low",
                similarity_score=0.0,
                preserved_value=original_value
            )

        clean_name = spec_name.strip().lower()

        # 1. Exact or direct synonym match
        if clean_name in self.synonym_map:
            match = self.synonym_map[clean_name]
            return NormalizedSpecKey(
                canonical_name=match["canonical"],
                display_name=match["display"],
                category=match["category"],
                confidence="high",
                similarity_score=1.0,
                preserved_value=original_value
            )

        # 2. Vectorized character n-gram cosine similarity
        query_vec = self.vectorizer.transform([clean_name])
        sims = cosine_similarity(query_vec, self.tfidf_matrix).flatten()
        best_idx = int(sims.argmax())
        best_score = float(sims[best_idx])

        if best_score >= 0.70:
            match = self.corpus_canonical[best_idx]
            return NormalizedSpecKey(
                canonical_name=match["canonical"],
                display_name=match["display"],
                category=match["category"],
                confidence="high",
                similarity_score=round(best_score, 3),
                preserved_value=original_value
            )
        elif best_score >= 0.45:
            match = self.corpus_canonical[best_idx]
            return NormalizedSpecKey(
                canonical_name=match["canonical"],
                display_name=match["display"],
                category=match["category"],
                confidence="medium",
                similarity_score=round(best_score, 3),
                preserved_value=original_value
            )
        else:
            # Low confidence: preserve original name as slug
            slug = re.sub(r'[^a-zA-Z0-9]+', '_', clean_name).strip('_')
            return NormalizedSpecKey(
                canonical_name=slug or "unknown",
                display_name=spec_name.strip(),
                category="other",
                confidence="low",
                similarity_score=round(best_score, 3),
                preserved_value=original_value
            )
