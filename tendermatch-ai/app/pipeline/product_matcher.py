from typing import List, Optional, Dict, Any
from pydantic import BaseModel
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from app.pipeline.spec_extractor import ExtractedProduct


class MatchedProduct(BaseModel):
    matched_product_name: str
    confidence: float
    matched_product_id: Optional[str] = None


class MatchedSpec(BaseModel):
    matched_spec_name: str
    confidence: float
    matched_spec_id: Optional[str] = None


class ProductMatcher:
    def __init__(self):
        self.vectorizer = TfidfVectorizer(analyzer="char_wb", ngram_range=(3, 5))

    def _similarity(self, text1: str, text2: str) -> float:
        import re
        t1, t2 = text1.strip().lower(), text2.strip().lower()
        if t1 == t2:
            return 1.0

        words1 = set(re.findall(r'[a-zA-Z0-9]+', t1))
        words2 = set(re.findall(r'[a-zA-Z0-9]+', t2))
        if not words1 or not words2:
            return 0.0

        inter_words = words1.intersection(words2)
        word_sim = 2.0 * len(inter_words) / (len(words1) + len(words2))

        # Char 3-grams
        n = 3
        ngrams1 = {t1[i:i + n] for i in range(len(t1) - n + 1)} if len(t1) >= n else {t1}
        ngrams2 = {t2[i:i + n] for i in range(len(t2) - n + 1)} if len(t2) >= n else {t2}
        inter_char = ngrams1.intersection(ngrams2)
        char_sim = 2.0 * len(inter_char) / (len(ngrams1) + len(ngrams2)) if (ngrams1 and ngrams2) else 0.0

        return 0.5 * word_sim + 0.5 * char_sim

    def match_product(self,
                      vendor_product: ExtractedProduct,
                      req_products: List[ExtractedProduct],
                      threshold: float = 0.30) -> Optional[MatchedProduct]:
        if not req_products:
            return None

        v_name = vendor_product.product_name.strip()
        best_product = None
        best_score = -1.0

        for p in req_products:
            score = self._similarity(v_name, p.product_name)
            if vendor_product.product_category and p.product_category and vendor_product.product_category == p.product_category:
                score = min(1.0, score + 0.20)
            if score > best_score:
                best_score = score
                best_product = p

        # If requirement tender has exactly 1 product, match if category matches or score meets threshold
        if len(req_products) == 1:
            if vendor_product.product_category == req_products[0].product_category or best_score >= threshold:
                return MatchedProduct(
                    matched_product_name=req_products[0].product_name,
                    confidence=max(best_score, 0.85) if best_score >= threshold else 0.85
                )
            return None

        if best_product and best_score >= threshold:
            return MatchedProduct(
                matched_product_name=best_product.product_name,
                confidence=round(best_score, 2)
            )

        return None

    def match_spec(self,
                   vendor_spec: Dict[str, Any],
                   req_specs: List[Dict[str, Any]],
                   threshold: float = 0.30) -> Optional[MatchedSpec]:
        if not req_specs:
            return None

        v_canonical = vendor_spec.get("canonical_spec_name")
        v_name = vendor_spec.get("spec_name", "").strip()

        # 1. Match by Canonical Spec Key (Highest priority)
        if v_canonical and v_canonical != "unknown":
            for r in req_specs:
                if r.get("canonical_spec_name") == v_canonical:
                    return MatchedSpec(
                        matched_spec_name=r.get("spec_name", ""),
                        confidence=1.0,
                        matched_spec_id=r.get("id")
                    )

        # 2. Match by Name Similarity
        best_spec = None
        best_score = -1.0

        for r in req_specs:
            r_name = r.get("spec_name", "").strip()
            r_canonical = r.get("canonical_spec_name", "")
            name_score = self._similarity(v_name, r_name)
            can_score = self._similarity(v_canonical, r_canonical) if (v_canonical and r_canonical) else 0.0
            score = max(name_score, can_score)
            if score > best_score:
                best_score = score
                best_spec = r

        if best_spec and best_score >= threshold:
            return MatchedSpec(
                matched_spec_name=best_spec.get("spec_name", ""),
                confidence=round(best_score, 2),
                matched_spec_id=best_spec.get("id")
            )

        return None
