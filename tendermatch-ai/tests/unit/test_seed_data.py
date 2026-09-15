import json
import subprocess
import sys
from pathlib import Path
import pytest

DATA_DIR = Path(__file__).resolve().parent.parent.parent / "data"
SCRIPTS_DIR = Path(__file__).resolve().parent.parent.parent / "scripts"

VALID_OPERATORS = {">=", "<=", "==", "contains", "semantic"}


def test_ontology_seed_structure():
    seed_path = DATA_DIR / "ontology_seed.json"
    assert seed_path.exists(), f"Missing {seed_path}"

    with open(seed_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    assert isinstance(data, list)
    assert len(data) >= 15, "Expected at least 15 canonical specs"

    canonical_names = set()
    for item in data:
        assert "canonical_name" in item
        assert "display_name" in item
        assert "category" in item
        assert "default_operator" in item
        assert "synonyms" in item
        assert isinstance(item["synonyms"], list)
        assert len(item["synonyms"]) > 0

        assert item["default_operator"] in VALID_OPERATORS
        assert item["canonical_name"] not in canonical_names, f"Duplicate canonical_name: {item['canonical_name']}"
        canonical_names.add(item["canonical_name"])


def test_unit_aliases_structure():
    unit_path = DATA_DIR / "unit_aliases.json"
    assert unit_path.exists(), f"Missing {unit_path}"

    with open(unit_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    assert "units" in data
    units = data["units"]

    expected_categories = ["storage", "frequency", "weight", "time", "network_speed"]
    for cat in expected_categories:
        assert cat in units, f"Missing category {cat}"
        cat_data = units[cat]
        assert "base_unit" in cat_data
        assert "conversions" in cat_data
        assert "aliases" in cat_data
        assert len(cat_data["conversions"]) > 0
        assert len(cat_data["aliases"]) > 0

    # Storage conversions check: 1 GB = 1024 MB
    assert units["storage"]["conversions"]["GB"] == 1024.0
    assert units["storage"]["conversions"]["MB"] == 1.0


def test_operator_rules_structure():
    op_path = DATA_DIR / "operator_rules.json"
    assert op_path.exists(), f"Missing {op_path}"

    with open(op_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    assert "category_defaults" in data
    assert "spec_rules" in data
    assert "type_fallbacks" in data

    for cat, op in data["category_defaults"].items():
        assert op in VALID_OPERATORS, f"Invalid operator {op} for category {cat}"

    for spec, op in data["spec_rules"].items():
        assert op in VALID_OPERATORS, f"Invalid operator {op} for spec {spec}"

    for t, op in data["type_fallbacks"].items():
        assert op in VALID_OPERATORS, f"Invalid operator {op} for type {t}"


def test_seed_script_dry_run():
    script_path = SCRIPTS_DIR / "seed_ontology.py"
    assert script_path.exists(), f"Missing {script_path}"

    result = subprocess.run(
        [sys.executable, str(script_path), "--dry-run"],
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0
    assert "Dry run complete" in result.stdout
