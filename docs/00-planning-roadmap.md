# Planning roadmap

We plan **from the general to the specific**. A level is closed only once its decisions are agreed (an ADR or an entry in [01 · Big picture](#/01-big-picture.md)); only then do we move to the next one.

## Level 1 · Architecture: services and their boundaries ✅ closed

| Topic | State |
|---|---|
| Stack: Java 25 + Spring Boot 4.1, REST only | ✅ ADR 0000, 0001, 0005 |
| Aggregator + 4 upstreams as separate services in Docker | ✅ ADR 0002 |
| Pricing as a real archetype, other upstreams as mocks | ✅ ADR 0003 |
| Monorepo with a module per service | ✅ ADR 0006 |
| Gradle | ✅ ADR 0007 |
| Ports and adapters in the aggregator | ✅ ADR 0008 |

## Level 2 · Communication ✅ closed (projection to be revisited in level 4)

| Topic | State |
|---|---|
| Read path: synchronous REST, parallel fan-out | ✅ ADR 0009 |
| Catalog projection in Postgres (CQRS light) | ✅ ADR 0010, provisional |
| Asynchronous channel: change feed polled over HTTP, no broker | ✅ ADR 0010 |

## Level 3 · Domain modelling

| Topic | State |
|---|---|
| Bounded contexts and the context map (who is upstream, who conforms, where the anti-corruption layers go) | ⬜ |
| Aggregator model: product view, sections, required/optional source policy, market/locale | ⬜ |
| Pricing model: valuation archetype | 🟡 draft in [02 · Pricing](#/02-pricing.md) |
| Who owns the customer discount and segment | ⬜ open: [01 §8](#/01-big-picture.md) |
| Upstream contracts (what each mock returns) | ⬜ |

## Level 4 · Details (later)

Not to be discussed before levels 1–3 are closed:
- resilience parameters (timeouts, budget, CB, retry), caching;
- API contract and error model;
- observability, tests, docker compose;
- README and the design question.
