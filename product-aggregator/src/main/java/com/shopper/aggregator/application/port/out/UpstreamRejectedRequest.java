package com.shopper.aggregator.application.port.out;

public class UpstreamRejectedRequest extends UpstreamException {

    public UpstreamRejectedRequest(String upstream, int status) {
        super(upstream, Failure.REJECTED, status, null);
    }
}
