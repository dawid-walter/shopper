# 07 · Resilience parameters from the numbers

> Status: **proposal**. Timeouts, retries, time budget and circuit breaker are derived from the upstreams' typical latency and reliability instead of being guessed. Decision: [ADR 0016](adr/0016-resilience-parameters-derived-from-upstream-latency-and-reliability.md).

**Percentiles:** p50 is the typical (median) latency, half of the responses are faster; p99 means 1 in 100 responses is slower; p99.9 means 1 in 1000 is slower. They show the slow tail that an average hides.

## Inputs and assumptions

| Upstream | Typical latency | Reliability |
|---|---|---|
| Catalog | 50 ms | 99.9% |
| Customer | 60 ms | 99% |
| Pricing | 80 ms | 99.5% |
| Availability | 100 ms | 98% |

Assumptions (stated, because the numbers depend on them):
1. **"Typical latency" is the median**, and latency is log-normal with σ = 0.5: a long tail, p99 ≈ 3.2 × median, p99.9 ≈ 4.7 × median.
2. **"Reliability" means fast errors** (5xx, refused connection); slowness is the latency tail, not part of the error rate.
3. **Failures are independent** between attempts. Real outages are correlated, which is what the circuit breaker is for.

## 1. Timeout per upstream = its p99.9

| Upstream | p99 | **p99.9 → timeout** |
|---|---|---|
| Catalog | 160 ms | **~235 ms** |
| Customer | 192 ms | **~280 ms** |
| Pricing | 256 ms | **~375 ms** |
| Availability | 320 ms | **~470 ms** |

Why p99.9 and not p99: a timeout at p99 alone throws away 1% of otherwise good answers; at p99.9 only 0.1%. The cost is a longer worst case (still under half a second).

## 2. The request budget follows from the slowest timeout

Calls run in parallel, so the worst case is the slowest upstream's timeout: **budget = 500 ms** (Availability's ~470 ms, rounded up). Anything not back by then is degraded.

Why per-section degradation matters, in numbers: the chance that **at least one** of the four calls lands beyond its p99 is 1 − 0.99⁴ ≈ **3.9%** of requests. Without independent timeouts, one in ~25 requests would wait for the slowest tail.

## 3. Retry: once, fast errors only

With independent failures, one retry turns failure probability `p` into `p²`:

| Upstream | Without retry | **With 1 retry** |
|---|---|---|
| Catalog | 99.80% | **99.90%** |
| Customer | 98.90% | **99.89%** |
| Pricing | 99.40% | **99.90%** |
| Availability | 97.90% | **99.86%** |

(Both columns include the 0.1% lost to the p99.9 timeout; a timeout is not retried.)

Rules that follow:
- **Retry fast errors, never timeouts.** A timed-out call has already used its time; retrying it would double the worst case past the budget.
- **One retry is enough.** A second one would move Availability from 99.86% to ~99.9% (the timeout loss dominates), for more load on a struggling service.
- **No delay, and no budget check.** Implemented with Spring's `@Retryable(maxRetries = 1, delay = 0)` on the adapters. A retry that cannot finish in time is cut by the request budget like any call: it wastes one upstream request but never delays the response. A retry predicate could skip it when less than the upstream's p99 remains.

## 4. The whole response

| | All four sections complete |
|---|---|
| No timeouts, no retries | 0.999 × 0.995 × 0.98 × 0.99 ≈ **96.4%** (1 in 28 responses degraded) |
| Timeouts at p99.9 + 1 retry | ≈ **99.5%** (1 in 200 degraded) |

The response itself (`200`) depends only on Catalog: ≈ **99.9%**.

## 5. Circuit breaker: never trips on normal noise

- Window: last **20 calls**, open at **≥ 50% failures**, stay open **10 s**, then half-open with a few trial calls.
- Normal failure rate of the worst upstream ≈ 2.1% (Availability: 2% errors + 0.1% timeouts); take 3% to be conservative. Probability that 10 of 20 fail by chance: **≈ 8 × 10⁻¹¹**, so a false trip practically never happens.
- A real outage (≥ 50% failing) opens it within ~20 calls, and from then on the section degrades **immediately** instead of after a timeout.

## 6. Options: latency against completeness

Monte Carlo over the same model (200 000 simulated requests, all four sections requested). "Complete" = every section has data; the `200` itself depends only on Catalog (~99.9%) in every option.

| Option | Complete response | Median | p99 | Worst case |
|---|---|---|---|---|
| 0 · No timeouts, no retries | 96.4% | 125 ms | 335 ms | ~1 s, unbounded |
| 1 · **Fast**: everything capped at 200 ms, retry | 85.5% | 125 ms | 200 ms | **200 ms** |
| 2 · **Balanced**: timeouts at p99, retry | 96.0% | 126 ms | 320 ms | 325 ms |
| 3 · **Complete** (ADR 0016): timeouts at p99.9, retry, 500 ms budget | **99.6%** | 126 ms | 335 ms | 474 ms |
| 4 · Complete + hedged requests at p95 | 99.9% | 128 ms | 347 ms | 500 ms |

What it shows:
- **The median does not move** (~125 ms): the options only decide what happens in the tail.
- **Speed is bought with optional sections.** Option 1 guarantees 200 ms, but one response in seven arrives without price, stock or personalisation.
- **Option 3 bounds the wait and completes 99.6%** of responses; that is the chosen default.
- **Hedging** (a second request when the first is slower than p95) adds ~5% load and lifts completeness to 99.9%, but does not shorten the tail with this latency model. Worth it only if measurements show a heavier tail.

The guarantee is therefore **bounded latency plus a defined degraded response**, not 100% complete data.

## What changes if the assumptions are wrong

The formulas stay; the inputs change. With real latency histograms (Micrometer, Grafana) the timeouts are recomputed from measured p99.9 instead of the assumed σ.
