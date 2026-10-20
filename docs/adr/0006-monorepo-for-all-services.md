---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Monorepo for all services

## Context and Problem Statement

The system consists of five services (ADR 0002): the aggregator and four upstreams.
We have to decide how their code is organised into repositories.

## Decision Drivers

* The system is small, delivered as one unit, and has to run locally with minimal
  setup.

## Considered Options

* One repository with a module per service
* A separate repository per service

## Decision Outcome

Chosen option: "One repository with a module per service", because the system is
reviewed, built and run as one unit.

```mermaid
flowchart TB
  REPO[("shopper<br/>one repository")]
  REPO --> M1["product-aggregator"]
  REPO --> M2["pricing-service"]
  REPO --> M3["catalog-service"]
  REPO --> M4["availability-service"]
  REPO --> M5["customer-service"]
  REPO --> DC["docker-compose.yml<br/>one command starts everything"]
  REPO --> DOC["docs/adr"]

  classDef repo fill:#1f2937,stroke:#1f2937,color:#fff
  classDef mod fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef file fill:#fef3c7,stroke:#d97706,color:#78350f
  class REPO repo
  class M1,M2,M3,M4,M5 mod
  class DC,DOC file
```

### Consequences

* Good, because one clone and one command start the whole system.
* Neutral, because the choice fits the current size of the system and should be
  revisited as it grows.

## More Information

As the system grows the choice depends on complexity: a modular monolith is the likely
default, and for a very large system each service would get its own repository.
