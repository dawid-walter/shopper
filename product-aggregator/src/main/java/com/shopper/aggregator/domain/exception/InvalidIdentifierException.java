package com.shopper.aggregator.domain.exception;

public class InvalidIdentifierException extends RuntimeException {

    public InvalidIdentifierException(String type) {
        super(type + " must not be blank");
    }
}
