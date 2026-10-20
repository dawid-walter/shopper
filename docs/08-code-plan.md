# 08 · Code plan

> **Read the code in [09 · Code browser](#/09-code-browser.html)**: tabs per module, files grouped by layer, filter by stage.

> Status: **for review**. Every class and test that will be written, split into stages. Each stage is two commits: the tests first (red), then the implementation (green). Stages 0 and 6 are one commit each.
>
> The code below was **built and run before being published here**: Gradle build green, 44 tests passing three runs in a row, and the system checked end to end in Docker. What you read is what goes into the repository.

## Stages

| Stage | Adds | Tests | Depends on |
|---|---|---|---|
| 0 | Gradle skeleton, five bootable modules | context loads ×5 | — |
| 1 | Catalog, Availability, Customer mocks | 7 | 0 |
| 2 | Pricing model, service, in-memory storage | 7 (P1–P7) | 0 |
| 3 | Pricing REST API | 5 | 2 |
| 4 | Aggregator domain, ports, service | 11 | 0 |
| 5 | Aggregator adapters, config, REST API | 9 | 4 (contracts from 1, 3) |
| 6 | Dockerfile, docker compose | by hand | 1, 3, 5 |

Stages 1, 2 and 4 are independent of each other and can run in parallel.

## Stage 0 · Gradle skeleton

**Commits:** `build: add Gradle multi-module skeleton for five services` (one commit: nothing to test yet beyond the contexts starting)

Five Spring Boot modules in one Gradle build (ADR 0006, 0007), Java 25 toolchain, Spring Boot 4.1.1 (ADR 0005). Each module starts and its context loads. Ports: aggregator 8080, catalog 8081, pricing 8082, availability 8083, customer 8084. The Gradle wrapper (`gradlew`, `gradle/`) is generated with `gradle wrapper --gradle-version 9.8.2` and not shown.

**Spec covered:** —

**Files:**

`settings.gradle.kts`, `build.gradle.kts`, `AggregatorApplication.java`, `AggregatorApplicationTest.java`, `PricingApplication.java`, `PricingApplicationTest.java`, `CatalogApplication.java`, `CatalogApplicationTest.java`, `AvailabilityApplication.java`, `AvailabilityApplicationTest.java`, `CustomerApplication.java`, `CustomerApplicationTest.java`, `application.yml`, `application.yml`, `application.yml`, `application.yml`, `application.yml`

## Stage 1 · Upstream mocks

**Commits:** `test(mocks): specify mock upstream behaviour` (tests, red) → `feat(mocks): implement catalog, availability and customer mocks` (green)

Three plain services with fixed sample data shared by all services (spec page 05): P-1001 in four languages, stock per market, customers C-42 (DEALER) and C-7. Simple layered code, no ports: there is no logic to protect (ADR 0003). Catalog falls back to English for an unknown language.

**Spec covered:** Mock contracts used by the aggregator adapters (stage 5).

**Tests (commit 1):**

`CatalogControllerTest.java`, `StockControllerTest.java`, `CustomerControllerTest.java`

**Implementation (commit 2):**

`CatalogController.java`, `CatalogData.java`, `Product.java`, `StockController.java`, `StockData.java`, `StockLevel.java`, `Customer.java`, `CustomerController.java`, `CustomerData.java`

## Stage 2 · Pricing core (valuation archetype)

**Commits:** `test(pricing): specify pricing behaviour P1–P7` (tests, red) → `feat(pricing): implement the pricing model, service and in-memory storage` (green)

The archetype in its minimal form: `Calculator` (sealed: Fixed, Percentage) knows only maths; `PriceComponent` gives it a role (LIST_PRICE, DISCOUNT, TAX) and holds immutable versions with `Validity` and `Applicability`; `Tariff` is the composite: the ordered component ids of a market, so discounts come before VAT. `PriceQuote` sums the breakdown lines and refuses a price without a list price or tax (`NotPricedException`). Writes go through `ComponentRepository.update`, an atomic compare-and-set on the aggregate's version (optimistic locking, ADR 0013). Storage is in memory (ADR 0004).

**Spec covered:** P1–P7 in `PricingServiceTest`: real domain, in-memory repositories, fixed clock.

**Tests (commit 1):**

`PricingServiceTest.java`

**Implementation (commit 2):**

`CalculatePrice.java`, `ManagePriceComponents.java`, `PricingService.java`, `Market.java`, `Money.java`, `NotPricedException.java`, `PriceQuote.java`, `PricingContext.java`, `QuoteLine.java`, `UnsupportedMarketException.java`, `Calculator.java`, `FixedCalculator.java`, `PercentageCalculator.java`, `Applicability.java`, `ComponentNotFoundException.java`, `ComponentRole.java`, `ComponentVersion.java`, `DuplicateVersionException.java`, `PriceComponent.java`, `Tariff.java`, `Validity.java`, `ComponentRepository.java`, `StaleComponentException.java`, `TariffRepository.java`, `PricingConfig.java`, `InMemoryComponentRepository.java`, `InMemoryTariffRepository.java`, `SeedData.java`

## Stage 3 · Pricing REST API

**Commits:** `test(pricing): specify the pricing HTTP API` (tests, red) → `feat(pricing): expose prices and component versions over REST` (green)

`GET /prices/{id}?market&customerId&at` (`at` previews a future price), `GET /components/{id}` (the version history), `POST /components/{id}/versions` with `expectedVersion`. Domain exceptions map to 404 / 409 / 400 as problem details.

**Spec covered:** P5, P6, P7 over HTTP plus the full breakdown, in `PricingApiTest`.

**Tests (commit 1):**

`PricingApiTest.java`

**Implementation (commit 2):**

`AddVersionRequest.java`, `ComponentController.java`, `ComponentResponse.java`, `ErrorHandler.java`, `PriceController.java`, `PriceResponse.java`

## Stage 4 · Aggregator core

**Commits:** `test(aggregator): specify product view composition A1, A3–A10` (tests, red) → `feat(aggregator): implement the product view service with one shared fetch path` (green)

Domain records and the three section types (each with a status), ports in and out, and `ProductViewService`: Scatter-Gather over the four ports (ADR 0009) through one shared fetch path (ADR 0017): run in parallel, retry a fast error once if the remaining budget covers the upstream's p99 (ADR 0016), cut everything at the budget. Catalog is required (404 / 503), the rest degrade. Optional ports return `Optional`: a 404 there is "no data", not a failure.

**Spec covered:** A1, A3–A10, plus "fast error is retried once" and "timeout is not retried", in `ProductViewServiceTest`, with lambdas as fake ports.

**Tests (commit 1):**

`ProductViewServiceTest.java`

**Implementation (commit 2):**

`GetProductView.java`, `AvailabilityPort.java`, `CatalogPort.java`, `CustomerPort.java`, `PricingPort.java`, `UpstreamException.java`, `ProductViewService.java`, `ProductViewSettings.java`, `CatalogUnavailableException.java`, `CustomerProfile.java`, `CustomerSection.java`, `Market.java`, `Price.java`, `PriceSection.java`, `ProductDetails.java`, `ProductNotFoundException.java`, `ProductView.java`, `Stock.java`, `StockSection.java`, `UnsupportedMarketException.java`

## Stage 5 · Aggregator adapters and REST API

**Commits:** `test(aggregator): specify the aggregator over HTTP A1, A2, A5–A7, A10–A13` (tests, red) → `feat(aggregator): add REST adapters with timeouts and circuit breakers, and the product endpoint` (green)

`UpstreamClient`: one per upstream, with its own timeout (p99.9) and circuit breaker; every failure leaves as `UpstreamException` (ERROR, TIMEOUT, CIRCUIT_OPEN), so HTTP and Resilience4j never reach the core. Four small adapters translate upstream JSON into domain records, reading only the fields they need. `ProductController` + `ErrorHandler` + `ProductResponse` (every section has `status`). `AggregatorConfig` is the only place that knows both sides; values from ADR 0016 in `application.yml`.

**Spec covered:** A1, A2, A5–A7, A10–A13 in `ProductEndpointComponentTest`: the whole app, upstreams stubbed by one WireMock server, circuit window 4 so A13 needs four requests.

**Tests (commit 1):**

`ProductEndpointComponentTest.java`

**Implementation (commit 2):**

`build.gradle.kts`, `application.yml`, `UpstreamClient.java`, `AvailabilityRestClient.java`, `CatalogRestClient.java`, `CustomerRestClient.java`, `PricingRestClient.java`, `AggregatorConfig.java`, `AggregatorProperties.java`, `ErrorHandler.java`, `ProductController.java`, `ProductResponse.java`

## Stage 6 · Docker

**Commits:** `build(docker): run the whole system with docker compose` (one commit: verified by running it)

One Dockerfile for every module (build stage with the Gradle wrapper, JRE runtime stage) and `docker-compose.yml` with the five services; the aggregator gets upstream URLs through environment variables. `docker compose up --build` is the only setup.

**Spec covered:** Checked by hand, see Verification.

**Files:**

`Dockerfile`, `dot-dockerignore`, `docker-compose.yml`

## Where the code differs from earlier pages

Decided while writing the code; to confirm in review:

1. **Composite = `Tariff`, not a versioned `CompositeComponent`.** A tariff is the ordered list of component ids for a market, without versions. The structure of a price changes rarely; the rates inside components are versioned. ADR 0013's diagram shows a `CompositeVersion`; it would be updated to match.
2. **List prices as data inside one component per market** (`list-price-pl`): each product is a version with `Applicability` on the product id. Adding a product or a new list price is a new version, not new code (archetype audit finding 2).
3. **A price needs a list price and a tax**; otherwise `NotPricedException` → 404 (archetype audit finding 1: an unconfigured market never gets a silently wrong price).
4. **One rounding point:** each breakdown line is rounded to cents in `Money`; the totals are sums of rounded lines, like an invoice.
5. **Optional ports return `Optional`**: a 404 from Pricing, Availability or Customer means "no data", and the circuit breaker counts it as a success. Only Catalog turns 404 into `ProductNotFoundException`.
6. **Retry lives in the application's fetch path**, because only it knows the remaining budget; timeouts and circuit breakers live in `UpstreamClient` (infrastructure).
7. **Extra classes** not on page 06: `ProductViewSettings`, `UpstreamException`, `UpstreamClient`, `AggregatorProperties`, `ManagePriceComponents` (second inbound port in Pricing), `TariffRepository`, `QuoteLine`, `Applicability`, and small exception types.
8. **Spec additions:** two unit tests for the retry rule (fast error retried once, timeout never), and a 400 test for an unknown market in Pricing.
9. **Not in this plan:** Swagger UI, Toxiproxy, Grafana, the chaos skill and the README. They come after, as planned.

## Verification done

- `./gradlew test`: 44 tests, all green, three runs in a row (timing tests A10, A11 included).
- `docker compose up --build`, then:

| Request | Result |
|---|---|
| `pl-PL`, `C-42` | `200`; Polish texts, 110.70 PLN after the 10% contract discount, 42 in Poznań, `PERSONALIZED` |
| `de-DE` | `200`; German texts, 29.75 EUR |
| `nl-NL` | `200`; `price: UNAVAILABLE` (no price list in NL) |
| `P-9999` | `404` |
| `xx-XX` | `400` |
| Pricing container stopped | `200`, `price: UNAVAILABLE`, in 0.39 s (cut by the pricing timeout) |
| Catalog container stopped | `503` in 0.24 s |
