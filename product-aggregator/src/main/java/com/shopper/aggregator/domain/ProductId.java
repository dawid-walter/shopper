package com.shopper.aggregator.domain;

public record ProductId(String value) {

    public ProductId {
        if (value == null || value.isBlank()) {
            throw new InvalidIdentifierException("ProductId");
        }
    }
}
