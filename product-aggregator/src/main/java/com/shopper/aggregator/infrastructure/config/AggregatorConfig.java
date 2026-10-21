package com.shopper.aggregator.infrastructure.config;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.RestClient;

import com.shopper.aggregator.application.port.out.AvailabilityPort;
import com.shopper.aggregator.application.port.out.CatalogPort;
import com.shopper.aggregator.application.port.out.CustomerPort;
import com.shopper.aggregator.application.port.out.PricingPort;
import com.shopper.aggregator.application.service.ProductViewService;
import com.shopper.aggregator.infrastructure.adapter.UpstreamClient;
import com.shopper.aggregator.infrastructure.adapter.availability.AvailabilityRestClient;
import com.shopper.aggregator.infrastructure.adapter.catalog.CatalogRestClient;
import com.shopper.aggregator.infrastructure.adapter.customer.CustomerRestClient;
import com.shopper.aggregator.infrastructure.adapter.pricing.PricingRestClient;
import com.shopper.aggregator.infrastructure.config.AggregatorProperties.Upstream;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@EnableResilientMethods
@EnableConfigurationProperties(AggregatorProperties.class)
class AggregatorConfig {

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(AggregatorProperties properties, MeterRegistry meters) {
        var settings = properties.circuitBreaker();
        var registry = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(settings.window())
                .minimumNumberOfCalls(settings.window())
                .failureRateThreshold(settings.failureRate())
                .waitDurationInOpenState(settings.openFor())
                .permittedNumberOfCallsInHalfOpenState(3)
                .build());
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meters);
        return registry;
    }

    @Bean
    CatalogPort catalogPort(AggregatorProperties properties, CircuitBreakerRegistry breakers,
                            RestClient.Builder builder) {
        return new CatalogRestClient(client("catalog", properties.upstreams().catalog(), breakers, builder));
    }

    @Bean
    PricingPort pricingPort(AggregatorProperties properties, CircuitBreakerRegistry breakers,
                            RestClient.Builder builder) {
        return new PricingRestClient(client("pricing", properties.upstreams().pricing(), breakers, builder));
    }

    @Bean
    AvailabilityPort availabilityPort(AggregatorProperties properties, CircuitBreakerRegistry breakers,
                                      RestClient.Builder builder) {
        return new AvailabilityRestClient(
                client("availability", properties.upstreams().availability(), breakers, builder));
    }

    @Bean
    CustomerPort customerPort(AggregatorProperties properties, CircuitBreakerRegistry breakers,
                              RestClient.Builder builder) {
        return new CustomerRestClient(client("customer", properties.upstreams().customer(), breakers, builder));
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService upstreamExecutor() {
        return ContextExecutorService.wrap(Executors.newVirtualThreadPerTaskExecutor(),
                ContextSnapshotFactory.builder().build());
    }

    @Bean
    ProductViewService productViewService(CatalogPort catalog, PricingPort pricing, AvailabilityPort availability,
                                          CustomerPort customers, AggregatorProperties properties,
                                          ExecutorService upstreamExecutor) {
        return new ProductViewService(catalog, pricing, availability, customers, properties.budget(),
                upstreamExecutor, Clock.systemUTC());
    }

    private static UpstreamClient client(String name, Upstream upstream, CircuitBreakerRegistry breakers,
                                         RestClient.Builder builder) {
        return new UpstreamClient(name, upstream, breakers.circuitBreaker(name), builder);
    }
}
