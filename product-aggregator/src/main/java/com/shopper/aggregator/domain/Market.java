package com.shopper.aggregator.domain;

import java.util.Set;

public record Market(String code) {

    private static final Set<String> SUPPORTED = Set.of("pl-PL", "de-DE", "nl-NL");

    public static Market of(String code) {
        if (!SUPPORTED.contains(code)) {
            throw new IllegalArgumentException("Unsupported market: " + code);
        }
        return new Market(code);
    }
}
