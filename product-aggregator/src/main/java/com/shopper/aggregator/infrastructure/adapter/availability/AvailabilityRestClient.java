package com.shopper.aggregator.infrastructure.adapter.availability;

import java.util.Optional;

import org.springframework.resilience.annotation.Retryable;

import com.shopper.aggregator.application.port.out.AvailabilityPort;
import com.shopper.aggregator.application.port.out.model.Stock;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductId;
import com.shopper.aggregator.infrastructure.adapter.FastUpstreamFailure;
import com.shopper.aggregator.infrastructure.adapter.UpstreamClient;

public class AvailabilityRestClient implements AvailabilityPort {

    private record StockLevel(int quantity, String warehouse, int deliveryInDays) {
    }

    private final UpstreamClient client;

    public AvailabilityRestClient(UpstreamClient client) {
        this.client = client;
    }

    @Override
    @Retryable(includes = FastUpstreamFailure.class, maxRetries = 1, delay = 0)
    public Optional<Stock> stock(ProductId productId, Market market) {
        return client.get(uri -> uri.path("/stock/{id}").queryParam("market", market.code()).build(productId.value()),
                        StockLevel.class)
                .map(s -> new Stock(s.quantity(), s.warehouse(), s.deliveryInDays()));
    }
}
