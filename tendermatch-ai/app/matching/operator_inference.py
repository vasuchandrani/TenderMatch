from typing import Optional


class OperatorInference:
    """
    Infers comparison operators (>=, <=, ==, contains, semantic)
    based on canonical spec keys and value types.
    """

    OPERATOR_RULES = {
        # "Greater or equal" specs — vendor must offer AT LEAST the required value
        "ram_capacity": ">=",
        "storage_capacity": ">=",
        "processor_speed": ">=",
        "battery_capacity": ">=",
        "display_size": ">=",
        "resolution_width": ">=",
        "resolution_height": ">=",
        "warranty_period": ">=",
        "usb_ports": ">=",
        "bandwidth": ">=",

        # "Less or equal" specs — vendor must offer AT MOST the required value
        "weight": "<=",
        "power_consumption": "<=",
        "noise_level": "<=",
        "delivery_time": "<=",
        "price": "<=",
        "dimensions_length": "<=",
        "dimensions_width": "<=",
        "dimensions_height": "<=",

        # "Exact match" specs — must match exactly
        "ram_type": "==",
        "processor_model": "==",
        "os_version": "==",
        "certification": "==",
        "connector_type": "==",
        "color": "==",
        "voltage": "==",
        "form_factor": "==",

        # "Contains / semantic"
        "features": "contains",
        "description": "semantic",
        "compliance": "semantic",
        "support_terms": "semantic",
    }

    @classmethod
    def infer(cls, canonical_name: Optional[str], is_numeric: bool = True) -> str:
        if canonical_name:
            norm_key = canonical_name.strip().lower()
            if norm_key in cls.OPERATOR_RULES:
                return cls.OPERATOR_RULES[norm_key]

        return ">=" if is_numeric else "semantic"
