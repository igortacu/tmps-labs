"""Config-driven construction with a singleton component registry.

The gateway core depends on abstractions. Vendor factories provide built-in
backend constructors; new types can be added with ``register_*``.
"""
from __future__ import annotations

import threading
from typing import Any, Callable, Mapping

from .core import Backend, BalancingStrategy
from .gateway import Gateway
from .strategies import LeastConnections, RandomChoice, RoundRobin
from .vendor_factories import VendorAFactory, VendorBFactory


class FactoryRegistry:
    """Process-wide singleton registry of component constructors."""

    _instance: FactoryRegistry | None = None
    _instance_lock = threading.Lock()

    def __new__(cls) -> FactoryRegistry:
        with cls._instance_lock:
            if cls._instance is None:
                instance = super().__new__(cls)
                instance._lock = threading.RLock()
                instance._backends = {
                    "vendor_a": VendorAFactory().create_backend,
                    "vendor_b": VendorBFactory().create_backend,
                }
                instance._strategies = {
                    "round_robin": RoundRobin,
                    "least_connections": LeastConnections,
                    "random": RandomChoice,
                }
                cls._instance = instance
            return cls._instance

    def register_backend(self, kind: str, ctor: Callable[..., Backend]) -> None:
        with self._lock:
            self._backends[kind] = ctor

    def register_strategy(self, kind: str, ctor: Callable[..., BalancingStrategy]) -> None:
        with self._lock:
            self._strategies[kind] = ctor

    def backend(self, kind: str) -> Callable[..., Backend]:
        with self._lock:
            try:
                return self._backends[kind]
            except KeyError:
                raise ValueError(f"unknown backend type: {kind!r}") from None

    def strategy(self, kind: str) -> Callable[..., BalancingStrategy]:
        with self._lock:
            try:
                return self._strategies[kind]
            except KeyError:
                raise ValueError(f"unknown strategy: {kind!r}") from None


def register_backend(kind: str, ctor: Callable[..., Backend]) -> None:
    FactoryRegistry().register_backend(kind, ctor)


def register_strategy(kind: str, ctor: Callable[..., BalancingStrategy]) -> None:
    FactoryRegistry().register_strategy(kind, ctor)


def build_gateway(config: Mapping[str, Any]) -> Gateway:
    """config = {"strategy": "round_robin", "backends": [{"type": "vendor_a", ...}]}"""
    strategy_cfg = config["strategy"]
    if isinstance(strategy_cfg, str):
        strategy_cfg = {"type": strategy_cfg}
    strategy_args = {k: v for k, v in strategy_cfg.items() if k != "type"}
    registry = FactoryRegistry()
    strategy = registry.strategy(strategy_cfg["type"])(**strategy_args)

    backends: list[Backend] = []
    for entry in config["backends"]:
        args = {k: v for k, v in entry.items() if k != "type"}
        backends.append(registry.backend(entry["type"])(**args))
    return Gateway(backends, strategy)
