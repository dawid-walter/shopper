package com.shopper.aggregator.application.port.out;

import java.util.Optional;

import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductId;
import com.shopper.aggregator.domain.Stock;

public interface AvailabilityPort {

    Optional<Stock> stock(ProductId productId, Market market);
}
