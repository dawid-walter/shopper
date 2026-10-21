package com.shopper.aggregator.domain;

public class UnsupportedMarketException extends RuntimeException {

    public UnsupportedMarketException(String code) {
        super("Unsupported market: " + code);
    }
}
