package com.shopper.pricing.domain;

import java.util.Arrays;

public enum Market {

    PL_PL("pl-PL", "PLN"),
    DE_DE("de-DE", "EUR"),
    NL_NL("nl-NL", "EUR");

    private final String code;
    private final String currency;

    Market(String code, String currency) {
        this.code = code;
        this.currency = currency;
    }

    public static Market of(String code) {
        return Arrays.stream(values())
                .filter(market -> market.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported market: " + code));
    }

    public String code() {
        return code;
    }

    public String currency() {
        return currency;
    }
}
