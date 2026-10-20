# 03 · Implementation plan (4–5 h)

Goal: show the concepts, not cover every case. Details are explained in the interview.

| # | Step | Time |
|---|---|---|
| 1 | Gradle multi-module skeleton, `docker-compose.yml` | 0:20 |
| 2 | Mocks (catalog, availability, customer): fake data, latency and failure simulation | 0:40 |
| 3 | Pricing: archetype core (fixed + percentage calculators, simple/composite components, validity, applicability), breakdown API | 1:15 |
| 4 | Aggregator: ports and adapters, parallel fan-out, timeouts, circuit breaker, required/optional sections | 1:15 |
| 5 | Catalog projection (CQRS light) | 0:30 |
| 6 | README with decisions, trade-offs and the design question | 0:30 |

**Cut order if time runs out:** projection (5) → second calculator type → circuit breaker tuning. Everything cut goes into the README's "with more time".

**Default taken without further discussion:** the customer discount lives in Pricing (keyed by customer id), so Pricing and Customer are called in parallel.
