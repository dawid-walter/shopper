package com.shopper.aggregator.domain;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(ProductId productId) {
        super("No product " + productId.value());
    }
}
