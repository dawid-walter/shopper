package com.shopper.pricing.domain.component;

import java.util.Set;

import com.shopper.pricing.domain.Market;
import com.shopper.pricing.domain.PricingContext;

public record Applicability(Set<Market> markets, Set<String> productIds, Set<String> customerIds) {

    public Applicability {
        markets = Set.copyOf(markets);
        productIds = Set.copyOf(productIds);
        customerIds = Set.copyOf(customerIds);
    }

    public static Applicability any() {
        return new Applicability(Set.of(), Set.of(), Set.of());
    }

    public boolean appliesTo(PricingContext context) {
        return (markets.isEmpty() || markets.contains(context.market()))
                && (productIds.isEmpty() || productIds.contains(context.productId()))
                && (customerIds.isEmpty() || isListedCustomer(context.customerId()));
    }

    private boolean isListedCustomer(String customerId) {
        return customerId != null && customerIds.contains(customerId);
    }
}
