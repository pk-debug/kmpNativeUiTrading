"""Measure local read latency; this is not a DAU or production-capacity certification.

Only loopback origins are accepted. A bearer token may be supplied through
NORTHSTAR_LOAD_TOKEN for snapshot reads; neither headers nor bodies are printed.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
import os
import time
from urllib.request import Request, urlopen
from urllib.parse import urlparse


def main():
    """Run a bounded local workload and report latency, errors and throughput."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--origin', required=True)
    parser.add_argument('--requests', type=int, default=500)
    parser.add_argument('--concurrency', type=int, default=20)
    parser.add_argument('--endpoint', choices=['/health', '/api/v2/assets', '/api/v2/snapshot'], default='/api/v2/assets')
    args = parser.parse_args()
    origin = urlparse(args.origin)
    if origin.hostname not in {'localhost', '127.0.0.1'} or origin.scheme != 'http' or origin.username or origin.password or origin.path not in {'', '/'} or origin.query or origin.fragment:
        parser.error('Use a loopback HTTP origin without credentials, path, query or fragment.')
    if not 1 <= args.requests <= 10000 or not 1 <= args.concurrency <= 100:
        parser.error('Use 1–10000 requests and 1–100 workers.')
    headers = {}
    if args.endpoint == '/api/v2/snapshot':
        token = os.environ.get('NORTHSTAR_LOAD_TOKEN')
        if not token:
            parser.error('Snapshot reads require NORTHSTAR_LOAD_TOKEN.')
        headers['Authorization'] = 'Bearer ' + token

    def read(_):
        started = time.perf_counter()
        try:
            with urlopen(Request(args.origin.rstrip('/') + args.endpoint, headers=headers), timeout=15) as response:
                payload = json.load(response)
                valid = isinstance(payload, list) if args.endpoint.endswith('assets') else isinstance(payload, dict)
                return time.perf_counter() - started, response.status == 200 and valid
        except Exception:
            return time.perf_counter() - started, False

    start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        results = list(pool.map(read, range(args.requests)))
    elapsed = time.perf_counter() - start
    latency = sorted(duration * 1000 for duration, _ in results)
    report = dict(endpoint=args.endpoint, requests=args.requests, concurrency=args.concurrency,
                  errors=sum(not ok for _, ok in results), elapsed_seconds=round(elapsed, 3),
                  requests_per_second=round(len(results)/elapsed, 2),
                  p50_ms=round(latency[int((len(latency)-1)*.5)], 2),
                  p95_ms=round(latency[int((len(latency)-1)*.95)], 2),
                  p99_ms=round(latency[int((len(latency)-1)*.99)], 2))
    print(json.dumps(report, indent=2))
    raise SystemExit(1 if report['errors'] else 0)


if __name__ == '__main__':
    main()
