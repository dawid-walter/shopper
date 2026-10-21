package com.shopper.aggregator.application.port.out;

import java.util.Optional;

import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.Price;

public interface PricingPort {

    Optional<Price> price(String productId, Market market, String customerId);
}
