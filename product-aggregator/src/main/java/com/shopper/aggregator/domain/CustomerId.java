package com.shopper.aggregator.domain;

public record CustomerId(String value) {

    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new InvalidIdentifierException("CustomerId");
        }
    }
}
