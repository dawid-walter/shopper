package com.shopper.aggregator.infrastructure.adapter;

import com.shopper.aggregator.application.port.out.exception.UpstreamException;

public class UpstreamRejectedRequest extends UpstreamException {

    public UpstreamRejectedRequest(String upstream, int status) {
        super(upstream, Failure.REJECTED, status, null);
    }
}
