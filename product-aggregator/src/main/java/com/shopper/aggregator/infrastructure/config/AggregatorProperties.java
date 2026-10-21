package com.shopper.aggregator.infrastructure.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("aggregator")
public record AggregatorProperties(Duration budget, CircuitBreaker circuitBreaker, Upstreams upstreams) {

    public record CircuitBreaker(int window, float failureRate, Duration openFor, int halfOpenCalls) {
    }

    public record Upstreams(Upstream catalog, Upstream pricing, Upstream availability, Upstream customer) {
    }

    public record Upstream(URI url, Duration timeout) {
    }
}
