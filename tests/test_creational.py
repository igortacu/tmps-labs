import threading
import unittest
from concurrent.futures import ThreadPoolExecutor

from gateway import (FactoryRegistry, GatewayBuilder, GatewayConfigPrototype,
                     ObjectPool, Request, VendorAFactory, VendorBFactory,
                     build_gateway)
from gateway.backends import VendorAAdapter, VendorAClient, VendorBAdapter, VendorBClient


class CreationalPatternTests(unittest.TestCase):
    def test_singleton_registry_shared_across_threads(self):
        with ThreadPoolExecutor(16) as executor:
            registries = list(executor.map(lambda _: FactoryRegistry(), range(100)))
        self.assertTrue(all(registry is registries[0] for registry in registries))
        self.assertIs(FactoryRegistry(), registries[0])

    def test_builder_creates_configured_gateway(self):
        gateway = (GatewayBuilder()
                   .strategy("round_robin")
                   .add_backend("vendor_a", name="a", latency_s=0)
                   .add_backend("vendor_b", name="b", latency_s=0)
                   .build())
        self.assertEqual([gateway.handle(Request("x")).backend for _ in range(2)], ["a", "b"])

    def test_prototype_clones_nested_config_without_sharing(self):
        original = {"strategy": "round_robin",
                    "backends": [{"type": "vendor_a", "name": "a", "latency_s": 0}]}
        prototype = GatewayConfigPrototype(original)
        original["backends"][0]["name"] = "changed"
        clone = prototype.clone()
        clone["backends"][0]["name"] = "different"
        self.assertEqual(prototype.clone()["backends"][0]["name"], "a")
        gateway = GatewayBuilder(prototype).add_backend("vendor_b", name="b", latency_s=0).build()
        self.assertEqual({gateway.handle(Request("x")).backend for _ in range(2)}, {"a", "b"})

    def test_pool_reuses_objects_and_caps_concurrency(self):
        created = []
        pool = ObjectPool(lambda: created.append(object()) or created[-1], max_size=2)
        with ThreadPoolExecutor(1) as executor:
            with pool.acquire() as first:
                with pool.acquire() as second:
                    self.assertIsNot(first, second)
                    ready = threading.Event()
                    acquired = threading.Event()

                    def wait_for_slot():
                        ready.set()
                        with pool.acquire():
                            acquired.set()

                    future = executor.submit(wait_for_slot)
                    self.assertTrue(ready.wait(1))
                    self.assertFalse(acquired.wait(0.05))
                self.assertTrue(acquired.wait(1))
                future.result()
        self.assertEqual(len(created), 2)
        with pool.acquire() as reused:
            self.assertIn(reused, created)

    def test_pool_releases_on_error(self):
        pool = ObjectPool(object, max_size=1)
        with self.assertRaisesRegex(RuntimeError, "boom"):
            with pool.acquire():
                raise RuntimeError("boom")
        with pool.acquire() as item:
            self.assertIsNotNone(item)
        with self.assertRaises(ValueError):
            ObjectPool(object, max_size=0)

    def test_abstract_factories_pair_clients_and_adapters(self):
        for factory, client_type, adapter_type in (
            (VendorAFactory(), VendorAClient, VendorAAdapter),
            (VendorBFactory(), VendorBClient, VendorBAdapter),
        ):
            with self.subTest(factory=type(factory).__name__):
                self.assertIsInstance(factory.create_client(0, 0, 1), client_type)
                backend = factory.create_backend(latency_s=0)
                self.assertIsInstance(backend, adapter_type)
                self.assertTrue(backend.handle(Request("hello")).text)

    def test_configured_vendor_pool_handles_concurrent_requests(self):
        gateway = build_gateway({
            "strategy": "round_robin",
            "backends": [{"type": "vendor_a", "latency_s": 0, "pool_size": 2}],
        })
        with ThreadPoolExecutor(8) as executor:
            results = list(executor.map(lambda i: gateway.handle(Request(str(i))), range(40)))
        self.assertEqual(len(results), 40)
        self.assertTrue(all(result.backend == "vendor-a" for result in results))


if __name__ == "__main__":
    unittest.main()
