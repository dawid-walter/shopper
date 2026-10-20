---
status: "proposed"
date: 2026-10-20
decision-makers:
consulted:
informed:
---

# Ports and adapters in the aggregator

## Context and Problem Statement

The aggregator is the core of the system: it receives requests over REST and calls
four upstreams. We have to decide how its code is structured internally. Upstream
mocks and Pricing are outside the scope of this decision.

## Decision Drivers

* New data sources must be addable without major refactoring.
* gRPC is out of scope (ADR 0001), but adding it later should not change the core.

## Considered Options

* Ports and adapters (hexagonal): a core with ports, an adapter per upstream and per
  inbound protocol
* Layered: controller → service → upstream clients

## Decision Outcome

Chosen option: "Ports and adapters", because inbound protocols and upstream sources
both become adapters around one application service. Adding gRPC means adding another
inbound adapter next to the REST controller, and adding a data source means adding a
port and its adapter, with no change to how the core handles calls and failures
(ADR 0017).

Packages follow the layers `domain`, `application` (inbound and outbound ports,
services), `infrastructure` (outbound adapters, configuration) and `presentation`
(inbound adapters such as REST).

```mermaid
flowchart LR
  subgraph in["Inbound adapters"]
    direction TB
    REST["REST controller"]
    GRPC["gRPC endpoint<br/>(not built, ADR 0001)"]
  end
  subgraph hex["Core"]
    direction TB
    APP(["application service<br/>compose product view"])
    P1[/"catalog port"/]
    P2[/"pricing port"/]
    P3[/"availability port"/]
    P4[/"customer port"/]
    APP --> P1 & P2 & P3 & P4
  end
  subgraph out["Outbound adapters"]
    direction TB
    O1["catalog REST client"]
    O2["pricing REST client"]
    O3["availability REST client"]
    O4["customer REST client"]
    O5["new source<br/>= port + adapter"]
  end
  REST --> APP
  GRPC -.-> APP
  P1 --- O1
  P2 --- O2
  P3 --- O3
  P4 --- O4

  classDef core fill:#1f2937,stroke:#1f2937,color:#fff
  classDef port fill:#e4002b,stroke:#9f0020,color:#fff
  classDef adapter fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef future fill:#f3f4f6,stroke:#9ca3af,color:#6b7280,stroke-dasharray:4 3
  class APP core
  class P1,P2,P3,P4 port
  class REST,O1,O2,O3,O4 adapter
  class GRPC,O5 future
```

### Consequences

* Good, because the core is tested without a network by substituting the ports.
* Bad, because there are more types and mapping than in a layered structure.
