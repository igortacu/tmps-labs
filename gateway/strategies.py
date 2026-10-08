"""Load-balancing algorithms (Lab 1).

Pattern: STRATEGY. Each algorithm is a separate class behind
`BalancingStrategy`.

SOLID evidence
- O (Open/Closed): add a new algorithm by adding a class + registering it in
  the factory; Gateway and existing strategies are untouched.
- S (Single Responsibility): a strategy only chooses; it does not call
  backends, count load, or handle errors.
"""
from __future__ import annotations

import random
import threading
from typing import Mapping, Sequence

from .core import Backend, BalancingStrategy


class RoundRobin(BalancingStrategy):
    def __init__(self) -> None:
        self._i = 0
        self._lock = threading.Lock()  # shared counter -> needs a lock

    def select(self, backends: Sequence[Backend], load: Mapping[str, int]) -> Backend:
        with self._lock:
            backend = backends[self._i % len(backends)]
            self._i += 1
        return backend


class LeastConnections(BalancingStrategy):
    def select(self, backends: Sequence[Backend], load: Mapping[str, int]) -> Backend:
        return min(backends, key=lambda b: load.get(b.name, 0))


class RandomChoice(BalancingStrategy):
    def __init__(self, seed: int | None = None) -> None:
        self._rng = random.Random(seed)
        self._lock = threading.Lock()  # random.Random is not documented thread-safe for sequences of calls

    def select(self, backends: Sequence[Backend], load: Mapping[str, int]) -> Backend:
        with self._lock:
            return self._rng.choice(list(backends))
