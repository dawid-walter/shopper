---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Resilience parameters derived from upstream latency and reliability

## Context and Problem Statement

The aggregator calls four upstreams synchronously and in parallel (ADR 0009). Each has a
known typical latency and reliability: Catalog 50 ms / 99.9%, Customer 60 ms / 99%,
Pricing 80 ms / 99.5%, Availability 100 ms / 98%. Timeouts, retries, the request budget
and the circuit breaker need values.

## Decision Drivers

* Parameters should follow from the known latency and reliability, not from guesses.
* The response must stay fast even when an upstream is slow.
* Retries must not push a request past its budget or overload a failing upstream.

## Considered Options

* Parameters derived from a latency and failure model of each upstream
* Round-number defaults (e.g. 1 s timeouts, 3 retries) tuned later

## Decision Outcome

Chosen option: "Parameters derived from a latency and failure model", with these
assumptions: typical latency is the median of a log-normal distribution with σ = 0.5;
reliability counts fast errors; failures are independent between attempts.

| Parameter | Value | Derivation |
|---|---|---|
| Timeout per upstream | Catalog 235 ms, Customer 280 ms, Pricing 375 ms, Availability 470 ms | p99.9 of each latency distribution, the time within which 99.9% of normal responses arrive: cuts 0.1% of good answers, against 1% at p99 (1 in 100 slower) |
| Request budget | 500 ms | calls run in parallel, so the slowest timeout bounds the request |
| Retry | once, fast errors only, only if the remaining budget covers the upstream's p99 | one retry turns failure `p` into `p²` (Availability 97.9% → 99.86%); a timeout has used its time and is not retried |
| Circuit breaker | open at ≥ 50% failures over the last 20 calls, 10 s open, then half-open | a false trip at a 3% failure rate has probability ≈ 8 × 10⁻¹¹; a real outage opens it within about 20 calls |

### Consequences

* Good, because a fully complete response rises from about 96.4% (no timeouts, no
  retries) to about 99.5%, and every value can be traced to its input.
* Good, because a slow upstream costs at most its own timeout, never the request.
* Bad, because the values are only as good as the assumptions; real latency histograms
  should replace the assumed distribution once the service runs.

## More Information

The values should be recomputed from measured p99.9 latency and error rates when the
upstreams change or when metrics are available.
