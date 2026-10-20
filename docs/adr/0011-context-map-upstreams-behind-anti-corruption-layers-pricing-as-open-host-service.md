---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Context map: upstreams behind anti-corruption layers, Pricing as open host service

## Context and Problem Statement

Five bounded contexts exist: the aggregator, Catalog, Pricing, Availability and Customer.
We have to decide how they relate: who is upstream, how the downstream adapts to the
upstream models, and whether upstreams depend on one another.

## Decision Drivers

* Changes in an upstream API should not spread into the aggregator's core (ADR 0008).
* The aggregator calls the upstreams in parallel (ADR 0009), which requires them to be
  independent of each other.
* Pricing provides a capability other consumers could use as well, not a process tied
  to the aggregator.

## Considered Options

* Upstreams behind anti-corruption layers in the aggregator; Pricing as an open host
  service; no dependencies between upstreams
* The aggregator conforms to the upstream models directly

## Decision Outcome

Chosen option: "Upstreams behind anti-corruption layers in the aggregator; Pricing as an
open host service; no dependencies between upstreams", because it keeps upstream changes
at the edge of the aggregator and keeps every upstream callable in parallel.

```mermaid
flowchart LR
  subgraph up["Upstream contexts · independent of each other"]
    direction TB
    CAT["Catalog<br/><i>Product</i>"]
    PRI["Pricing<br/><i>Valuation archetype</i><br/>OHS · published language"]
    AVA["Availability<br/><i>Inventory</i>"]
    CUS["Customer<br/><i>Party</i>"]
  end
  subgraph agg["Product Aggregator · downstream"]
    direction TB
    A1["ACL<br/>catalog adapter"]
    A2["ACL<br/>pricing adapter"]
    A3["ACL<br/>availability adapter"]
    A4["ACL<br/>customer adapter"]
    CORE(["core<br/>product view"])
    A1 & A2 & A3 & A4 --> CORE
  end
  CAT -- "U → D<br/>REST" --> A1
  PRI -- "U → D<br/>REST" --> A2
  AVA -- "U → D<br/>REST" --> A3
  CUS -- "U → D<br/>REST" --> A4
  PRI -. "open to other consumers<br/>(ordering, quoting …)" .-> OTHER["future consumers"]

  classDef upstream fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef ohs fill:#e4002b,stroke:#9f0020,color:#fff
  classDef acl fill:#fef3c7,stroke:#d97706,color:#78350f
  classDef core fill:#1f2937,stroke:#1f2937,color:#fff
  classDef future fill:#f9fafb,stroke:#9ca3af,color:#6b7280,stroke-dasharray:4 3
  class CAT,AVA,CUS upstream
  class PRI ohs
  class A1,A2,A3,A4 acl
  class CORE core
  class OTHER future
```

### Consequences

* Good, because an upstream API change ends at its adapter.
* Good, because no upstream waits for another, so a slow or failed upstream affects only
  its own section of the response.
* Bad, because every upstream needs a translation into the aggregator's model.
