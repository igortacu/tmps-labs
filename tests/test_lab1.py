import threading
import unittest
from collections import Counter
from concurrent.futures import ThreadPoolExecutor

from gateway import (Backend, BackendError, BalancingStrategy, Gateway,
                     NoBackendAvailable, Request, Response, build_gateway,
                     register_strategy)
from gateway.backends import VendorAAdapter, VendorBAdapter
from gateway.strategies import LeastConnections, RoundRobin


class FakeBackend(Backend):
    """Test double: deterministic, no sleeping."""

    def __init__(self, name, fail=False):
        self._name, self._fail, self.calls = name, fail, 0
        self._lock = threading.Lock()

    @property
    def name(self):
        return self._name

    def handle(self, request):
        with self._lock:
            self.calls += 1
        if self._fail:
            raise BackendError(f"{self._name} down")
        return Response("ok", self._name)


class AdapterContractTests(unittest.TestCase):
    """LSP-style check: every backend honours the same contract."""

    def test_all_adapters_return_response_with_own_name(self):
        for backend in (VendorAAdapter(latency_s=0), VendorBAdapter(latency_s=0)):
            with self.subTest(backend=backend.name):
                resp = backend.handle(Request("hi"))
                self.assertIsInstance(resp, Response)
                self.assertEqual(resp.backend, backend.name)
                self.assertTrue(resp.text)

    def test_vendor_failures_become_backend_error(self):
        for backend in (VendorAAdapter(latency_s=0, failure_rate=1.0, seed=1),
                        VendorBAdapter(latency_s=0, failure_rate=1.0, seed=1)):
            with self.subTest(backend=backend.name):
                with self.assertRaises(BackendError):
                    backend.handle(Request("hi"))


class StrategyTests(unittest.TestCase):
    def test_round_robin_cycles(self):
        a, b, c = FakeBackend("a"), FakeBackend("b"), FakeBackend("c")
        rr = RoundRobin()
        picks = [rr.select([a, b, c], {}).name for _ in range(6)]
        self.assertEqual(picks, ["a", "b", "c", "a", "b", "c"])

    def test_least_connections_picks_lowest_load(self):
        a, b = FakeBackend("a"), FakeBackend("b")
        self.assertEqual(LeastConnections().select([a, b], {"a": 5, "b": 1}).name, "b")


class GatewayTests(unittest.TestCase):
    def test_routes_via_strategy(self):
        a, b = FakeBackend("a"), FakeBackend("b")
        gw = Gateway([a, b], RoundRobin())
        for _ in range(10):
            gw.handle(Request("x"))
        self.assertEqual((a.calls, b.calls), (5, 5))

    def test_failover_to_next_backend(self):
        bad, good = FakeBackend("bad", fail=True), FakeBackend("good")
        gw = Gateway([bad, good], RoundRobin())
        self.assertEqual(gw.handle(Request("x")).backend, "good")

    def test_all_failing_raises(self):
        gw = Gateway([FakeBackend("a", fail=True), FakeBackend("b", fail=True)], RoundRobin())
        with self.assertRaises(NoBackendAvailable):
            gw.handle(Request("x"))

    def test_requires_backends(self):
        with self.assertRaises(ValueError):
            Gateway([], RoundRobin())

    def test_load_returns_to_zero_after_concurrent_requests(self):
        backends = [FakeBackend("a"), FakeBackend("b", fail=True), FakeBackend("c")]
        gw = Gateway(backends, RoundRobin())
        with ThreadPoolExecutor(16) as pool:
            list(pool.map(lambda _: gw.handle(Request("x")), range(400)))
        self.assertTrue(all(v == 0 for v in gw._snapshot().values()))

    def test_round_robin_is_balanced_under_threads(self):
        backends = [FakeBackend(n) for n in "abcd"]
        gw = Gateway(backends, RoundRobin())
        with ThreadPoolExecutor(16) as pool:
            results = list(pool.map(lambda _: gw.handle(Request("x")).backend, range(400)))
        counts = Counter(results)
        self.assertEqual(sum(counts.values()), 400)
        self.assertTrue(all(c == 100 for c in counts.values()), counts)


class FactoryAndOCPTests(unittest.TestCase):
    CONFIG = {
        "strategy": "round_robin",
        "backends": [{"type": "vendor_a", "latency_s": 0}, {"type": "vendor_b", "latency_s": 0}],
    }

    def test_build_from_config(self):
        gw = build_gateway(self.CONFIG)
        self.assertEqual({gw.handle(Request("x")).backend for _ in range(4)}, {"vendor-a", "vendor-b"})

    def test_unknown_types_rejected(self):
        with self.assertRaises(ValueError):
            build_gateway({**self.CONFIG, "strategy": "nope"})
        with self.assertRaises(ValueError):
            build_gateway({**self.CONFIG, "backends": [{"type": "nope"}]})

    def test_new_strategy_needs_no_core_change(self):
        class AlwaysFirst(BalancingStrategy):
            def select(self, backends, load):
                return backends[0]

        register_strategy("always_first", AlwaysFirst)
        gw = build_gateway({**self.CONFIG, "strategy": "always_first"})
        self.assertEqual({gw.handle(Request("x")).backend for _ in range(5)}, {"vendor-a"})


if __name__ == "__main__":
    unittest.main()
