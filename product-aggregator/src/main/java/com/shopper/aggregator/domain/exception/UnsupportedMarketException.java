package com.shopper.aggregator.domain.exception;

public class UnsupportedMarketException extends RuntimeException {

    public UnsupportedMarketException(String code) {
        super("Unsupported market: " + code);
    }
}
