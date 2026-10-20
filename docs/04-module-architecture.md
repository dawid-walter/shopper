# 04 · Module architecture: aggregator and pricing

> Status: **agreed** (ADR 0008, ADR 0015). Communication is synchronous REST today (ADR 0009); the broker is an option for later. Both services use ports and adapters. Dashed boxes are not built: they show where gRPC and a message broker would plug in.

## The rule

- **Domain + application** in the middle: no Spring Web, no HTTP, no broker.
- **Inbound adapters** (REST today, gRPC tomorrow) call an **inbound port**, a use-case interface.
- **Outbound adapters** (REST clients, repository, broker publisher) implement **outbound ports** declared by the application.
- A new protocol or transport = a new adapter. The middle does not change.

## Aggregator

```mermaid
flowchart LR
  subgraph in["adapter/in"]
    direction TB
    REST["rest<br/>ProductController"]
    GRPC["grpc<br/>ProductGrpcService"]
    MSG["messaging<br/>PriceChangedListener"]
  end
  subgraph core["application + domain"]
    direction TB
    UC[/"GetProductView<br/>(inbound port)"/]
    SVC(["ProductViewService"])
    OUTP[/"CatalogPort · PricingPort<br/>AvailabilityPort · CustomerPort"/]
    UC --> SVC --> OUTP
  end
  subgraph out["adapter/out"]
    direction TB
    C1["catalog REST client"]
    C2["pricing REST client"]
    C3["availability REST client"]
    C4["customer REST client"]
  end
  REST --> UC
  GRPC -.-> UC
  MSG -. "e.g. evict cached price" .-> SVC
  OUTP --- C1 & C2 & C3 & C4

  classDef core fill:#1f2937,stroke:#1f2937,color:#fff
  classDef port fill:#e4002b,stroke:#9f0020,color:#fff
  classDef adapter fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef future fill:#f3f4f6,stroke:#9ca3af,color:#6b7280,stroke-dasharray:4 3
  class SVC core
  class UC,OUTP port
  class REST,C1,C2,C3,C4 adapter
  class GRPC,MSG future
```

```
com.shopper.aggregator
├── domain/            ProductView, sections, Market
├── application/       ProductViewService
│   └── port/
│       ├── in/        GetProductView
│       └── out/       CatalogPort, PricingPort, AvailabilityPort, CustomerPort
└── adapter/
    ├── in/rest/       ProductController
    └── out/           catalog/, pricing/, availability/, customer/  (REST clients + resilience)
```

## Pricing

```mermaid
flowchart LR
  subgraph in["adapter/in"]
    direction TB
    REST["rest<br/>PriceController"]
    GRPC["grpc<br/>PriceGrpcService"]
  end
  subgraph core["application + domain"]
    direction TB
    UC[/"CalculatePrice<br/>(inbound port)"/]
    SVC(["PricingService"])
    DOM["domain: valuation archetype<br/>calculators · components · validity"]
    REPO[/"ComponentRepository<br/>(outbound port)"/]
    PUB[/"PriceEvents<br/>(outbound port)"/]
    UC --> SVC --> DOM
    SVC --> REPO
    SVC -.-> PUB
  end
  subgraph out["adapter/out"]
    direction TB
    MEM["persistence<br/>InMemoryComponentRepository"]
    KAFKA["messaging<br/>broker publisher"]
  end
  REST --> UC
  GRPC -.-> UC
  REPO --- MEM
  PUB -.- KAFKA

  classDef core fill:#1f2937,stroke:#1f2937,color:#fff
  classDef dom fill:#374151,stroke:#374151,color:#fff
  classDef port fill:#e4002b,stroke:#9f0020,color:#fff
  classDef adapter fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef future fill:#f3f4f6,stroke:#9ca3af,color:#6b7280,stroke-dasharray:4 3
  class SVC core
  class DOM dom
  class UC,REPO port
  class REST,MEM adapter
  class GRPC,KAFKA,PUB future
```

```
com.shopper.pricing
├── domain/            Money, Calculator, PriceComponent, Validity, Applicability
├── application/       PricingService
│   └── port/
│       ├── in/        CalculatePrice
│       └── out/       ComponentRepository
└── adapter/
    ├── in/rest/       PriceController
    └── out/persistence/  InMemoryComponentRepository
```

## Future broker: who talks to whom

```mermaid
sequenceDiagram
  participant PS as Pricing · PricingService
  participant PP as PriceEvents (port)
  participant BA as broker adapter (out)
  participant BR as Kafka / Rabbit
  participant LA as listener adapter (in)
  participant AS as Aggregator · application
  PS->>PP: price component changed
  PP->>BA: implemented by
  BA->>BR: publish event
  BR->>LA: deliver event
  LA->>AS: call inbound port (e.g. evict cached price)
```

Services never talk to the broker from the core: **adapters talk to adapters**, ports keep the core unaware of the transport.

## What we build vs what we only show

| | Built | Shown as a slot (diagram + README) |
|---|---|---|
| Inbound | REST controllers | gRPC services, broker listeners |
| Outbound | REST clients, in-memory repository | broker publisher |

Future slots are not created in code (no empty packages, no unused interfaces); they live in this diagram and the README.
