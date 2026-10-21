package com.shopper.aggregator.application.port.out;

import java.util.Optional;

import com.shopper.aggregator.application.port.out.model.Price;
import com.shopper.aggregator.domain.CustomerId;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductId;

public interface PricingPort {

    Optional<Price> price(ProductId productId, Market market, Optional<CustomerId> customerId);
}
