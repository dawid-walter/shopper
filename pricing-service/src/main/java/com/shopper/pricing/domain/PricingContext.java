package com.shopper.pricing.domain;

import java.time.Instant;

public record PricingContext(String productId, Market market, String customerId, Instant at) {
}
