"""Config-driven construction (Lab 1).

Pattern: FACTORY (with registries). This is the ONLY module that knows
concrete classes; everything else depends on abstractions (DIP). New types are
added with `register_*` and no existing code changes (OCP).
"""
from __future__ import annotations

from typing import Any, Callable, Mapping

from .backends import VendorAAdapter, VendorBAdapter
from .core import Backend, BalancingStrategy
from .gateway import Gateway
from .strategies import LeastConnections, RandomChoice, RoundRobin

_BACKENDS: dict[str, Callable[..., Backend]] = {
    "vendor_a": VendorAAdapter,
    "vendor_b": VendorBAdapter,
}
_STRATEGIES: dict[str, Callable[..., BalancingStrategy]] = {
    "round_robin": RoundRobin,
    "least_connections": LeastConnections,
    "random": RandomChoice,
}


def register_backend(kind: str, ctor: Callable[..., Backend]) -> None:
    _BACKENDS[kind] = ctor


def register_strategy(kind: str, ctor: Callable[..., BalancingStrategy]) -> None:
    _STRATEGIES[kind] = ctor


def build_gateway(config: Mapping[str, Any]) -> Gateway:
    """config = {"strategy": "round_robin", "backends": [{"type": "vendor_a", ...}]}"""
    strategy_cfg = config["strategy"]
    if isinstance(strategy_cfg, str):
        strategy_cfg = {"type": strategy_cfg}
    strategy_args = {k: v for k, v in strategy_cfg.items() if k != "type"}
    try:
        strategy = _STRATEGIES[strategy_cfg["type"]](**strategy_args)
    except KeyError:
        raise ValueError(f"unknown strategy: {strategy_cfg['type']!r}") from None

    backends: list[Backend] = []
    for entry in config["backends"]:
        args = {k: v for k, v in entry.items() if k != "type"}
        try:
            backends.append(_BACKENDS[entry["type"]](**args))
        except KeyError:
            raise ValueError(f"unknown backend type: {entry['type']!r}") from None
    return Gateway(backends, strategy)
