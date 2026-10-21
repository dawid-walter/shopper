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

import com.shopper.aggregator.application.port.in.GetProductView;
import com.shopper.aggregator.application.port.out.AvailabilityPort;
import com.shopper.aggregator.application.port.out.CatalogPort;
import com.shopper.aggregator.application.port.out.CustomerPort;
import com.shopper.aggregator.application.port.out.PricingPort;
import com.shopper.aggregator.domain.CatalogUnavailableException;
import com.shopper.aggregator.domain.CustomerProfile;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductDetails;
import com.shopper.aggregator.domain.ProductNotFoundException;
import com.shopper.aggregator.domain.ProductView;

public class ProductViewService implements GetProductView {

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

    private ProductDetails awaitCatalog(CompletableFuture<ProductDetails> future, Instant deadline) {
        try {
            return future.orTimeout(remaining(deadline).toMillis(), TimeUnit.MILLISECONDS).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof ProductNotFoundException notFound) {
                throw notFound;
            }
            throw new CatalogUnavailableException(e.getCause());
        }
    }

    private <T> Optional<T> awaitOptional(CompletableFuture<Optional<T>> future, Instant deadline) {
        return future.completeOnTimeout(Optional.empty(), remaining(deadline).toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(failure -> Optional.empty())
                .join();
    }

    private Duration remaining(Instant deadline) {
        Duration left = Duration.between(clock.instant(), deadline);
        return left.isNegative() ? Duration.ZERO : left;
    }
}
