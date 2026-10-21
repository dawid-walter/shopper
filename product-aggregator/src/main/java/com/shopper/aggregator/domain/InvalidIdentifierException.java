package com.shopper.aggregator.domain;

public class InvalidIdentifierException extends RuntimeException {

    public InvalidIdentifierException(String type) {
        super(type + " must not be blank");
    }
}
