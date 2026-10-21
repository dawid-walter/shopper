package com.shopper.aggregator.infrastructure.adapter.pricing;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.resilience.annotation.Retryable;

import com.shopper.aggregator.application.port.out.PricingPort;
import com.shopper.aggregator.application.port.out.model.Price;
import com.shopper.aggregator.domain.CustomerId;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductId;
import com.shopper.aggregator.infrastructure.adapter.FastUpstreamFailure;
import com.shopper.aggregator.infrastructure.adapter.UpstreamClient;

public class PricingAdapter implements PricingPort {

    private record PricingQuote(String currency, BigDecimal basePrice, BigDecimal discount, BigDecimal netPrice,
                                BigDecimal vat, BigDecimal finalPrice) {
    }

    private final UpstreamClient client;

    public PricingAdapter(UpstreamClient client) {
        this.client = client;
    }

    @Override
    @Retryable(includes = FastUpstreamFailure.class, maxRetries = 1, delay = 0)
    public Optional<Price> price(ProductId productId, Market market, Optional<CustomerId> customerId) {
        return client.get(uri -> uri.path("/prices/{id}")
                                .queryParam("market", market.code())
                                .queryParamIfPresent("customerId", customerId.map(CustomerId::value))
                                .build(productId.value()),
                        PricingQuote.class)
                .map(q -> new Price(q.currency(), q.basePrice(), q.discount(), q.netPrice(), q.vat(), q.finalPrice()));
    }
}
