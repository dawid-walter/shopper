# 03 · Implementation plan (4–5 h)

Goal: show the concepts, not cover every case. Details are explained in the interview.

| # | Step | Time |
|---|---|---|
| 1 | Gradle multi-module skeleton, `docker-compose.yml` | 0:20 |
| 2 | Mocks (catalog, availability, customer): fake data from the shared test data set | 0:30 |
| 3 | Pricing: archetype core (fixed + percentage calculators, simple/composite components, validity, applicability), breakdown API; market drives currency and VAT (PL 23%, DE 19%, NL 21%) as configuration | 1:15 |
| 4 | Aggregator: ports and adapters, parallel fan-out, timeouts, circuit breaker, required/optional sections | 1:15 |
| 5 | Catalog projection (CQRS light) | 0:30 |
| 6 | README with decisions, trade-offs and the design question | 0:30 |

**Demo, if time allows:** Toxiproxy injecting latency and failures (ADR 0014) + Prometheus/Grafana dashboard showing their effect (per-upstream latency, errors, circuit breaker state, degraded sections).

**Last, after the demo:** a project skill in `.claude/skills/` for chaos testing this environment: switch Toxiproxy scenarios and stop services one by one, call the aggregator, and report what the client sees at each step (with the Grafana view alongside).

**Cut order if time runs out:** projection (5) → second calculator type → circuit breaker tuning. Everything cut goes into the README's "with more time".

**Default taken without further discussion:** the customer discount lives in Pricing (keyed by customer id), so Pricing and Customer are called in parallel.
