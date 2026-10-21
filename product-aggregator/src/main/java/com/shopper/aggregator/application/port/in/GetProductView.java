package com.shopper.aggregator.application.port.in;

import java.util.Optional;

import com.shopper.aggregator.application.port.in.model.ProductView;
import com.shopper.aggregator.domain.CustomerId;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductId;

public interface GetProductView {

    ProductView get(ProductId productId, Market market, Optional<CustomerId> customerId);
}
