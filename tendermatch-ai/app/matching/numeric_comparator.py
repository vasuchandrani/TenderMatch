from typing import Optional, Dict, Any

UNIT_CONVERSIONS = {
    ("mb", "gb"): lambda x: x / 1024.0,
    ("gb", "tb"): lambda x: x / 1024.0,
    ("tb", "gb"): lambda x: x * 1024.0,
    ("gb", "mb"): lambda x: x * 1024.0,
    ("kb", "mb"): lambda x: x / 1024.0,
    ("mb", "kb"): lambda x: x * 1024.0,
    ("cm", "mm"): lambda x: x * 10.0,
    ("mm", "cm"): lambda x: x / 10.0,
    ("in", "cm"): lambda x: x * 2.54,
    ("cm", "in"): lambda x: x / 2.54,
    ("kg", "g"): lambda x: x * 1000.0,
    ("g", "kg"): lambda x: x / 1000.0,
    ("hours", "days"): lambda x: x / 24.0,
    ("days", "hours"): lambda x: x * 24.0,
    ("months", "years"): lambda x: x / 12.0,
    ("years", "months"): lambda x: x * 12.0,
}


UNIT_GROUPS = {
    # Data Storage
    "b": "storage", "byte": "storage", "bytes": "storage", "kb": "storage", "mb": "storage", "gb": "storage", "tb": "storage", "pb": "storage",
    # Frequency
    "hz": "frequency", "khz": "frequency", "mhz": "frequency", "ghz": "frequency",
    # Data Rate / Throughput
    "bps": "datarate", "kbps": "datarate", "mbps": "datarate", "gbps": "datarate", "tbps": "datarate",
    # Length / Dimensions
    "mm": "length", "cm": "length", "m": "length", "km": "length", "in": "length", "inch": "length", "inches": "length", "ft": "length", "feet": "length",
    # Weight
    "mg": "weight", "g": "weight", "kg": "weight", "lbs": "weight", "oz": "weight",
    # Power (Active Watts)
    "w": "power", "kw": "power", "mw": "power", "watt": "power", "watts": "power",
    # Apparent Power (Volt-Amperes)
    "va": "apparent_power", "kva": "apparent_power", "mva": "apparent_power",
    # Energy / Battery
    "wh": "energy", "kwh": "energy", "mwh": "energy", "j": "energy", "kj": "energy", "mah": "energy", "ah": "energy",
    # Voltage & Current
    "v": "voltage", "volt": "voltage", "volts": "voltage", "kv": "voltage", "mv": "voltage",
    "a": "current", "amp": "current", "amps": "current", "ma": "current",
    # Display / Brightness
    "nit": "brightness", "nits": "brightness", "cd/m2": "brightness", "cd/m²": "brightness",
    # Sound / Gain
    "db": "gain", "dbi": "gain", "dbm": "gain",
    # Time / Duration
    "s": "time", "sec": "time", "seconds": "time", "min": "time", "mins": "time", "minutes": "time", "h": "time", "hr": "time", "hrs": "time", "hours": "time", "days": "time", "months": "time", "years": "time", "yr": "time", "yrs": "time",
    # Count / Hardware Items
    "ports": "count", "port": "count", "cores": "count", "core": "count", "threads": "count", "thread": "count",
    "units": "count", "unit": "count", "items": "count", "item": "count", "licenses": "count", "lic": "count",
    "users": "count", "user": "count", "no": "count", "nos": "count", "qty": "count",
    "aps": "count", "ap": "count", "wireless ap": "count", "wireless aps": "count", "access point": "count", "access points": "count"
}


def _clean_unit(u: Optional[str]) -> str:
    if not u:
        return ""
    cleaned = u.strip().lower().rstrip(".").replace("-", " ")
    # remove leading/trailing extra punctuation
    return " ".join(cleaned.split())


def convert_units(val: float, from_unit: Optional[str], to_unit: Optional[str]) -> float:
    if not from_unit or not to_unit:
        return val
    f = _clean_unit(from_unit)
    t = _clean_unit(to_unit)
    if f == t:
        return val
    pair = (f, t)
    if pair in UNIT_CONVERSIONS:
        return UNIT_CONVERSIONS[pair](val)
    return val


