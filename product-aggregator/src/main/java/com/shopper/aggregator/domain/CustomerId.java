package com.shopper.aggregator.domain;

import com.shopper.aggregator.domain.exception.InvalidIdentifierException;

public record CustomerId(String value) {

    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new InvalidIdentifierException("CustomerId");
        }
    }
}
