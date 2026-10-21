package com.shopper.aggregator.application.port.out;

import java.util.Optional;

import com.shopper.aggregator.application.port.out.model.CustomerProfile;
import com.shopper.aggregator.domain.CustomerId;

public interface CustomerPort {

    Optional<CustomerProfile> customer(CustomerId customerId);
}