def compare_numeric(
    required_val: Optional[float],
    vendor_val: Optional[float],
    operator: str = ">=",
    required_unit: Optional[str] = None,
    vendor_unit: Optional[str] = None,
    min_val: Optional[float] = None,
    max_val: Optional[float] = None,
    tolerance: float = 0.0,
) -> Dict[str, Any]:
    """
    Compares required numeric specification against offered vendor value.
    Returns verdict ('match', 'unmatch', 'uncertain'), confidence, method, and details.
    """
    if vendor_val is None:
        return {
            "verdict": "uncertain",
            "confidence": 0.20,
            "method": "missing_value",
            "details": {"error": "Vendor value is missing or non-numeric"},
        }

    req_u = _clean_unit(required_unit)
    ven_u = _clean_unit(vendor_unit)

    # Unit compatibility check: Reject comparisons across incompatible physical dimensions or different unconvertible units
    if req_u and ven_u and req_u != ven_u:
        req_group = UNIT_GROUPS.get(req_u)
        ven_group = UNIT_GROUPS.get(ven_u)

        # 1. Known groups that are different
        if req_group and ven_group and req_group != ven_group:
            return {
                "verdict": "unmatch",
                "confidence": 0.98,
                "method": "incompatible_units",
                "details": {
                    "error": f"Incompatible physical units: required '{required_unit}' ({req_group}) vs offered '{vendor_unit}' ({ven_group})",
                    "required_unit": required_unit,
                    "vendor_unit": vendor_unit,
                },
            }

        # 2. If not convertible and at least one is recognized in a distinct group or they are different named units
        is_convertible = (req_u, ven_u) in UNIT_CONVERSIONS or (ven_u, req_u) in UNIT_CONVERSIONS
        if not is_convertible:
            # Different physical units with no known conversion
            return {
                "verdict": "unmatch",
                "confidence": 0.98,
                "method": "incompatible_units",
                "details": {
                    "error": f"Incompatible unconvertible units: required '{required_unit}' vs offered '{vendor_unit}'",
                    "required_unit": required_unit,
                    "vendor_unit": vendor_unit,
                },
            }

    norm_vendor_val = vendor_val
    if vendor_unit and required_unit:
        norm_vendor_val = convert_units(vendor_val, vendor_unit, required_unit)

    # Range comparison (e.g. 8-16 GB required)
    if min_val is not None and max_val is not None:
        matched = (min_val * (1 - tolerance)) <= norm_vendor_val <= (max_val * (1 + tolerance))
        return {
            "verdict": "match" if matched else "unmatch",
            "confidence": 0.95,
            "method": "numeric_exact",
            "details": {
                "min": min_val,
                "max": max_val,
                "vendor": norm_vendor_val,
                "operator": "range",
                "normalized_vendor_val": norm_vendor_val,
            },
        }

    if required_val is None:
        return {
            "verdict": "uncertain",
            "confidence": 0.30,
            "method": "missing_value",
            "details": {"error": "Required numeric target is missing"},
        }

    margin = 0.0
    if required_val != 0:
        margin = (norm_vendor_val - required_val) / required_val

    if operator == ">=":
        matched = norm_vendor_val >= (required_val * (1 - tolerance))
    elif operator == "<=":
        matched = norm_vendor_val <= (required_val * (1 + tolerance))
    elif operator == "==":
        matched = abs(norm_vendor_val - required_val) <= (abs(required_val) * (tolerance if tolerance > 0 else 0.0001))
    else:
        matched = norm_vendor_val >= required_val

    # Clear difference produces high confidence
    if matched:
        confidence = 0.95 if abs(margin) >= 0.05 or operator == "==" else 0.85
    else:
        confidence = 0.95 if abs(margin) >= 0.05 or operator == "==" else 0.75

    return {
        "verdict": "match" if matched else "unmatch",
        "confidence": round(confidence, 3),
        "method": "numeric_exact",
        "details": {
            "required": required_val,
            "vendor": vendor_val,
            "normalized_vendor_val": norm_vendor_val,
            "required_unit": required_unit,
            "vendor_unit": vendor_unit,
            "operator": operator,
            "margin_percent": round(margin * 100, 2),
        },
    }
