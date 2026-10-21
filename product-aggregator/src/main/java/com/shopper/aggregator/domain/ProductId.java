package com.shopper.aggregator.domain;

import com.shopper.aggregator.domain.exception.InvalidIdentifierException;

public record ProductId(String value) {

    public ProductId {
        if (value == null || value.isBlank()) {
            throw new InvalidIdentifierException("ProductId");
        }
    }
}
