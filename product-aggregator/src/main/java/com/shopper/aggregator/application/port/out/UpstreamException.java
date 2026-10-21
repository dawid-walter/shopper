package com.shopper.aggregator.application.port.out;

public class UpstreamException extends RuntimeException {

    public enum Failure { ERROR, TIMEOUT, CIRCUIT_OPEN }

    private final Failure failure;

    public UpstreamException(String upstream, Failure failure, Throwable cause) {
        super(upstream + " failed: " + failure, cause);
        this.failure = failure;
    }

    public Failure failure() {
        return failure;
    }
}
