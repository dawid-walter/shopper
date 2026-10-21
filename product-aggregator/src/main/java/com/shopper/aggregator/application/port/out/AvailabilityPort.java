package com.shopper.aggregator.application.port.out;

import java.util.Optional;

import com.shopper.aggregator.application.port.out.model.Stock;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductId;

public interface AvailabilityPort {

    Optional<Stock> stock(ProductId productId, Market market);
}
