package com.shopper.pricing.domain;

public class NotPricedException extends RuntimeException {

    public NotPricedException(String productId, Market market) {
        super("No price for " + productId + " in " + market.code());
    }
}
