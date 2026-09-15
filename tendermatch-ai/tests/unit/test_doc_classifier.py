import pytest
from app.pipeline.doc_classifier import DocumentClassifier, ClassificationResult


@pytest.fixture
def classifier():
    return DocumentClassifier()


def test_classify_requirement_tender(classifier):
    text = (
        "REQUEST FOR PROPOSAL (RFP) - IT HARDWARE PROCUREMENT\n"
        "Technical Requirements:\n"
        "The bidder must meet all the minimum specifications described herein.\n"
        "Tender Notice: Bids must be submitted before the deadline."
    )
    res = classifier.classify(text)

    assert isinstance(res, ClassificationResult)
    assert res.doc_type == "requirement_tender"
    assert res.confidence >= 0.70


def test_classify_vendor_response(classifier):
    text = (
        "TECHNICAL OFFER AND BID RESPONSE\n"
        "Submitted by Apex Solutions Inc.\n"
        "We propose the following compliance matrix and equipment models:\n"
        "Model: ThinkPad T14, Part Number: 21AH001"
    )
    res = classifier.classify(text)

    assert res.doc_type == "vendor_response"
    assert res.confidence >= 0.70


def test_classify_unknown_document(classifier):
    text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Random generic blog article text."
    res = classifier.classify(text)

    assert res.doc_type == "unknown"
    assert res.confidence < 0.50


def test_confidence_reflects_keyword_density(classifier):
    dense_req = "RFP Request for Proposal Technical Requirements Minimum Specifications must meet shall comply Tender Notice"
    sparse_req = "This note mentions RFP once."

    res_dense = classifier.classify(dense_req)
    res_sparse = classifier.classify(sparse_req)

    assert res_dense.doc_type == "requirement_tender"
    assert res_sparse.doc_type == "requirement_tender"
    assert res_dense.confidence > res_sparse.confidence


def test_case_insensitive_matching(classifier):
    text = "rEqUeSt FoR pRoPoSaL - tEcHnIcAl ReQuIrEmEnTs"
    res = classifier.classify(text)

    assert res.doc_type == "requirement_tender"
