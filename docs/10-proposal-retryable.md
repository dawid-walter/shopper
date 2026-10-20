# 10 · Proposal: Spring's @Retryable instead of the hand-written retry

> Status: **proposal, not applied to the code plan yet**. Built on a copy of the code: 38 tests green; with `@EnableResilientMethods` switched off, `fastErrorIsRetriedOnce` fails, so the test really exercises the retry.

## What changes

| | Now (code plan) | With `@Retryable` |
|---|---|---|
| Where the retry lives | `ProductViewService.callWithRetry` (application) | `@Retryable` on each adapter method (infrastructure), like in your other service |
| Which failures are retried | `UpstreamException.retryable()` = fast `ERROR` | `includes = FastUpstreamFailure.class` (a subclass thrown for 5xx and refused connections) |
| Budget check before a retry | yes: only if the remaining budget ≥ the upstream's p99 | no; the request is still cut at 500 ms by `orTimeout` / `completeOnTimeout` |
| `ProductViewSettings` | budget + four p99 values | removed; the service takes `Duration budget` |
| `p99` in `application.yml` | four entries | removed |
| Adapters in config | created inline in one `@Bean` | one `@Bean` per port, so Spring can proxy them |
| Retry tests | 2 unit tests with fake ports | 2 component tests with WireMock (retry is now a Spring feature, so it is tested with Spring) |
| Lines | | +84 / −78: about the same in total; the service gets shorter, the config longer |

## The service after the change

```java
public ProductView get(String productId, Market market, String customerId) {
    Instant deadline = clock.instant().plus(budget);

    var product = inParallel(() -> catalog.find(productId, market));
    var price = inParallel(() -> pricing.price(productId, market, customerId));
    var stock = inParallel(() -> availability.stock(productId, market));
    var profile = customerId == null
            ? CompletableFuture.completedFuture(Optional.<CustomerProfile>empty())
            : inParallel(() -> customers.customer(customerId));

    return new ProductView(productId, market, awaitCatalog(product, deadline),
            awaitOptional(price, deadline), awaitOptional(stock, deadline), awaitOptional(profile, deadline));
}

private <T> CompletableFuture<T> inParallel(Supplier<T> call) {
    return CompletableFuture.supplyAsync(call, executor);
}
```

## An adapter after the change

```java
@Override
@Retryable(includes = FastUpstreamFailure.class, maxRetries = 1, delay = 0)
public Optional<Price> price(String productId, Market market, String customerId) {
    return client.get(uri -> uri.path("/prices/{id}") ... , PricingQuote.class)
            .map(q -> new Price(...));
}
```

`delay = 0` matters: Spring's default delay is 1 s, longer than the whole request budget.

## Full diff

