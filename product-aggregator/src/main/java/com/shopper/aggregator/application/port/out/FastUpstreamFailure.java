package com.shopper.aggregator.application.port.out;

public class FastUpstreamFailure extends UpstreamException {

    public FastUpstreamFailure(String upstream, Throwable cause) {
        super(upstream, Failure.ERROR, cause);
    }
}
