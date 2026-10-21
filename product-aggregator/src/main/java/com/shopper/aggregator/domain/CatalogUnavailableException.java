package com.shopper.aggregator.domain;

public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(Throwable cause) {
        super("Catalog is unavailable", cause);
    }
}
