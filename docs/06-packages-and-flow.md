# 06 · Packages and flow

> Status: **proposal**. Main classes only; the tests mirror the [behaviour spec](#/05-behaviour-spec.md). Each service has its own `Market` and `Money`: no shared kernel between services.

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
│   ├── ProductView               product + price, stock and customer sections
│   ├── PriceSection              AVAILABLE (with Price) | UNAVAILABLE
│   ├── StockSection              KNOWN (with Stock) | UNKNOWN
│   ├── CustomerSection           PERSONALIZED (with profile) | STANDARD
│   ├── ProductNotFoundException  → 404
│   └── CatalogUnavailableException → 503
├── application/
│   ├── ProductViewService        parallel calls, time budget, builds ProductView
│   └── port/
│       ├── in/  GetProductView
│       └── out/ CatalogPort, PricingPort, AvailabilityPort, CustomerPort
├── adapter/
│   ├── in/rest/
│   │   ├── ProductController     GET /products/{id}?market&customerId
│   │   ├── ProductResponse       JSON shape
│   │   └── ErrorHandler          exceptions → 400 / 404 / 503
│   └── out/
│       ├── catalog/      CatalogRestClient       (+ upstream DTO)
│       ├── pricing/      PricingRestClient
│       ├── availability/ AvailabilityRestClient
│       └── customer/     CustomerRestClient
└── config/
    ├── UpstreamClientsConfig     base URLs and timeout per upstream
    └── ResilienceConfig          circuit breaker per upstream

product-aggregator/src/test/java/com/shopper/aggregator/
├── application/ProductViewServiceTest        unit: fake ports (A1, A3–A9)
└── component/ProductEndpointComponentTest    whole app + WireMock (A1, A2, A5–A7, A10–A13)
```

Where each concern lives:

| Concern | Place |
|---|---|
| HTTP in / JSON / status codes | `adapter/in/rest` |
| Timeout and circuit breaker per upstream | `adapter/out/*` + `config` |
| Parallel calls, time budget, required vs optional | `application/ProductViewService` |
| What a section looks like when data is missing | `domain` (the section types) |

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
│   ├── PriceQuote                net, VAT, gross + breakdown lines
│   └── NotPricedException        → 404
├── application/
│   ├── PricingService            picks applicable components at the moment, sums the breakdown
│   └── port/
│       ├── in/  CalculatePrice
│       └── out/ ComponentRepository
└── adapter/
    ├── in/rest/
    │   ├── PriceController       GET /prices/{productId}?market&customerId
    │   └── PriceResponse
    └── out/persistence/
        ├── InMemoryComponentRepository
        └── SeedData              shared test data: P-1001, C-42, VAT per market, flash sale

pricing-service/src/test/java/com/shopper/pricing/
├── application/PricingServiceTest    examples P1–P4 (fixed clock)
└── adapter/in/rest/PriceControllerTest  P5 over HTTP
```

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
- **"A customer in Poland requests a product"**: the left-to-right path down the green boxes.
- **"Pricing starts timing out"**: `PricingPort` times out (or its circuit is open), so `price: UNAVAILABLE` (amber). The response is still `200`, and it is never slower than the time budget.
