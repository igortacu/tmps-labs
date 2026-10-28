"""Gateway builder and cloneable configuration prototype."""
from __future__ import annotations

from copy import deepcopy
from typing import Any, Mapping

from .gateway import Gateway


class GatewayConfigPrototype:
    """Keep a private template; each clone owns its nested configuration."""

    def __init__(self, config: Mapping[str, Any]) -> None:
        self._template = deepcopy(dict(config))

    def clone(self, **overrides: Any) -> dict[str, Any]:
        config = deepcopy(self._template)
        config.update(deepcopy(overrides))
        return config


class GatewayBuilder:
    """Assemble gateway configuration through a fluent interface."""

    def __init__(self, prototype: GatewayConfigPrototype | None = None) -> None:
        self._config = prototype.clone() if prototype else {"backends": []}

    def strategy(self, kind: str, **options: Any) -> GatewayBuilder:
        self._config["strategy"] = {"type": kind, **deepcopy(options)}
        return self

    def add_backend(self, kind: str, **options: Any) -> GatewayBuilder:
        self._config.setdefault("backends", []).append({"type": kind, **deepcopy(options)})
        return self

    def build(self) -> Gateway:
        from .factory import build_gateway

        return build_gateway(deepcopy(self._config))
