package com.shopper.aggregator.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.shopper.aggregator.application.port.out.AvailabilityPort;
import com.shopper.aggregator.application.port.out.CatalogPort;
import com.shopper.aggregator.application.port.out.CustomerPort;
import com.shopper.aggregator.application.port.out.PricingPort;
import com.shopper.aggregator.application.port.out.UpstreamException;
import com.shopper.aggregator.application.port.out.UpstreamException.Failure;
import com.shopper.aggregator.domain.CatalogUnavailableException;
import com.shopper.aggregator.domain.CustomerProfile;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.Price;
import com.shopper.aggregator.domain.ProductDetails;
import com.shopper.aggregator.domain.ProductNotFoundException;
import com.shopper.aggregator.domain.ProductView;
import com.shopper.aggregator.domain.Stock;

class ProductViewServiceTest {

    private static final Market PL = Market.of("pl-PL");
    private static final ProductDetails OIL_FILTER =
            new ProductDetails("Filtr oleju", "Filtr oleju do silników ciągników.", Map.of(), List.of());
    private static final Price PRICE = new Price("PLN", new BigDecimal("100.00"), BigDecimal.ZERO,
            new BigDecimal("100.00"), new BigDecimal("23.00"), new BigDecimal("123.00"));
    private static final Stock STOCK = new Stock(42, "Poznań", 1);
    private static final CustomerProfile DEALER = new CustomerProfile("DEALER", List.of("fast-delivery"));
    private static final Duration BUDGET = Duration.ofMillis(300);

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private CatalogPort catalog = (id, market) -> OIL_FILTER;
    private PricingPort pricing = (id, market, customer) -> Optional.of(PRICE);
    private AvailabilityPort availability = (id, market) -> Optional.of(STOCK);
    private CustomerPort customers = id -> Optional.of(DEALER);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private ProductView get(String customerId) {
        return new ProductViewService(catalog, pricing, availability, customers, BUDGET, executor,
                Clock.systemUTC()).get("P-1001", PL, customerId);
    }

    private static UpstreamException failure(Failure failure) {
        return new UpstreamException("test", failure, null);
    }

    @Test
    void allUpstreamsAnswer() {
        ProductView view = get("C-42");

        assertThat(view.product()).isEqualTo(OIL_FILTER);
        assertThat(view.price()).contains(PRICE);
        assertThat(view.stock()).contains(STOCK);
        assertThat(view.customer()).contains(DEALER);
    }

    @Test
    void withoutCustomerIdCustomerServiceIsNotCalled() {
        var calls = new AtomicInteger();
        customers = id -> {
            calls.incrementAndGet();
            return Optional.of(DEALER);
        };

        assertThat(get(null).customer()).isEmpty();
        assertThat(calls).hasValue(0);
    }

    @Test
    void knownCustomerPersonalisesTheView() {
        assertThat(get("C-42").customer()).map(CustomerProfile::segment).contains("DEALER");
    }

    @Test
    void unknownProductIsNotFound() {
        catalog = (id, market) -> {
            throw new ProductNotFoundException(id);
        };

        assertThatThrownBy(() -> get(null)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void failingCatalogFailsTheRequest() {
        catalog = (id, market) -> {
            throw failure(Failure.TIMEOUT);
        };

        assertThatThrownBy(() -> get(null)).isInstanceOf(CatalogUnavailableException.class);
    }

    @Test
    void failingPricingDegradesOnlyThePrice() {
        pricing = (id, market, customer) -> {
            throw failure(Failure.ERROR);
        };

        ProductView view = get(null);

        assertThat(view.price()).isEmpty();
        assertThat(view.product()).isEqualTo(OIL_FILTER);
        assertThat(view.stock()).contains(STOCK);
    }

    @Test
    void failingAvailabilityMakesStockUnknown() {
        availability = (id, market) -> {
            throw failure(Failure.CIRCUIT_OPEN);
        };

        assertThat(get(null).stock()).isEmpty();
    }

    @Test
    void failingCustomerServiceGivesStandardView() {
        customers = id -> {
            throw failure(Failure.ERROR);
        };

        assertThat(get("C-42").customer()).isEmpty();
    }

    @Test
    void hangingPricingIsCutOffAtTheBudget() {
        pricing = (id, market, customer) -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.of(PRICE);
        };

        long start = System.nanoTime();
        ProductView view = get(null);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(view.price()).isEmpty();
        assertThat(elapsedMillis).isLessThan(1_000);
    }
}
