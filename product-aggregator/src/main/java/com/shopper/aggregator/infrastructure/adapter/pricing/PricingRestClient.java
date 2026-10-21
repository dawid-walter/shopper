package com.shopper.aggregator.infrastructure.adapter.pricing;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.resilience.annotation.Retryable;

import com.shopper.aggregator.application.port.out.PricingPort;
import com.shopper.aggregator.application.port.out.FastUpstreamFailure;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.Price;
import com.shopper.aggregator.infrastructure.adapter.UpstreamClient;

public class PricingRestClient implements PricingPort {

    private record PricingQuote(String currency, BigDecimal basePrice, BigDecimal discount, BigDecimal netPrice,
                                BigDecimal vat, BigDecimal finalPrice) {
    }

    private final UpstreamClient client;

    public PricingRestClient(UpstreamClient client) {
        this.client = client;
    }

    @Override
    @Retryable(includes = FastUpstreamFailure.class, maxRetries = 1, delay = 0)
    public Optional<Price> price(String productId, Market market, String customerId) {
        return client.get(uri -> uri.path("/prices/{id}")
                                .queryParam("market", market.code())
                                .queryParamIfPresent("customerId", Optional.ofNullable(customerId))
                                .build(productId),
                        PricingQuote.class)
                .map(q -> new Price(q.currency(), q.basePrice(), q.discount(), q.netPrice(), q.vat(), q.finalPrice()));
    }
}
