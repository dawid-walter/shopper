---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Typed ports with one shared fetch path for upstream sources

## Context and Problem Statement

The aggregator composes one response from several upstreams (Scatter-Gather in
Enterprise Integration Patterns, API Composition in microservice terms). New upstreams
will join over time, for example a related-products service. We have to decide how a
source plugs into the aggregator so that adding one stays simple.

## Decision Drivers

* Adding a source should not require new logic for parallel calls, timeouts or
  degradation.
* The code should stay simple and readable at the current number of sources (four).

## Considered Options

* Typed ports, each source going through one shared fetch path in the application
  service
* A generic source contract (name, policy, timeout, fetch) with sources discovered as a
  list, and the response built as a map of sections

## Decision Outcome

Chosen option: "Typed ports with one shared fetch path", because every source gets the
same parallel call, timeout and degradation from one place, while the response keeps a
typed shape. Adding a source means a port, its adapter, one line in the application
service and a new optional section in the response.

```mermaid
flowchart LR
  SVC(["application service"]) --> FETCH{{"shared fetch path<br/>parallel call · timeout · degrade to status"}}
  FETCH --> P1[/"CatalogPort"/]
  FETCH --> P2[/"PricingPort"/]
  FETCH --> P3[/"AvailabilityPort"/]
  FETCH --> P4[/"CustomerPort"/]
  FETCH -.-> P5[/"new source port"/]

  classDef core fill:#1f2937,stroke:#1f2937,color:#fff
  classDef shared fill:#e4002b,stroke:#9f0020,color:#fff
  classDef port fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef future fill:#f3f4f6,stroke:#9ca3af,color:#6b7280,stroke-dasharray:4 3
  class SVC core
  class FETCH shared
  class P1,P2,P3,P4 port
  class P5 future
```

### Consequences

* Good, because resilience behaviour is written once and applies to every source.
* Good, because a new source is an additive change for clients: a new optional section
  with a status, which existing clients ignore.
* Bad, because each new source still touches the application service and the response
  type.

## More Information

The generic source contract becomes worth it when the number of sources grows well
beyond the current four.
