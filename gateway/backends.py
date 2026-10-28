"""Backends (Lab 1).

Pattern: ADAPTER. Two fake vendor SDKs expose incompatible APIs; each adapter
translates them to the single `Backend` contract so the core never knows the
vendor. Vendors only simulate latency and failures (no network).
"""
from __future__ import annotations

import random
import time
from contextlib import nullcontext

from .core import Backend, BackendError, Request, Response
from .pool import ObjectPool


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
                 failure_rate: float = 0.0, seed: int | None = None,
                 client: VendorAClient | None = None,
                 client_pool: ObjectPool[VendorAClient] | None = None):
        if client is not None and client_pool is not None:
            raise ValueError("provide a client or client_pool, not both")
        self._name = name
        self._client = client or (None if client_pool else VendorAClient(latency_s, failure_rate, random.Random(seed)))
        self._client_pool = client_pool

    @property
    def name(self) -> str:
        return self._name

    def handle(self, request: Request) -> Response:
        start = time.perf_counter()
        try:
            with self._client_pool.acquire() if self._client_pool else nullcontext(self._client) as client:
                raw = client.complete(request.prompt)
        except Exception as exc:  # translate vendor errors to our contract
            raise BackendError(f"{self._name}: {exc}") from exc
        return Response(raw["text"], self._name, (time.perf_counter() - start) * 1000)


class VendorBAdapter(Backend):
    def __init__(self, name: str = "vendor-b", latency_s: float = 0.05,
                 failure_rate: float = 0.0, seed: int | None = None,
                 client: VendorBClient | None = None,
                 client_pool: ObjectPool[VendorBClient] | None = None):
        if client is not None and client_pool is not None:
            raise ValueError("provide a client or client_pool, not both")
        self._name = name
        self._client = client or (None if client_pool else VendorBClient(latency_s, failure_rate, random.Random(seed)))
        self._client_pool = client_pool

    @property
    def name(self) -> str:
        return self._name

    def handle(self, request: Request) -> Response:
        start = time.perf_counter()
        try:
            with self._client_pool.acquire() if self._client_pool else nullcontext(self._client) as client:
                raw = client.generate([{"role": "user", "content": request.prompt}])
                text = raw["choices"][0]["message"]["content"]
        except Exception as exc:
            raise BackendError(f"{self._name}: {exc}") from exc
        return Response(text, self._name, (time.perf_counter() - start) * 1000)
