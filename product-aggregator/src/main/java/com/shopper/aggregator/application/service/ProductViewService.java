package com.shopper.aggregator.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shopper.aggregator.application.port.in.GetProductView;
import com.shopper.aggregator.application.port.out.AvailabilityPort;
import com.shopper.aggregator.application.port.out.CatalogPort;
import com.shopper.aggregator.application.port.out.CustomerPort;
import com.shopper.aggregator.application.port.out.PricingPort;
import com.shopper.aggregator.application.port.out.UpstreamException;
import com.shopper.aggregator.domain.CatalogUnavailableException;
import com.shopper.aggregator.domain.CustomerId;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductDetails;
import com.shopper.aggregator.domain.ProductId;
import com.shopper.aggregator.domain.ProductNotFoundException;
import com.shopper.aggregator.domain.ProductView;

public class ProductViewService implements GetProductView {

    private static final Logger log = LoggerFactory.getLogger(ProductViewService.class);

    private final CatalogPort catalog;
    private final PricingPort pricing;
    private final AvailabilityPort availability;
    private final CustomerPort customers;
    private final Duration budget;
    private final Executor executor;
    private final Clock clock;

    public ProductViewService(CatalogPort catalog, PricingPort pricing, AvailabilityPort availability,
                              CustomerPort customers, Duration budget, Executor executor,
                              Clock clock) {
        this.catalog = catalog;
        this.pricing = pricing;
        this.availability = availability;
        this.customers = customers;
        this.budget = budget;
        this.executor = executor;
        this.clock = clock;
    }

    @Override
    public ProductView get(ProductId productId, Market market, Optional<CustomerId> customerId) {
        Instant deadline = clock.instant().plus(budget);

        var product = inParallel(() -> catalog.find(productId, market));
        var price = inParallel(() -> pricing.price(productId, market, customerId));
        var stock = inParallel(() -> availability.stock(productId, market));
        var profile = customerId
                .map(id -> inParallel(() -> customers.customer(id)))
                .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty()));

        return new ProductView(productId, market, awaitCatalog(productId, product, deadline),
                awaitOptional(price, deadline), awaitOptional(stock, deadline), awaitOptional(profile, deadline));
    }

    private <T> CompletableFuture<T> inParallel(Supplier<T> call) {
        return CompletableFuture.supplyAsync(call, executor);
    }

    private ProductDetails awaitCatalog(ProductId productId, CompletableFuture<Optional<ProductDetails>> future,
                                        Instant deadline) {
        Optional<ProductDetails> details;
        try {
            details = future.orTimeout(remaining(deadline).toMillis(), TimeUnit.MILLISECONDS).join();
        } catch (CompletionException e) {
            throw new CatalogUnavailableException(e.getCause());
        }
        return details.orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private <T> Optional<T> awaitOptional(CompletableFuture<Optional<T>> future, Instant deadline) {
        return future.completeOnTimeout(Optional.empty(), remaining(deadline).toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(ProductViewService::degrade)
                .join();
    }

    private static <T> Optional<T> degrade(Throwable failure) {
        Throwable cause = failure instanceof CompletionException ? failure.getCause() : failure;
        if (!(cause instanceof UpstreamException)) {
            log.error("Unexpected failure in an optional section", cause);
        }
        return Optional.empty();
    }

    private Duration remaining(Instant deadline) {
        Duration left = Duration.between(clock.instant(), deadline);
        return left.isNegative() ? Duration.ZERO : left;
    }
}
