package com.shopper.aggregator.domain;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String productId) {
        super("No product " + productId);
    }
}
