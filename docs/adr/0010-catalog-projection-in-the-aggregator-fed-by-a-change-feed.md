---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Catalog projection in the aggregator fed by a change feed

## Context and Problem Statement

The aggregator should keep projections in Postgres. We have to decide what it projects
and how the projection is fed, given that the read path stays synchronous (ADR 0009).

## Decision Drivers

* Catalog data (name, description, specs, images) changes rarely; price and stock
  change constantly.
* Catalog is the only required upstream: its failure fails the request.
* The first version has limited time.

## Considered Options

* Full CQRS: every source projected through a message broker
* Last-known-good snapshot: successful responses stored and served only when an
  upstream fails
* No Postgres, in-memory cache only
* CQRS light: only the catalog projected, fed by a change feed the aggregator polls

## Decision Outcome

Chosen option: "CQRS light", because it applies projection only where data changes
slowly, and it takes the required upstream off the hot path. The catalog exposes an
ordered change feed; the aggregator polls it and stores the last processed offset with
the projection, which makes processing idempotent and replayable without a broker. On a
projection miss the aggregator reads the catalog live.

```mermaid
flowchart LR
  CAT["catalog-service"] -- "change feed<br/>events after offset" --> PROJ["projector<br/>(polls)"]
  PROJ -- "upsert view + offset<br/>one transaction" --> PG[("Postgres<br/>catalog projection")]
  READ(["aggregator<br/>read path"]) -- "1 · read" --> PG
  READ -. "2 · on miss: live read" .-> CAT
  READ -- "live, in parallel" --> LIVE["pricing · availability · customer"]

  classDef up fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef ours fill:#1f2937,stroke:#1f2937,color:#fff
  classDef db fill:#fef3c7,stroke:#d97706,color:#78350f
  class CAT,LIVE up
  class PROJ,READ ours
  class PG db
```

### Consequences

* Good, because a catalog outage no longer fails requests for products already
  projected.
* Bad, because it reinterprets the constraint "if Catalog fails, the request fails": it
  still holds for products not yet projected, and has to be agreed with the product
  owner.
* Bad, because it adds Postgres, migrations and a projector to the aggregator.

## More Information

Provisional: to be revisited while planning the details. If time gets tight, the
fallback is the last-known-good snapshot.
