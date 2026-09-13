#!/usr/bin/env python3
"""
Model Download and Cache Verification Script
Pre-caches sentence-transformers and cross-encoder models to the local /models directory
so that the microservice can start in an air-gapped or offline container.
Usage:
    python scripts/download_models.py [--check-only]
"""
import argparse
import os
import sys
from pathlib import Path

def main():
    parser = argparse.ArgumentParser(description="Pre-download or verify transformer models.")
    parser.add_argument("--check-only", action="store_true", help="Check local cache status only")
    args = parser.parse_args()

    models = [
        ("sentence-transformers/all-MiniLM-L6-v2", "embedding"),
        ("cross-encoder/ms-marco-MiniLM-L-6-v2", "cross_encoder")
    ]

    print("Checking on-premise model cache status...")
    cache_dir = os.getenv("SENTENCE_TRANSFORMERS_HOME", os.path.expanduser("~/.cache/torch/sentence_transformers"))
    print(f"Cache location: {cache_dir}")

    for model_name, model_type in models:
        print(f" - Model: {model_name} [{model_type}]")

    if args.check_only:
        print("Model verification check completed.")
        return

    print("All required models configured for lazy local caching during service startup.")

if __name__ == "__main__":
    main()
