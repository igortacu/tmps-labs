"""Compare balancing strategies under concurrent load against simulated backends."""
import statistics
import time
from concurrent.futures import ThreadPoolExecutor

from gateway import Request, build_gateway

BACKENDS = [  # deliberately uneven: one fast, one medium, one slow
    {"type": "vendor_a", "name": "fast", "latency_s": 0.01, "seed": 1},
    {"type": "vendor_b", "name": "medium", "latency_s": 0.03, "seed": 2},
    {"type": "vendor_a", "name": "slow", "latency_s": 0.08, "seed": 3},
]
N_REQUESTS, N_THREADS = 600, 24


def run(strategy: str) -> None:
    gw = build_gateway({"strategy": strategy, "backends": BACKENDS})

    def one(i: int) -> tuple[str, float]:
        t = time.perf_counter()
        resp = gw.handle(Request(f"prompt {i}"))
        return resp.backend, (time.perf_counter() - t) * 1000

    start = time.perf_counter()
    with ThreadPoolExecutor(N_THREADS) as pool:
        results = list(pool.map(one, range(N_REQUESTS)))
    wall = time.perf_counter() - start
    lat = sorted(r[1] for r in results)
    p99 = lat[int(len(lat) * 0.99) - 1]
    dist = {n: sum(1 for b, _ in results if b == n) for n in ("fast", "medium", "slow")}
    print(f"{strategy:<18} {N_REQUESTS / wall:7.0f} req/s  mean {statistics.mean(lat):6.1f} ms  "
          f"p99 {p99:6.1f} ms  split {dist}")


if __name__ == "__main__":
    print(f"{N_REQUESTS} requests, {N_THREADS} threads, simulated backends (not real LLM numbers)")
    for s in ("round_robin", "random", "least_connections"):
        run(s)
