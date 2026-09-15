import pytest
from app.pipeline.value_parser import ValueParser, ParsedValue


@pytest.fixture
def parser():
    return ValueParser()


def test_parse_value_with_unit(parser):
    res = parser.parse("16GB")
    assert isinstance(res, ParsedValue)
    assert res.is_numeric is True
    assert res.numeric_value == 16.0
    assert res.normalized_unit == "GB"


def test_parse_value_with_space(parser):
    res = parser.parse("16 GB")
    assert res.numeric_value == 16.0
    assert res.normalized_unit == "GB"


def test_parse_full_unit_name(parser):
    res = parser.parse("16 Gigabytes")
    assert res.numeric_value == 16.0
    assert res.normalized_unit == "GB"


def test_parse_comma_decimal(parser):
    res = parser.parse("16,000 MB")
    assert res.numeric_value == 16000.0
    assert res.normalized_unit == "MB"


def test_parse_range(parser):
    res = parser.parse("8-16 GB")
    assert res.is_range is True
    assert res.min_value == 8.0
    assert res.max_value == 16.0
    assert res.normalized_unit == "GB"


def test_non_numeric_value(parser):
    res = parser.parse("Midnight Black")
    assert res.is_numeric is False
    assert res.numeric_value is None
    assert res.raw_value == "Midnight Black"


def test_unit_alias_normalization(parser):
    res1 = parser.parse("3.2 gigahertz")
    assert res1.numeric_value == 3.2
    assert res1.normalized_unit == "GHz"

    res2 = parser.parse("1.5 kilograms")
    assert res2.numeric_value == 1.5
    assert res2.normalized_unit == "kg"


def test_empty_value_returns_none(parser):
    res = parser.parse("")
    assert res is None
