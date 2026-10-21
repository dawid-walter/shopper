package com.shopper.aggregator.infrastructure.adapter;

import com.shopper.aggregator.application.port.out.exception.UpstreamException;

public class FastUpstreamFailure extends UpstreamException {

    public FastUpstreamFailure(String upstream, Throwable cause) {
        super(upstream, Failure.ERROR, cause);
    }

    public FastUpstreamFailure(String upstream, int status) {
        super(upstream, Failure.ERROR, status, null);
    }
}
