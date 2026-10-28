"""Abstract factory for matching vendor clients and backend adapters."""
from __future__ import annotations

import random
from abc import ABC, abstractmethod
from typing import Any

from .backends import VendorAAdapter, VendorAClient, VendorBAdapter, VendorBClient
from .core import Backend
from .pool import ObjectPool


class BackendFamilyFactory(ABC):
    @abstractmethod
    def create_client(self, latency_s: float, failure_rate: float,
                      seed: int | None) -> Any: ...

    @abstractmethod
    def create_backend(self, name: str | None = None, latency_s: float = 0.02,
                       failure_rate: float = 0.0, seed: int | None = None,
                       pool_size: int | None = None) -> Backend: ...


class VendorAFactory(BackendFamilyFactory):
    def create_client(self, latency_s: float, failure_rate: float,
                      seed: int | None) -> VendorAClient:
        return VendorAClient(latency_s, failure_rate, random.Random(seed))

    def create_backend(self, name: str | None = None, latency_s: float = 0.02,
                       failure_rate: float = 0.0, seed: int | None = None,
                       pool_size: int | None = None) -> Backend:
        if pool_size is None:
            return VendorAAdapter(name=name or "vendor-a",
                                  client=self.create_client(latency_s, failure_rate, seed))
        pool = ObjectPool(lambda: self.create_client(latency_s, failure_rate, seed), pool_size)
        return VendorAAdapter(name=name or "vendor-a", client_pool=pool)


class VendorBFactory(BackendFamilyFactory):
    def create_client(self, latency_s: float, failure_rate: float,
                      seed: int | None) -> VendorBClient:
        return VendorBClient(latency_s, failure_rate, random.Random(seed))

    def create_backend(self, name: str | None = None, latency_s: float = 0.05,
                       failure_rate: float = 0.0, seed: int | None = None,
                       pool_size: int | None = None) -> Backend:
        if pool_size is None:
            return VendorBAdapter(name=name or "vendor-b",
                                  client=self.create_client(latency_s, failure_rate, seed))
        pool = ObjectPool(lambda: self.create_client(latency_s, failure_rate, seed), pool_size)
        return VendorBAdapter(name=name or "vendor-b", client_pool=pool)
