import logging
from typing import List, Dict, Any, Optional
import numpy as np
import pandas as pd

logger = logging.getLogger(__name__)


class InsufficientDataError(Exception):
    """Raised when training data count does not meet the minimum required threshold."""
    pass


class TrainingDataExtractor:
    """
    Extracts, transforms, and validates training datasets from human correction logs,
    tender specifications, and award outcomes for active learning ML models.
    """

    CONFIDENCE_MAP = {"HIGH": 2, "MEDIUM": 1, "LOW": 0}

    def extract_confidence_training_data(
        self, raw_data: Optional[List[Dict[str, Any]]] = None, min_corrections: int = 50
    ) -> pd.DataFrame:
        """
        Extracts feature vectors and target labels for confidence meta-model training.
        Target: 1 if system_verdict == human_verdict, 0 otherwise.
        """
        if raw_data is None:
            raw_data = []

        if len(raw_data) < min_corrections:
            raise InsufficientDataError(
                f"Insufficient data: found {len(raw_data)} records, required minimum is {min_corrections}"
            )

        rows = []
        for item in raw_data:
            sys_v = str(item.get("system_verdict", "Unmatch")).strip().lower()
            hum_v = str(item.get("human_verdict", sys_v)).strip().lower()
            correct = 1 if sys_v == hum_v else 0

            # Map confidences
            req_c = self.CONFIDENCE_MAP.get(str(item.get("extraction_confidence_required", "LOW")).upper(), 0)
            ven_c = self.CONFIDENCE_MAP.get(str(item.get("extraction_confidence_vendor", "LOW")).upper(), 0)
            norm_c = self.CONFIDENCE_MAP.get(str(item.get("normalization_confidence", "LOW")).upper(), 0)

            method = str(item.get("comparison_method", "numeric_exact")).lower()

            row = {
                "cross_encoder_score": float(item.get("cross_encoder_score", 0.5)),
                "is_numeric": 1 if item.get("is_numeric", False) else 0,
                "unit_match": 1 if item.get("unit_match", False) else 0,
                "extraction_confidence_required": req_c,
                "extraction_confidence_vendor": ven_c,
                "normalization_confidence": norm_c,
                "value_ratio": float(item.get("value_ratio", 1.0)),
                "method_numeric_exact": 1 if "numeric" in method else 0,
                "method_cross_encoder": 1 if "cross_encoder" in method else 0,
                "method_llm_judge": 1 if "llm" in method else 0,
                "method_exact_text": 1 if "exact" in method and "numeric" not in method else 0,
                "correct": correct,
            }
            rows.append(row)

        return pd.DataFrame(rows)

    def generate_synthetic_confidence_data(self, n_samples: int = 100) -> pd.DataFrame:
        """Helper to generate well-formed synthetic data for testing and baseline initialization."""
        rng = np.random.default_rng(42)
        rows = []
        for _ in range(n_samples):
            score = float(rng.uniform(0.3, 1.0))
            is_num = int(rng.choice([0, 1]))
            u_match = int(rng.choice([0, 1]))
            req_c = int(rng.choice([0, 1, 2]))
            ven_c = int(rng.choice([0, 1, 2]))
            norm_c = int(rng.choice([0, 1, 2]))
            v_ratio = float(rng.uniform(0.5, 1.5))
            method_idx = int(rng.choice([0, 1, 2, 3]))

            # Deterministic signal with small noise
            signal = 0.5 * score + 0.3 * u_match + 0.1 * (req_c / 2.0)
            correct = 1 if signal > 0.55 else 0

            row = {
                "cross_encoder_score": score,
                "is_numeric": is_num,
                "unit_match": u_match,
                "extraction_confidence_required": req_c,
                "extraction_confidence_vendor": ven_c,
                "normalization_confidence": norm_c,
                "value_ratio": v_ratio,
                "method_numeric_exact": 1 if method_idx == 0 else 0,
                "method_cross_encoder": 1 if method_idx == 1 else 0,
                "method_llm_judge": 1 if method_idx == 2 else 0,
                "method_exact_text": 1 if method_idx == 3 else 0,
                "correct": correct,
            }
            rows.append(row)

        return pd.DataFrame(rows)

    def generate_synthetic_operator_data(self, n_samples: int = 60) -> pd.DataFrame:
        """Generates synthetic dataset for DecisionTree operator inference model."""
        rng = np.random.default_rng(42)
        operators = [">=", "<=", "==", "contains", "semantic"]
        unit_types = ["storage", "frequency", "weight", "count", "dimension", "none"]
        value_types = ["numeric", "text", "boolean"]

        rows = []
        for _ in range(n_samples):
            u_type = str(rng.choice(unit_types))
            v_type = "numeric" if u_type != "none" else str(rng.choice(value_types))
            
            # Deterministic/semi-deterministic operator rule mapping
            if u_type in ["storage", "frequency"]:
                op = ">="
            elif u_type == "weight":
                op = "<="
            elif v_type == "boolean":
                op = "=="
            elif v_type == "text":
                op = rng.choice(["contains", "semantic"])
            else:
                op = rng.choice(operators)

            row = {
                "unit_type": u_type,
                "value_type": v_type,
                "is_numeric": 1 if v_type == "numeric" else 0,
                "feature_spec_length": int(rng.integers(3, 25)),
                "correct_operator": op,
            }
            rows.append(row)

        return pd.DataFrame(rows)

    def generate_synthetic_doctype_data(self, n_samples: int = 50) -> pd.DataFrame:
        """Generates synthetic text samples and document type labels for SGDClassifier."""
        rng = np.random.default_rng(42)
        doctypes = [
            ("tender_requirement", "Request for Proposal tender requirement technical compliance criteria scope of work bid submission deadline specifications"),
            ("vendor_proposal", "Technical proposal offered solution compliance statement specification sheet delivery schedule bid offer by vendor"),
            ("technical_datasheet", "Product technical datasheet hardware specifications operating temperature voltage power dimensions warranty"),
            ("commercial_bid", "Financial commercial proposal bill of quantities pricing quotation cost taxes currency total amount"),
        ]

        rows = []
        for _ in range(n_samples):
            dt, base_text = doctypes[rng.integers(0, len(doctypes))]
            # Add some jitter words
            jitter = " ".join([f"item_{i}" for i in rng.integers(100, 999, size=5)])
            rows.append({"text": f"{base_text} {jitter}", "doc_type": dt})

        return pd.DataFrame(rows)

    def generate_synthetic_normalization_data(self, n_samples: int = 50) -> pd.DataFrame:
        """Generates synthetic embedding vectors (16-dim for speed) and canonical category labels."""
        rng = np.random.default_rng(42)
        categories = ["ram_capacity", "cpu_cores", "storage_capacity", "display_size", "battery_life"]

        rows = []
        for i in range(n_samples):
            cat_idx = i % len(categories)
            cat = categories[cat_idx]
            # Create a cluster center with small Gaussian noise
            base = np.zeros(16)
            base[cat_idx * 3 : cat_idx * 3 + 3] = 1.0
            emb = base + rng.normal(0, 0.1, 16)
            rows.append({"embedding": emb.tolist(), "canonical_name": cat})

        return pd.DataFrame(rows)

    def generate_synthetic_ranking_data(
        self, n_tenders: int = 10, vendors_per_tender: int = 3
    ) -> pd.DataFrame:
        """Generates synthetic vendor match features and award relevance labels for LambdaRank."""
        rng = np.random.default_rng(42)
        rows = []
        for t_idx in range(n_tenders):
            t_id = f"tender-{t_idx + 1}"
            # Generate scores for each vendor in the tender
            v_scores = []
            for v_idx in range(vendors_per_tender):
                match_ratio = float(rng.uniform(0.5, 0.98))
                crit_ratio = float(min(1.0, match_ratio + rng.uniform(-0.1, 0.1)))
                high_ratio = float(min(1.0, match_ratio + rng.uniform(-0.1, 0.1)))
                ai_err = float(rng.uniform(0.0, 0.1))
                overrides = float(rng.uniform(0.0, 0.15))
                conf_mean = float(rng.uniform(0.7, 0.95))
                conf_std = float(rng.uniform(0.05, 0.15))
                v_scores.append(
                    {
                        "tender_id": t_id,
                        "vendor_id": f"vendor-{t_idx}-{v_idx}",
                        "overall_match_ratio": match_ratio,
                        "critical_spec_match_ratio": crit_ratio,
                        "high_spec_match_ratio": high_ratio,
                        "ai_err_ratio": ai_err,
                        "override_ratio": overrides,
                        "confidence_mean": conf_mean,
                        "confidence_std": conf_std,
                    }
                )

            # Sort by match_ratio descending to assign relevance: 2 = winner, 1 = runner-up, 0 = non-winner
            v_scores.sort(key=lambda x: x["overall_match_ratio"], reverse=True)
            for rank_idx, v in enumerate(v_scores):
                v["relevance_label"] = max(0, 2 - rank_idx)
                rows.append(v)

        return pd.DataFrame(rows)
