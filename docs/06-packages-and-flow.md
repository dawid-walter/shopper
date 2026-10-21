# 06 · Packages and flow

> Status: **agreed**. Layers: `domain` · `application` · `infrastructure` (outbound adapters, config) · `presentation` (inbound adapters). Main classes only; tests mirror the [behaviour spec](05-behaviour-spec.md). Each service has its own `Market` and `Money`: no shared kernel between services. Packages for future transports (`grpc`, `kafka`, `listener`, `integrationevent`) are not created until needed.

## Aggregator

```
product-aggregator/src/main/java/com/shopper/aggregator/
├── AggregatorApplication
├── domain/
│   ├── Market                    pl-PL / de-DE / nl-NL; parsing an unknown market fails
│   ├── ProductDetails            name, description, specs, images
│   ├── Price                     currency, base, discount, final
│   ├── Stock                     quantity, warehouse, expected delivery
│   ├── CustomerProfile           segment, preferences
│   ├── ProductView               product + Optional price, stock and customer
│   ├── ProductNotFoundException  → 404
│   └── CatalogUnavailableException → 503
├── application/
│   ├── port/in/   GetProductView
│   ├── port/out/  CatalogPort, PricingPort, AvailabilityPort, CustomerPort
│   └── service/   ProductViewService     parallel calls, time budget, builds ProductView
├── infrastructure/
│   ├── adapter/
│   │   ├── catalog/      CatalogRestClient       (+ upstream DTO)
│   │   ├── pricing/      PricingRestClient
│   │   ├── availability/ AvailabilityRestClient
│   │   └── customer/     CustomerRestClient
│   └── config/
│       ├── UpstreamClientsConfig  base URLs and timeout per upstream
│       └── ResilienceConfig       circuit breaker per upstream
└── presentation/
    └── rest/
        ├── ProductController      GET /products/{id}?market&customerId
        ├── ProductResponse        JSON shape
        └── ErrorHandler           exceptions → 400 / 404 / 503

product-aggregator/src/test/java/com/shopper/aggregator/
├── application/service/ProductViewServiceTest   unit: fake ports (A1, A3–A9)
└── component/ProductEndpointComponentTest       whole app + WireMock (A1, A2, A5–A7, A10–A13)
```

Where each concern lives:

| Concern | Place |
|---|---|
| HTTP in / JSON / status codes | `presentation/rest` |
| Timeout and circuit breaker per upstream | `infrastructure/adapter/*` + `infrastructure/config` |
| Parallel calls, time budget, required vs optional | `application/service/ProductViewService` |
| How a missing part is shown to clients (`UNAVAILABLE`, `UNKNOWN`, `STANDARD`) | `presentation/rest/ProductResponse` |

## Pricing

```
pricing-service/src/main/java/com/shopper/pricing/
├── PricingApplication
├── domain/
│   ├── Market, Money             market → currency; amount + currency
│   ├── PricingContext            product, market, customer, moment in time
│   ├── calculator/
│   │   ├── Calculator            sealed: pure function, knows no business
│   │   ├── FixedCalculator       list price
│   │   └── PercentageCalculator  discount, promotion, VAT
│   ├── component/
│   │   ├── PriceComponent        aggregate: identity + versions
│   │   ├── ComponentVersion      immutable: validity + calculator + applicability
│   │   ├── Validity              [from, to)
│   │   ├── Applicability         markets, customers, products
│   │   └── ComponentRole         LIST_PRICE, DISCOUNT, TAX
│   ├── repository/
│   │   └── ComponentRepository   repository of the PriceComponent aggregate
│   ├── PriceQuote                net, VAT, gross + breakdown lines
│   └── NotPricedException        → 404
├── application/
│   ├── port/in/   CalculatePrice
│   └── service/   PricingService   picks applicable components at the moment, sums the breakdown
├── infrastructure/
│   ├── persistence/
│   │   ├── InMemoryComponentRepository
│   │   └── SeedData              shared test data: P-1001, C-42, VAT per market, flash sale
│   └── config/
└── presentation/
    └── rest/
        ├── PriceController       GET /prices/{productId}?market&customerId
        └── PriceResponse

pricing-service/src/test/java/com/shopper/pricing/
├── application/service/PricingServiceTest     examples P1–P4 (fixed clock)
└── presentation/rest/PriceControllerTest      P5 over HTTP
```

Pricing has no outbound port: it calls no other system. Its repository belongs to the domain, because it stores the domain's own aggregate.

## Aggregator request flow

```mermaid
flowchart TD
  REQ(["GET /products/P-1001?market=pl-PL&customerId=C-42"]) --> CTRL["ProductController"]
  CTRL --> MKT{"market known?"}
  MKT -- no --> E400(["400 Bad Request"])
  MKT -- yes --> SVC["ProductViewService<br/>start calls in parallel"]

  SVC --> CAT["CatalogPort"]
  SVC --> PRI["PricingPort"]
  SVC --> AVA["AvailabilityPort"]
  SVC --> CUSQ{"customerId given?"}
  CUSQ -- yes --> CUS["CustomerPort"]
  CUSQ -- no --> CSTD["customer: STANDARD"]

  CAT & PRI & AVA & CUS --> WAIT["wait for all, at most the time budget<br/>(each adapter: own timeout + circuit breaker)"]
  WAIT --> CATR{"catalog result"}
  CATR -- not found --> E404(["404 Not Found"])
  CATR -- failed / timed out --> E503(["503 Service Unavailable"])
  CATR -- ok --> BUILD["build ProductView"]

  BUILD --> PS{"price ok?"}
  PS -- yes --> PA["price: AVAILABLE"]
  PS -- no --> PU["price: UNAVAILABLE"]
  BUILD --> SS{"stock ok?"}
  SS -- yes --> SK["stock: KNOWN"]
  SS -- no --> SU["stock: UNKNOWN"]
  BUILD --> CS{"customer ok?"}
  CS -- yes --> CP["customer: PERSONALIZED"]
  CS -- no --> CSTD

  PA & PU & SK & SU & CP & CSTD --> OK(["200 OK · ProductView JSON"])

  classDef io fill:#1f2937,stroke:#1f2937,color:#fff
  classDef err fill:#e4002b,stroke:#9f0020,color:#fff
  classDef port fill:#eef2ff,stroke:#4f46e5,color:#1e1b4b
  classDef degraded fill:#fef3c7,stroke:#d97706,color:#78350f
  classDef good fill:#dcfce7,stroke:#16a34a,color:#14532d
  class REQ,OK io
  class E400,E404,E503 err
  class CAT,PRI,AVA,CUS port
  class PU,SU,CSTD degraded
  class PA,SK,CP good
```

Reading it in the interview:
- **"A customer in Poland requests a product"**: the path down the green boxes.
- **"Pricing starts timing out"**: `PricingPort` times out (or its circuit is open), so `price: UNAVAILABLE` (amber). The response is still `200`, and it is never slower than the time budget.
