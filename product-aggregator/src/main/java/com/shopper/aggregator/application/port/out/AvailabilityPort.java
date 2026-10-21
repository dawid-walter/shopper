package com.shopper.aggregator.application.port.out;

import java.util.Optional;

import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.Stock;

public interface AvailabilityPort {

    Optional<Stock> stock(String productId, Market market);
}
