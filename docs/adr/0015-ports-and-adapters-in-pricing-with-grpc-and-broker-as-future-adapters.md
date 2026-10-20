---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Ports and adapters in Pricing, with gRPC and broker as future adapters

## Context and Problem Statement

ADR 0008 applies ports and adapters to the aggregator. Pricing, the only upstream with a
real domain model (ADR 0003), needs an internal structure too. Communication is
synchronous REST today (ADR 0009), but a message broker may join later.

## Decision Drivers

* The same structure in both services, so gRPC and a broker are explained once.
* Adding gRPC or a broker later should be an adapter, not a change to the domain.
* Only what is used gets built.

## Considered Options

* Ports and adapters, with gRPC and the broker shown as future adapters but not built
* Ports and adapters, with gRPC and broker adapters built now
* Layered: controller → service → repository

## Decision Outcome

Chosen option: "Ports and adapters, with gRPC and the broker shown as future adapters
but not built", because it keeps today's scope to REST while leaving a clear place for
each future transport: gRPC as an inbound adapter next to the REST controller, a broker
as an outbound publisher in Pricing and an inbound listener in the aggregator. Adapters
talk to adapters; the domain stays unaware of the transport.

Packages follow the same layers as the aggregator (ADR 0008). The repository of the
`PriceComponent` aggregate is declared in `domain`, because it stores the domain's own
aggregate; outbound ports are reserved for other systems.

```mermaid
flowchart LR
  REST["REST controller"] --> UC[/"CalculatePrice"/]
  GRPC["gRPC service"] -.-> UC
  UC --> SVC(["pricing application<br/>+ valuation domain"])
  SVC --> REPO[/"ComponentRepository"/] --- MEM["in-memory adapter"]
  SVC -.-> EV[/"PriceEvents"/] -.- BRK["broker publisher"]

  classDef core fill:#1f2937,stroke:#1f2937,color:#fff
  classDef port fill:#e4002b,stroke:#9f0020,color:#fff
  classDef adapter fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef future fill:#f3f4f6,stroke:#9ca3af,color:#6b7280,stroke-dasharray:4 3
  class SVC core
  class UC,REPO port
  class REST,MEM adapter
  class GRPC,EV,BRK future
```

### Consequences

* Good, because adding gRPC or a broker later means adding an adapter in a known place
  rather than refactoring.
* Neutral, because the future adapters exist only in diagrams and the README; no empty
  packages or unused interfaces are created.

## More Information

Domain events are not built yet, because nothing consumes them. The intended path once a
broker exists: a private domain event inside Pricing (e.g. a component version added) is
translated at the service boundary into a public integration event, published through
the broker adapter, and consumed by a listener adapter in the aggregator, which can
evict a cached price or maintain a projection. The same mechanism would feed a catalog
projection (rejected for now in ADR 0010). A precomputed read model of current prices is
not on this path: prices depend on validity periods, and the passing of a period boundary
produces no event, so such a projection would go stale without a scheduler.
