---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Aggregator as an integration context without DDD aggregates

## Context and Problem Statement

The aggregator composes a product view from upstream data. It has no business rules of
its own to protect: it decides which sources are required, how each section degrades
and how the response fits the market. We have to decide how much domain modelling it
gets.

## Decision Drivers

* The aggregator is a simple service; its problem is integration and presentation, not
  business invariants.

## Considered Options

* An integration context: a plain model of sections, source policies and market context,
  without DDD aggregates
* Full tactical DDD: aggregates, domain events and repositories in the aggregator

## Decision Outcome

Chosen option: "An integration context without DDD aggregates", because there is no
state whose consistency the aggregator must guard, so aggregates would add structure
without protecting anything.

```mermaid
flowchart LR
  REQ["request<br/>product · market · customer?"] --> CTX["market context<br/>language · currency"]
  CTX --> FAN{{"fan-out"}}
  FAN --> S1["catalog section<br/><b>required</b>"]
  FAN --> S2["price section<br/>optional"]
  FAN --> S3["stock section<br/>optional"]
  FAN --> S4["customer section<br/>conditional: only with a customer"]
  S1 & S2 & S3 & S4 --> POL["source policy<br/>OK · UNAVAILABLE · fail request"]
  POL --> RES["product view"]

  classDef io fill:#1f2937,stroke:#1f2937,color:#fff
  classDef req fill:#e4002b,stroke:#9f0020,color:#fff
  classDef opt fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef step fill:#f9fafb,stroke:#6b7280,color:#111827
  class REQ,RES io
  class S1 req
  class S2,S3,S4 opt
  class CTX,FAN,POL step
```

### Consequences

* Good, because the aggregator stays small and easy to follow.
* Good, because the contrast is deliberate: tactical DDD is applied in Pricing (ADR 0013),
  where real invariants exist.
