package com.shopper.aggregator.infrastructure.adapter.customer;

import java.util.List;
import java.util.Optional;

import org.springframework.resilience.annotation.Retryable;

import com.shopper.aggregator.application.port.out.CustomerPort;
import com.shopper.aggregator.application.port.out.model.CustomerProfile;
import com.shopper.aggregator.domain.CustomerId;
import com.shopper.aggregator.infrastructure.adapter.FastUpstreamFailure;
import com.shopper.aggregator.infrastructure.adapter.UpstreamClient;

public class CustomerAdapter implements CustomerPort {

    private record Customer(String segment, List<String> preferences) {
    }

    private final UpstreamClient client;

    public CustomerAdapter(UpstreamClient client) {
        this.client = client;
    }

    @Override
    @Retryable(includes = FastUpstreamFailure.class, maxRetries = 1, delay = 0)
    public Optional<CustomerProfile> customer(CustomerId customerId) {
        return client.get(uri -> uri.path("/customers/{id}").build(customerId.value()), Customer.class)
                .map(c -> new CustomerProfile(c.segment(), c.preferences()));
    }
}
