---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Upstreams as separate services in Docker

## Context and Problem Statement

The aggregator combines data from four upstreams (Catalog, Pricing, Availability,
Customer) that have to be mocked. The assignment says outright that the mocks are how
they will judge our understanding of distributed systems. We have to decide where the
mocks run.

## Decision Drivers

* The problem is distributed by nature, so the simulation should reflect that.
* The assignment requires running it locally with minimal setup.

## Considered Options

* Each upstream as a separate service, run in Docker
* Mocks as components inside the aggregator process

## Decision Outcome

Chosen option: "Each upstream as a separate service, run in Docker", because only
separate processes talking over the network simulate the distributed system the
assignment is about.

```mermaid
flowchart LR
  subgraph chosen["Chosen · docker compose, one network"]
    direction LR
    AGG["product-aggregator"] -- "HTTP" --> CAT["catalog-service"]
    AGG -- "HTTP" --> PRI["pricing-service"]
    AGG -- "HTTP" --> AVA["availability-service"]
    AGG -- "HTTP" --> CUS["customer-service"]
  end
  subgraph rejected["Rejected · one JVM"]
    direction LR
    AGG2["aggregator"] -- "method call" --> MOCKS["in-process mocks"]
  end

  classDef ours fill:#1f2937,stroke:#1f2937,color:#fff
  classDef svc fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef gone fill:#f3f4f6,stroke:#9ca3af,color:#6b7280,stroke-dasharray:4 3
  class AGG ours
  class CAT,PRI,AVA,CUS svc
  class AGG2,MOCKS gone
```

### Consequences

* Good, because the aggregator deals with real network calls: timeouts, dropped
  connections and 5xx responses.
* Bad, because running the whole system requires Docker. Setup has to come down to a
  single command.
