package com.shopper.aggregator.application.port.in.exception;

import com.shopper.aggregator.domain.ProductId;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(ProductId productId) {
        super("No product " + productId.value());
    }
}
