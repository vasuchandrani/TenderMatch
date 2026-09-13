import hashlib
import json
import logging
from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)


class ShadowModelRouter:
    """
    Routes inference requests through both rule-based engines and ML models.
    In shadow mode, executes both models and logs comparisons, but strictly returns
    the rule-based output to preserve system safety until promotion.
    """

    def __init__(
        self,
        rule_engine: Any,
        ml_model: Any,
        model_name: str,
        model_version: int = 1,
        shadow_mode: bool = True,
    ):
        self.rule_engine = rule_engine
        self.ml_model = ml_model
        self.model_name = model_name
        self.model_version = model_version
        self.shadow_mode = shadow_mode
        self.prediction_history: List[Dict[str, Any]] = []

    def predict(self, input_data: Any) -> Any:
        """
        Executes prediction on both rule engine and ML model, records comparison,
        and returns the appropriate result based on shadow_mode flag.
        """
        rule_result = self.rule_engine.predict(input_data)
        ml_result = self.ml_model.predict(input_data)

        agreed = bool(rule_result == ml_result)
        input_str = json.dumps(input_data, sort_keys=True, default=str)
        input_hash = hashlib.sha256(input_str.encode("utf-8")).hexdigest()[:16]

        record = {
            "model_name": self.model_name,
            "model_version": self.model_version,
            "input_hash": input_hash,
            "rule_result": rule_result,
            "ml_result": ml_result,
            "agreed": agreed,
            "input_sample": str(input_data)[:200],
        }
        self.prediction_history.append(record)

        if not agreed:
            logger.info(
                f"Shadow disagreement for {self.model_name}: Rule='{rule_result}' vs ML='{ml_result}'"
            )

        if self.shadow_mode:
            return rule_result
        return ml_result

    def get_shadow_stats(self) -> Dict[str, Any]:
        """Calculates agreement rate and compiles disagreement examples."""
        total = len(self.prediction_history)
        if total == 0:
            return {
                "model_name": self.model_name,
                "total_predictions": 0,
                "agreement_count": 0,
                "disagreement_count": 0,
                "agreement_rate": 1.0,
                "disagreements": [],
            }

        agreed_count = sum(1 for p in self.prediction_history if p["agreed"])
        disagreements = [p for p in self.prediction_history if not p["agreed"]]

        return {
            "model_name": self.model_name,
            "total_predictions": total,
            "agreement_count": agreed_count,
            "disagreement_count": len(disagreements),
            "agreement_rate": round(agreed_count / total, 4),
            "disagreements": disagreements[-20:],  # last 20 disagreements
        }
