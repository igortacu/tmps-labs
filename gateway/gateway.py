"""Gateway (Lab 1).

Pattern: FACADE. Clients call `handle()` and never see strategies, load
tracking or backend selection.

SOLID evidence
- D: constructor receives abstractions (Sequence[Backend], BalancingStrategy);
  there is no import of any concrete backend or strategy in this file.
- S: orchestration only. Choosing = strategy, vendor calls = adapters,
  construction = factory.
"""
from __future__ import annotations

import threading
from typing import Sequence

from .core import Backend, BackendError, BalancingStrategy, NoBackendAvailable, Request, Response


class Gateway:
    def __init__(self, backends: Sequence[Backend], strategy: BalancingStrategy) -> None:
        if not backends:
            raise ValueError("Gateway needs at least one backend")
        self._backends = list(backends)
        self._strategy = strategy
        self._load: dict[str, int] = {b.name: 0 for b in self._backends}
        self._lock = threading.Lock()  # protects _load

    def handle(self, request: Request) -> Response:
        # Lab 1 policy: try each backend at most once, in strategy order.
        tried: set[str] = set()
        last_error: Exception | None = None
        while len(tried) < len(self._backends):
            candidates = [b for b in self._backends if b.name not in tried]
            backend = self._strategy.select(candidates, self._snapshot())
            tried.add(backend.name)
            self._inc(backend.name)
            try:
                return backend.handle(request)
            except BackendError as exc:
                last_error = exc
            finally:
                self._dec(backend.name)
        raise NoBackendAvailable(f"all backends failed; last error: {last_error}")

    def _snapshot(self) -> dict[str, int]:
        with self._lock:
            return dict(self._load)

    def _inc(self, name: str) -> None:
        with self._lock:
            self._load[name] += 1

    def _dec(self, name: str) -> None:
        with self._lock:
            self._load[name] -= 1
