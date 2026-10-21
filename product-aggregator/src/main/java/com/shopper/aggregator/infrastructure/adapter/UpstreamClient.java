package com.shopper.aggregator.infrastructure.adapter;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.shopper.aggregator.application.port.out.FastUpstreamFailure;
import com.shopper.aggregator.application.port.out.UpstreamException;
import com.shopper.aggregator.application.port.out.UpstreamException.Failure;
import com.shopper.aggregator.application.port.out.UpstreamRejectedRequest;
import com.shopper.aggregator.infrastructure.config.AggregatorProperties.Upstream;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;

public class UpstreamClient {

    private static final Logger log = LoggerFactory.getLogger(UpstreamClient.class);

    private final String name;
    private final RestClient rest;
    private final CircuitBreaker circuitBreaker;
    private final MeterRegistry meters;

    public UpstreamClient(String name, Upstream upstream, CircuitBreaker circuitBreaker, RestClient.Builder builder,
                          MeterRegistry meters) {
        var requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(upstream.timeout()).build());
        requestFactory.setReadTimeout(upstream.timeout());
        this.name = name;
        this.rest = builder.clone().baseUrl(upstream.url().toString()).requestFactory(requestFactory).build();
        this.circuitBreaker = circuitBreaker;
        this.meters = meters;
    }

    public <T> Optional<T> get(Function<UriBuilder, URI> uri, Class<T> type) {
        try {
            return circuitBreaker.executeSupplier(() -> rest.get().uri(uri).exchange((request, response) -> {
                int status = response.getStatusCode().value();
                if (status == 404) {
                    return Optional.<T>empty();
                }
                if (response.getStatusCode().is4xxClientError() && status != 408 && status != 429) {
                    throw new UpstreamRejectedRequest(name, status);
                }
                if (response.getStatusCode().isError()) {
                    throw new FastUpstreamFailure(name, status);
                }
                return Optional.ofNullable(response.bodyTo(type));
            }));
        } catch (UpstreamException e) {
            throw logged(e);
        } catch (CallNotPermittedException e) {
            throw logged(new UpstreamException(name, Failure.CIRCUIT_OPEN, e));
        } catch (ResourceAccessException e) {
            throw logged(isTimeout(e)
                    ? new UpstreamException(name, Failure.TIMEOUT, e)
                    : new FastUpstreamFailure(name, e));
        } catch (RestClientException e) {
            throw logged(new UpstreamException(name, Failure.ERROR, e));
        }
    }

    private UpstreamException logged(UpstreamException e) {
        log.warn("{} call failed: {}", name, e.reason());
        meters.counter("aggregator.upstream.failures", "upstream", name, "reason", e.failure().name()).increment();
        return e;
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