```diff
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/application/port/out/FastUpstreamFailure.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/application/port/out/FastUpstreamFailure.java
--- main/application/port/out/FastUpstreamFailure.java
+++ main/application/port/out/FastUpstreamFailure.java
@@ -0,0 +1,8 @@
+package com.shopper.aggregator.application.port.out;
+
+public class FastUpstreamFailure extends UpstreamException {
+
+    public FastUpstreamFailure(String upstream, Throwable cause) {
+        super(upstream, Failure.ERROR, cause);
+    }
+}
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/application/port/out/UpstreamException.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/application/port/out/UpstreamException.java
--- main/application/port/out/UpstreamException.java
+++ main/application/port/out/UpstreamException.java
@@ -14,8 +14,4 @@
     public Failure failure() {
         return failure;
     }
-
-    public boolean retryable() {
-        return failure == Failure.ERROR;
-    }
 }
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/application/service/ProductViewService.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/application/service/ProductViewService.java
--- main/application/service/ProductViewService.java
+++ main/application/service/ProductViewService.java
@@ -15,7 +15,6 @@
 import com.shopper.aggregator.application.port.out.CatalogPort;
 import com.shopper.aggregator.application.port.out.CustomerPort;
 import com.shopper.aggregator.application.port.out.PricingPort;
-import com.shopper.aggregator.application.port.out.UpstreamException;
 import com.shopper.aggregator.domain.CatalogUnavailableException;
 import com.shopper.aggregator.domain.CustomerProfile;
 import com.shopper.aggregator.domain.Market;
@@ -28,51 +27,39 @@
     private final PricingPort pricing;
     private final AvailabilityPort availability;
     private final CustomerPort customers;
-    private final ProductViewSettings settings;
+    private final Duration budget;
     private final Executor executor;
     private final Clock clock;
 
     public ProductViewService(CatalogPort catalog, PricingPort pricing, AvailabilityPort availability,
-                              CustomerPort customers, ProductViewSettings settings, Executor executor,
+                              CustomerPort customers, Duration budget, Executor executor,
                               Clock clock) {
         this.catalog = catalog;
         this.pricing = pricing;
         this.availability = availability;
         this.customers = customers;
-        this.settings = settings;
+        this.budget = budget;
         this.executor = executor;
         this.clock = clock;
     }
 
     @Override
     public ProductView get(String productId, Market market, String customerId) {
-        Instant deadline = clock.instant().plus(settings.budget());
+        Instant deadline = clock.instant().plus(budget);
 
-        var product = fetch(() -> catalog.find(productId, market), settings.catalogP99(), deadline);
-        var price = fetch(() -> pricing.price(productId, market, customerId), settings.pricingP99(), deadline);
-        var stock = fetch(() -> availability.stock(productId, market), settings.availabilityP99(), deadline);
+        var product = inParallel(() -> catalog.find(productId, market));
+        var price = inParallel(() -> pricing.price(productId, market, customerId));
+        var stock = inParallel(() -> availability.stock(productId, market));
         var profile = customerId == null
                 ? CompletableFuture.completedFuture(Optional.<CustomerProfile>empty())
-                : fetch(() -> customers.customer(customerId), settings.customerP99(), deadline);
+                : inParallel(() -> customers.customer(customerId));
 
         return new ProductView(productId, market, awaitCatalog(product, deadline),
                 awaitOptional(price, deadline), awaitOptional(stock, deadline), awaitOptional(profile, deadline));
     }
 
-    private <T> CompletableFuture<T> fetch(Supplier<T> call, Duration retryIfRemaining, Instant deadline) {
-        return CompletableFuture.supplyAsync(() -> callWithRetry(call, retryIfRemaining, deadline), executor);
-    }
-
-    private <T> T callWithRetry(Supplier<T> call, Duration retryIfRemaining, Instant deadline) {
-        try {
-            return call.get();
-        } catch (UpstreamException e) {
-            boolean worthRetrying = e.retryable() && remaining(deadline).compareTo(retryIfRemaining) >= 0;
-            if (!worthRetrying) {
-                throw e;
-            }
-            return call.get();
-        }
+    private <T> CompletableFuture<T> inParallel(Supplier<T> call) {
+        return CompletableFuture.supplyAsync(call, executor);
     }
 
     private ProductDetails awaitCatalog(CompletableFuture<ProductDetails> future, Instant deadline) {
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/application/service/ProductViewSettings.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/application/service/ProductViewSettings.java
--- main/application/service/ProductViewSettings.java
+++ main/application/service/ProductViewSettings.java
@@ -1,7 +0,0 @@
-package com.shopper.aggregator.application.service;
-
-import java.time.Duration;
-
-public record ProductViewSettings(Duration budget, Duration catalogP99, Duration pricingP99,
-                                  Duration availabilityP99, Duration customerP99) {
-}
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/adapter/UpstreamClient.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/adapter/UpstreamClient.java
--- main/infrastructure/adapter/UpstreamClient.java
+++ main/infrastructure/adapter/UpstreamClient.java
@@ -12,6 +12,7 @@
 import org.springframework.web.client.RestClient;
 import org.springframework.web.util.UriBuilder;
 
+import com.shopper.aggregator.application.port.out.FastUpstreamFailure;
 import com.shopper.aggregator.application.port.out.UpstreamException;
 import com.shopper.aggregator.application.port.out.UpstreamException.Failure;
 import com.shopper.aggregator.infrastructure.config.AggregatorProperties.Upstream;
@@ -40,7 +41,7 @@
                     return Optional.<T>empty();
                 }
                 if (response.getStatusCode().isError()) {
-                    throw new UpstreamException(name, Failure.ERROR, null);
+                    throw new FastUpstreamFailure(name, null);
                 }
                 return Optional.ofNullable(response.bodyTo(type));
             }));
@@ -49,7 +50,10 @@
         } catch (CallNotPermittedException e) {
             throw new UpstreamException(name, Failure.CIRCUIT_OPEN, e);
         } catch (ResourceAccessException e) {
-            throw new UpstreamException(name, isTimeout(e) ? Failure.TIMEOUT : Failure.ERROR, e);
+            if (isTimeout(e)) {
+                throw new UpstreamException(name, Failure.TIMEOUT, e);
+            }
+            throw new FastUpstreamFailure(name, e);
         }
     }
 
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/adapter/availability/AvailabilityRestClient.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/adapter/availability/AvailabilityRestClient.java
--- main/infrastructure/adapter/availability/AvailabilityRestClient.java
+++ main/infrastructure/adapter/availability/AvailabilityRestClient.java
@@ -2,7 +2,10 @@
 
 import java.util.Optional;
 
+import org.springframework.resilience.annotation.Retryable;
+
 import com.shopper.aggregator.application.port.out.AvailabilityPort;
+import com.shopper.aggregator.application.port.out.FastUpstreamFailure;
 import com.shopper.aggregator.domain.Market;
 import com.shopper.aggregator.domain.Stock;
 import com.shopper.aggregator.infrastructure.adapter.UpstreamClient;
@@ -19,6 +22,7 @@
     }
 
     @Override
+    @Retryable(includes = FastUpstreamFailure.class, maxRetries = 1, delay = 0)
     public Optional<Stock> stock(String productId, Market market) {
         return client.get(uri -> uri.path("/stock/{id}").queryParam("market", market.code()).build(productId),
                         StockLevel.class)
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/adapter/catalog/CatalogRestClient.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/adapter/catalog/CatalogRestClient.java
--- main/infrastructure/adapter/catalog/CatalogRestClient.java
+++ main/infrastructure/adapter/catalog/CatalogRestClient.java
@@ -3,7 +3,10 @@
 import java.util.List;
 import java.util.Map;
 
+import org.springframework.resilience.annotation.Retryable;
+
 import com.shopper.aggregator.application.port.out.CatalogPort;
+import com.shopper.aggregator.application.port.out.FastUpstreamFailure;
 import com.shopper.aggregator.domain.Market;
 import com.shopper.aggregator.domain.ProductDetails;
 import com.shopper.aggregator.domain.ProductNotFoundException;
@@ -21,6 +24,7 @@
     }
 
     @Override
+    @Retryable(includes = FastUpstreamFailure.class, maxRetries = 1, delay = 0)
     public ProductDetails find(String productId, Market market) {
         return client.get(uri -> uri.path("/products/{id}").queryParam("locale", market.code()).build(productId),
                         CatalogProduct.class)
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/adapter/customer/CustomerRestClient.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/adapter/customer/CustomerRestClient.java
--- main/infrastructure/adapter/customer/CustomerRestClient.java
+++ main/infrastructure/adapter/customer/CustomerRestClient.java
@@ -3,7 +3,10 @@
 import java.util.List;
 import java.util.Optional;
 
+import org.springframework.resilience.annotation.Retryable;
+
 import com.shopper.aggregator.application.port.out.CustomerPort;
+import com.shopper.aggregator.application.port.out.FastUpstreamFailure;
 import com.shopper.aggregator.domain.CustomerProfile;
 import com.shopper.aggregator.infrastructure.adapter.UpstreamClient;
 
@@ -19,6 +22,7 @@
     }
 
     @Override
+    @Retryable(includes = FastUpstreamFailure.class, maxRetries = 1, delay = 0)
     public Optional<CustomerProfile> customer(String customerId) {
         return client.get(uri -> uri.path("/customers/{id}").build(customerId), Customer.class)
                 .map(c -> new CustomerProfile(c.segment(), c.preferences()));
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/adapter/pricing/PricingRestClient.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/adapter/pricing/PricingRestClient.java
--- main/infrastructure/adapter/pricing/PricingRestClient.java
+++ main/infrastructure/adapter/pricing/PricingRestClient.java
@@ -3,7 +3,10 @@
 import java.math.BigDecimal;
 import java.util.Optional;
 
+import org.springframework.resilience.annotation.Retryable;
+
 import com.shopper.aggregator.application.port.out.PricingPort;
+import com.shopper.aggregator.application.port.out.FastUpstreamFailure;
 import com.shopper.aggregator.domain.Market;
 import com.shopper.aggregator.domain.Price;
 import com.shopper.aggregator.infrastructure.adapter.UpstreamClient;
@@ -21,6 +24,7 @@
     }
 
     @Override
+    @Retryable(includes = FastUpstreamFailure.class, maxRetries = 1, delay = 0)
     public Optional<Price> price(String productId, Market market, String customerId) {
         return client.get(uri -> uri.path("/prices/{id}")
                                 .queryParam("market", market.code())
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/config/AggregatorConfig.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/config/AggregatorConfig.java
--- main/infrastructure/config/AggregatorConfig.java
+++ main/infrastructure/config/AggregatorConfig.java
@@ -7,21 +7,25 @@
 import org.springframework.boot.context.properties.EnableConfigurationProperties;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
+import org.springframework.resilience.annotation.EnableResilientMethods;
 
+import com.shopper.aggregator.application.port.out.AvailabilityPort;
+import com.shopper.aggregator.application.port.out.CatalogPort;
+import com.shopper.aggregator.application.port.out.CustomerPort;
+import com.shopper.aggregator.application.port.out.PricingPort;
 import com.shopper.aggregator.application.service.ProductViewService;
-import com.shopper.aggregator.application.service.ProductViewSettings;
 import com.shopper.aggregator.infrastructure.adapter.UpstreamClient;
 import com.shopper.aggregator.infrastructure.adapter.availability.AvailabilityRestClient;
 import com.shopper.aggregator.infrastructure.adapter.catalog.CatalogRestClient;
 import com.shopper.aggregator.infrastructure.adapter.customer.CustomerRestClient;
 import com.shopper.aggregator.infrastructure.adapter.pricing.PricingRestClient;
-import com.shopper.aggregator.infrastructure.config.AggregatorProperties.Upstream;
 
 import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
 import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
 import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
 
 @Configuration
+@EnableResilientMethods
 @EnableConfigurationProperties(AggregatorProperties.class)
 class AggregatorConfig {
 
@@ -38,27 +42,40 @@
                 .build());
     }
 
+    @Bean
+    CatalogPort catalogPort(AggregatorProperties properties, CircuitBreakerRegistry breakers) {
+        return new CatalogRestClient(new UpstreamClient("catalog", properties.upstreams().catalog(),
+                breakers.circuitBreaker("catalog")));
+    }
+
+    @Bean
+    PricingPort pricingPort(AggregatorProperties properties, CircuitBreakerRegistry breakers) {
+        return new PricingRestClient(new UpstreamClient("pricing", properties.upstreams().pricing(),
+                breakers.circuitBreaker("pricing")));
+    }
+
+    @Bean
+    AvailabilityPort availabilityPort(AggregatorProperties properties, CircuitBreakerRegistry breakers) {
+        return new AvailabilityRestClient(new UpstreamClient("availability", properties.upstreams().availability(),
+                breakers.circuitBreaker("availability")));
+    }
+
+    @Bean
+    CustomerPort customerPort(AggregatorProperties properties, CircuitBreakerRegistry breakers) {
+        return new CustomerRestClient(new UpstreamClient("customer", properties.upstreams().customer(),
+                breakers.circuitBreaker("customer")));
+    }
+
     @Bean(destroyMethod = "shutdown")
     ExecutorService upstreamExecutor() {
         return Executors.newVirtualThreadPerTaskExecutor();
     }
 
     @Bean
-    ProductViewService productViewService(AggregatorProperties properties, CircuitBreakerRegistry breakers,
+    ProductViewService productViewService(CatalogPort catalog, PricingPort pricing, AvailabilityPort availability,
+                                          CustomerPort customers, AggregatorProperties properties,
                                           ExecutorService upstreamExecutor) {
-        var upstreams = properties.upstreams();
-        return new ProductViewService(
-                new CatalogRestClient(client("catalog", upstreams.catalog(), breakers)),
-                new PricingRestClient(client("pricing", upstreams.pricing(), breakers)),
-                new AvailabilityRestClient(client("availability", upstreams.availability(), breakers)),
-                new CustomerRestClient(client("customer", upstreams.customer(), breakers)),
-                new ProductViewSettings(properties.budget(), upstreams.catalog().p99(), upstreams.pricing().p99(),
-                        upstreams.availability().p99(), upstreams.customer().p99()),
-                upstreamExecutor,
-                Clock.systemUTC());
-    }
-
-    private static UpstreamClient client(String name, Upstream upstream, CircuitBreakerRegistry breakers) {
-        return new UpstreamClient(name, upstream, breakers.circuitBreaker(name));
+        return new ProductViewService(catalog, pricing, availability, customers, properties.budget(),
+                upstreamExecutor, Clock.systemUTC());
     }
 }
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/config/AggregatorProperties.java retryable/product-aggregator/src/main/java/com/shopper/aggregator/infrastructure/config/AggregatorProperties.java
--- main/infrastructure/config/AggregatorProperties.java
+++ main/infrastructure/config/AggregatorProperties.java
@@ -14,6 +14,6 @@
     public record Upstreams(Upstream catalog, Upstream pricing, Upstream availability, Upstream customer) {
     }
 
-    public record Upstream(URI url, Duration timeout, Duration p99) {
+    public record Upstream(URI url, Duration timeout) {
     }
 }
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/main/resources/application.yml retryable/product-aggregator/src/main/resources/application.yml
--- src/main/resources/application.yml
+++ src/main/resources/application.yml
@@ -17,16 +17,12 @@
     catalog:
       url: http://localhost:8081
       timeout: 235ms
-      p99: 160ms
     pricing:
       url: http://localhost:8082
       timeout: 375ms
-      p99: 256ms
     availability:
       url: http://localhost:8083
       timeout: 470ms
-      p99: 320ms
     customer:
       url: http://localhost:8084
       timeout: 280ms
-      p99: 192ms
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/test/java/com/shopper/aggregator/application/service/ProductViewServiceTest.java retryable/product-aggregator/src/test/java/com/shopper/aggregator/application/service/ProductViewServiceTest.java
--- test/application/service/ProductViewServiceTest.java
+++ test/application/service/ProductViewServiceTest.java
@@ -40,8 +40,7 @@
             new BigDecimal("100.00"), new BigDecimal("23.00"), new BigDecimal("123.00"));
     private static final Stock STOCK = new Stock(42, "Poznań", 1);
     private static final CustomerProfile DEALER = new CustomerProfile("DEALER", List.of("fast-delivery"));
-    private static final ProductViewSettings SETTINGS = new ProductViewSettings(Duration.ofMillis(300),
-            Duration.ofMillis(50), Duration.ofMillis(50), Duration.ofMillis(50), Duration.ofMillis(50));
+    private static final Duration BUDGET = Duration.ofMillis(300);
 
     private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
 
@@ -56,7 +55,7 @@
     }
 
     private ProductView get(String customerId) {
-        return new ProductViewService(catalog, pricing, availability, customers, SETTINGS, executor,
+        return new ProductViewService(catalog, pricing, availability, customers, BUDGET, executor,
                 Clock.systemUTC()).get("P-1001", PL, customerId);
     }
 
@@ -158,30 +157,4 @@
         assertThat(view.price()).isEmpty();
         assertThat(elapsedMillis).isLessThan(1_000);
     }
-
-    @Test
-    void fastErrorIsRetriedOnce() {
-        var calls = new AtomicInteger();
-        pricing = (id, market, customer) -> {
-            if (calls.incrementAndGet() == 1) {
-                throw failure(Failure.ERROR);
-            }
-            return Optional.of(PRICE);
-        };
-
-        assertThat(get(null).price()).contains(PRICE);
-        assertThat(calls).hasValue(2);
-    }
-
-    @Test
-    void timeoutIsNotRetried() {
-        var calls = new AtomicInteger();
-        pricing = (id, market, customer) -> {
-            calls.incrementAndGet();
-            throw failure(Failure.TIMEOUT);
-        };
-
-        assertThat(get(null).price()).isEmpty();
-        assertThat(calls).hasValue(1);
-    }
 }
diff --color -ruN '--exclude=build' '--exclude=.gradle' shopper/product-aggregator/src/test/java/com/shopper/aggregator/component/ProductEndpointComponentTest.java retryable/product-aggregator/src/test/java/com/shopper/aggregator/component/ProductEndpointComponentTest.java
--- test/component/ProductEndpointComponentTest.java
+++ test/component/ProductEndpointComponentTest.java
@@ -26,6 +26,7 @@
 
 import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
 import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
+import com.github.tomakehurst.wiremock.stubbing.Scenario;
 
 import io.github.resilience4j.circuitbreaker.CircuitBreaker;
 import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
@@ -173,4 +174,26 @@
 
         upstreams.verify(exactly(4), getRequestedFor(urlPathEqualTo("/prices/P-1001")));
     }
+
+    @Test
+    void fastErrorIsRetriedOnce() throws Exception {
+        upstreams.stubFor(get(urlPathEqualTo("/prices/P-1001")).inScenario("retry")
+                .whenScenarioStateIs(Scenario.STARTED).willReturn(aResponse().withStatus(503))
+                .willSetStateTo("recovered"));
+        upstreams.stubFor(get(urlPathEqualTo("/prices/P-1001")).inScenario("retry")
+                .whenScenarioStateIs("recovered").willReturn(okJson("{\"currency\": \"PLN\", \"finalPrice\": 110.70}")));
+
+        request("pl-PL").andExpect(jsonPath("$.price.status").value("AVAILABLE"));
+
+        upstreams.verify(exactly(2), getRequestedFor(urlPathEqualTo("/prices/P-1001")));
+    }
+
+    @Test
+    void timeoutIsNotRetried() throws Exception {
+        stubPricing(okJson("{}").withFixedDelay(1_000));
+
+        request("pl-PL").andExpect(jsonPath("$.price.status").value("UNAVAILABLE"));
+
+        upstreams.verify(exactly(1), getRequestedFor(urlPathEqualTo("/prices/P-1001")));
+    }
 }
```
