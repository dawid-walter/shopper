package com.shopper.aggregator.domain;

import java.util.Set;

import com.shopper.aggregator.domain.exception.UnsupportedMarketException;

public record Market(String code) {

    private static final Set<String> SUPPORTED = Set.of("pl-PL", "de-DE", "nl-NL");

    public Market {
        if (!SUPPORTED.contains(code)) {
            throw new UnsupportedMarketException(code);
        }
    }
}
