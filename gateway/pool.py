"""A bounded, thread-safe pool for reusable vendor clients."""
from __future__ import annotations

from contextlib import contextmanager
from threading import Condition
from typing import Callable, Generic, Iterator, TypeVar

T = TypeVar("T")


class ObjectPool(Generic[T]):
    """Create at most ``max_size`` objects and return each after its lease."""

    def __init__(self, factory: Callable[[], T], max_size: int) -> None:
        if max_size < 1:
            raise ValueError("max_size must be at least 1")
        self._factory = factory
        self._max_size = max_size
        self._created = 0
        self._available: list[T] = []
        self._condition = Condition()

    @contextmanager
    def acquire(self) -> Iterator[T]:
        with self._condition:
            while not self._available and self._created >= self._max_size:
                self._condition.wait()
            if self._available:
                item = self._available.pop()
            else:
                # Reserve the slot before construction so concurrent callers
                # cannot exceed max_size, even if the factory raises.
                self._created += 1
                try:
                    item = self._factory()
                except BaseException:
                    self._created -= 1
                    self._condition.notify()
                    raise
        try:
            yield item
        finally:
            with self._condition:
                self._available.append(item)
                self._condition.notify()
