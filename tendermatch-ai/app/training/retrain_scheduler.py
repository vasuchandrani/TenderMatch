import logging
from datetime import datetime, timezone
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)


class RetrainingScheduler:
    """
    Checks if active learning models need retraining based on minimum volume,
    new accumulated correction thresholds, and maximum staleness durations.
    """

    RETRAIN_CONFIG = {
        "confidence_model": {
            "min_total_data": 500,
            "min_new_data": 100,
            "max_staleness_days": 30,
        },
        "operator_model": {
            "min_total_data": 500,
            "min_new_data": 50,
            "max_staleness_days": 60,
        },
        "doctype_model": {
            "min_total_data": 100,
            "min_new_data": 20,
            "max_staleness_days": 60,
        },
        "normalization_model": {
            "min_total_data": 200,
            "min_new_data": 50,
            "max_staleness_days": 60,
        },
        "ranking_model": {
            "min_total_data": 200,
            "min_new_data": 50,
            "max_staleness_days": 60,
        },
    }

    def check_model_retrain_needed(
        self,
        model_name: str,
        total_data_count: int,
        new_data_count: int,
        last_trained_at: Optional[datetime] = None,
    ) -> Dict[str, Any]:
        """
        Evaluates whether a model warrants retraining.
        """
        config = self.RETRAIN_CONFIG.get(
            model_name,
            {"min_total_data": 100, "min_new_data": 20, "max_staleness_days": 30},
        )

        # 1. Minimum total volume check
        if total_data_count < config["min_total_data"]:
            return {
                "model_name": model_name,
                "retrain_needed": False,
                "reason": (
                    f"Insufficient total data: {total_data_count} / {config['min_total_data']} minimum required"
                ),
                "total_data_count": total_data_count,
                "new_data_count": new_data_count,
            }

        # 2. Significant new data check
        if new_data_count >= config["min_new_data"]:
            return {
                "model_name": model_name,
                "retrain_needed": True,
                "reason": (
                    f"New data threshold met: {new_data_count} new samples (threshold: {config['min_new_data']})"
                ),
                "total_data_count": total_data_count,
                "new_data_count": new_data_count,
            }

        # 3. Staleness check
        if last_trained_at is not None:
            now = datetime.now(timezone.utc)
            if last_trained_at.tzinfo is None:
                last_trained_at = last_trained_at.replace(tzinfo=timezone.utc)
            age_days = (now - last_trained_at).days
            if age_days >= config["max_staleness_days"]:
                return {
                    "model_name": model_name,
                    "retrain_needed": True,
                    "reason": (
                        f"Model is stale: trained {age_days} days ago (max allowed: {config['max_staleness_days']})"
                    ),
                    "total_data_count": total_data_count,
                    "new_data_count": new_data_count,
                }

        return {
            "model_name": model_name,
            "retrain_needed": False,
            "reason": "Model is current and has not reached retraining triggers",
            "total_data_count": total_data_count,
            "new_data_count": new_data_count,
        }
