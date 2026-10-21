package com.shopper.aggregator.application.port.out.exception;

public class UpstreamException extends RuntimeException {

    public enum Failure { ERROR, REJECTED, TIMEOUT, CIRCUIT_OPEN }

    private final Failure failure;
    private final Integer status;

    public UpstreamException(String upstream, Failure failure, Throwable cause) {
        this(upstream, failure, null, cause);
    }

    protected UpstreamException(String upstream, Failure failure, Integer status, Throwable cause) {
        super(upstream + " failed: " + failure + (status == null ? "" : " (status " + status + ")"), cause);
        this.failure = failure;
        this.status = status;
    }

    public Failure failure() {
        return failure;
    }

    public String reason() {
        return status == null ? failure.name() : failure + " (status " + status + ")";
    }
}
