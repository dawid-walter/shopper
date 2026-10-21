package com.shopper.aggregator.application.port.out;

import java.util.Optional;

import com.shopper.aggregator.domain.CustomerId;
import com.shopper.aggregator.domain.CustomerProfile;

public interface CustomerPort {

    Optional<CustomerProfile> customer(CustomerId customerId);
}
