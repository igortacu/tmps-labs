"""Core abstractions (Lab 1).

SOLID evidence
- D (Dependency Inversion): the Gateway depends on `Backend` and
  `BalancingStrategy` abstractions defined here, never on concrete classes.
- S (Single Responsibility): this module only holds data types and contracts.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Mapping, Sequence


class BackendError(Exception):
    """The only exception a Backend is allowed to raise (contract)."""


class NoBackendAvailable(Exception):
    """Raised by the Gateway when no backend can serve a request."""


@dataclass(frozen=True)
class Request:
    prompt: str
    client_id: str = "anonymous"


@dataclass(frozen=True)
class Response:
    text: str
    backend: str
    latency_ms: float = 0.0
    meta: Mapping[str, object] = field(default_factory=dict)


class Backend(ABC):
    """Contract: handle() returns a Response or raises BackendError."""

    @property
    @abstractmethod
    def name(self) -> str: ...

    @abstractmethod
    def handle(self, request: Request) -> Response: ...


class BalancingStrategy(ABC):
    """Picks one backend. `load` maps backend name -> in-flight requests."""

    @abstractmethod
    def select(self, backends: Sequence[Backend], load: Mapping[str, int]) -> Backend: ...
