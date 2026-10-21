package com.shopper.aggregator.application.port.in.exception;

public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(Throwable cause) {
        super("Catalog is unavailable", cause);
    }
}
