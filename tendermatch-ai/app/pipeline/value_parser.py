import re
from typing import Optional
from pydantic import BaseModel


class ParsedValue(BaseModel):
    numeric_value: Optional[float] = None
    min_value: Optional[float] = None
    max_value: Optional[float] = None
    normalized_unit: Optional[str] = None
    is_numeric: bool = False
    is_range: bool = False
    raw_value: str = ""


class ValueParser:
    UNIT_ALIASES = {
        # Memory & Storage
        "gb": "GB", "gigabytes": "GB", "gigabyte": "GB", "gigs": "GB", "gib": "GB",
        "mb": "MB", "megabytes": "MB", "megabyte": "MB", "mib": "MB",
        "tb": "TB", "terabytes": "TB", "terabyte": "TB", "tib": "TB",
        "kb": "KB", "kilobytes": "KB",
        # Frequency
        "ghz": "GHz", "gigahertz": "GHz",
        "mhz": "MHz", "megahertz": "MHz",
        "khz": "kHz", "hz": "Hz",
        # Weight & Length
        "kg": "kg", "kilogram": "kg", "kilograms": "kg", "kgs": "kg",
        "g": "g", "gram": "g", "grams": "g",
        "mm": "mm", "millimeter": "mm", "millimeters": "mm",
        "cm": "cm", "centimeter": "cm", "centimeters": "cm",
        "m": "m", "meter": "m", "meters": "m",
        "in": "in", "inch": "in", "inches": "in", '"': "in",
        # Power & Battery
        "wh": "Wh", "watt-hour": "Wh", "watt-hours": "Wh", "whr": "Wh",
        "w": "W", "watt": "W", "watts": "W",
        "v": "V", "volt": "V", "volts": "V",
        "mah": "mAh", "milliamp-hours": "mAh",
        # Time & Speed
        "hr": "hours", "hrs": "hours", "hour": "hours", "hours": "hours",
        "min": "minutes", "mins": "minutes", "minute": "minutes",
        "sec": "seconds", "s": "seconds",
        "ppm": "ppm", "pages per minute": "ppm",
        "fps": "fps", "frames per second": "fps",
        # Display
        "nit": "nits", "nits": "nits",
    }

    RANGE_REGEX = re.compile(r'^\s*(\d+(?:[\.,]\d+)?)\s*(?:-|to|\.\.)\s*(\d+(?:[\.,]\d+)?)\s*([a-zA-Z"]+)?\s*$', re.IGNORECASE)
    SINGLE_REGEX = re.compile(r'^\s*(\d+(?:,\d{3})*(?:\.\d+)?|\d+(?:\.\d+)?)\s*([a-zA-Z"]+)?\s*$', re.IGNORECASE)

    def parse(self, raw_value: str, explicit_unit: Optional[str] = None) -> Optional[ParsedValue]:
        if not raw_value or not raw_value.strip():
            return None

        val_str = raw_value.strip()

        # 1. Check Range pattern (e.g., 8-16 GB)
        range_match = self.RANGE_REGEX.match(val_str)
        if range_match:
            min_raw, max_raw, unit_raw = range_match.groups()
            min_val = self._clean_number(min_raw)
            max_val = self._clean_number(max_raw)
            unit = self._normalize_unit(explicit_unit or unit_raw)
            return ParsedValue(
                min_value=min_val,
                max_value=max_val,
                normalized_unit=unit,
                is_numeric=True,
                is_range=True,
                raw_value=val_str
            )

        # 2. Check Single value pattern (e.g., 16GB, 16 GB, 16,000 MB)
        single_match = self.SINGLE_REGEX.match(val_str)
        if single_match:
            num_raw, unit_raw = single_match.groups()
            num_val = self._clean_number(num_raw)
            unit = self._normalize_unit(explicit_unit or unit_raw)
            return ParsedValue(
                numeric_value=num_val,
                normalized_unit=unit,
                is_numeric=True,
                is_range=False,
                raw_value=val_str
            )

        # 3. Non-numeric or complex text (e.g., "Midnight Black", "Windows 11 Pro")
        unit = self._normalize_unit(explicit_unit) if explicit_unit else None
        return ParsedValue(
            numeric_value=None,
            normalized_unit=unit,
            is_numeric=False,
            is_range=False,
            raw_value=val_str
        )

    def _clean_number(self, num_str: str) -> Optional[float]:
        try:
            # Handle comma as thousands separator (e.g. 16,000)
            if ',' in num_str and '.' not in num_str:
                parts = num_str.split(',')
                if len(parts) == 2 and len(parts[1]) == 3:
                    num_str = num_str.replace(',', '')
                else:
                    # Treat comma as decimal separator
                    num_str = num_str.replace(',', '.')
            else:
                num_str = num_str.replace(',', '')

            return float(num_str)
        except Exception:
            return None

    def _normalize_unit(self, unit_str: Optional[str]) -> Optional[str]:
        if not unit_str:
            return None
        cleaned = unit_str.strip().lower().rstrip('.,;')
        return self.UNIT_ALIASES.get(cleaned, unit_str.strip())
