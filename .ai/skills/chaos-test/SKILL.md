---
name: chaos-test
description: Runs failure scenarios against the local shopper stack and shows what the client sees, step by step, while Grafana shows it live. Trigger on "chaos test", "take pricing down", "make an upstream slow", "what does the client see when…", "demo the failures", "show the circuit breaker", "is the system resilient".
---

# Chaos test for the shopper stack

**Goal:** take the upstreams through realistic failures one at a time — slow, hanging, resetting, down — and show for each step what the client of the aggregator gets and that the aggregator stays within its 500 ms budget. The Grafana dashboard shows the same steps live.

**Constraints:**
- Inject faults only through the scripts below: Toxiproxy for network faults, `docker compose stop` for an outage. Never edit the services or their configuration to cause a failure.
- Go from optional upstreams to the required one: Pricing, Availability, Customer, and Catalog last, because Catalog down turns every response into `503`.
- Assert every step with `probe.sh` against the expected outcome in `scenarios.md`, and say plainly when a step does not match.
- End with recovery: remove every scenario toxic, start every stopped service, and confirm the circuit breakers close again.

## Scripts

All in `scripts/`, run from the repository root. Each prints what it did.

| Script | Use |
|---|---|
| `stack.sh up` · `down` · `status` | start the stack with the `observability` profile and warm it up; stop it |
| `traffic.sh start [rps]` · `stop` | steady background traffic, so the dashboard moves |
| `toxic.sh <upstream> slow\|hang\|reset` | inject a scenario on `catalog`, `pricing`, `availability` or `customer` |
| `toxic.sh <upstream> clear` · `clear-all` · `list` | remove scenario toxics; the realistic ones stay |
| `outage.sh stop\|start <upstream>` | take an upstream's container down and back up |
| `probe.sh [requests]` | call the aggregator and print status, time, section states and trace id per request, then a summary |

Scenarios, their expected outcomes and the order to run them: `scenarios.md`.

## Report

After the run, give one table: step → what was injected → statuses → slowest response → section states → circuit breaker state, and whether each step matched `scenarios.md`. Point to the Grafana panels that showed it (http://localhost:3000, dashboard "Shopper · aggregator resilience").

## Gotchas

- **Toxicity is per connection.** The aggregator reuses connections, so a toxic below 1.0 hits whole connections, not single requests. Scenarios use toxicity 1; the realistic background failures fire less often than their configured rate.
- **An open circuit stays open until traffic arrives.** It moves to half-open only on a call after its 10 s wait, so keep `traffic.sh` running while you check recovery.
- **The dashboard lags.** Panels use 30 s rate windows over 5 s scrapes: a change shows after 10–30 s. Wait before reading a panel, not before probing.
- **The first requests after a cold start can degrade or get `503`** while the JVMs warm up. `stack.sh up` sends warm-up requests; do not start scenarios before it says the stack is ready.
- **Toxiproxy cannot produce HTTP 5xx.** It causes network faults only. A refused connection from a stopped container is the closest to a fast error.
- **"slow" stays under the timeout on purpose** (+200 ms): the section stays available and only the latency grows. "hang" exceeds every timeout.
- **Docker needs the docker group**: the scripts call `sg docker -c …`.
- **Catalog down is not degradation**: `503` for every request is the expected result, because a product cannot be shown without catalog data.
