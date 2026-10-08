"""Backends (Lab 1).

Pattern: ADAPTER. Two fake vendor SDKs expose incompatible APIs; each adapter
translates them to the single `Backend` contract so the core never knows the
vendor. Vendors only simulate latency and failures (no network).
"""
from __future__ import annotations

import random
import time

from .core import Backend, BackendError, Request, Response


# --- "Third-party SDKs" we cannot change (adaptees) -------------------------
class VendorAClient:
    """Style A: complete(prompt) -> {"text": ...}"""

    def __init__(self, latency_s: float, failure_rate: float, rng: random.Random):
        self._latency, self._failure, self._rng = latency_s, failure_rate, rng

    def complete(self, prompt: str) -> dict:
        time.sleep(self._latency)
        if self._rng.random() < self._failure:
            raise RuntimeError("vendor A: 503")
        return {"text": f"[A] {prompt[::-1]}"}


class VendorBClient:
    """Style B: generate(messages) -> {"choices": [{"message": {"content": ...}}]}"""

    def __init__(self, latency_s: float, failure_rate: float, rng: random.Random):
        self._latency, self._failure, self._rng = latency_s, failure_rate, rng

    def generate(self, messages: list[dict]) -> dict:
        time.sleep(self._latency)
        if self._rng.random() < self._failure:
            raise ConnectionError("vendor B: connection reset")
        content = messages[-1]["content"].upper()
        return {"choices": [{"message": {"content": f"[B] {content}"}}]}


# --- Adapters ---------------------------------------------------------------
class VendorAAdapter(Backend):
    def __init__(self, name: str = "vendor-a", latency_s: float = 0.02,
                 failure_rate: float = 0.0, seed: int | None = None):
        self._name = name
        self._client = VendorAClient(latency_s, failure_rate, random.Random(seed))

    @property
    def name(self) -> str:
        return self._name

    def handle(self, request: Request) -> Response:
        start = time.perf_counter()
        try:
            raw = self._client.complete(request.prompt)
        except Exception as exc:  # translate vendor errors to our contract
            raise BackendError(f"{self._name}: {exc}") from exc
        return Response(raw["text"], self._name, (time.perf_counter() - start) * 1000)


class VendorBAdapter(Backend):
    def __init__(self, name: str = "vendor-b", latency_s: float = 0.05,
                 failure_rate: float = 0.0, seed: int | None = None):
        self._name = name
        self._client = VendorBClient(latency_s, failure_rate, random.Random(seed))

    @property
    def name(self) -> str:
        return self._name

    def handle(self, request: Request) -> Response:
        start = time.perf_counter()
        try:
            raw = self._client.generate([{"role": "user", "content": request.prompt}])
            text = raw["choices"][0]["message"]["content"]
        except Exception as exc:
            raise BackendError(f"{self._name}: {exc}") from exc
        return Response(text, self._name, (time.perf_counter() - start) * 1000)
