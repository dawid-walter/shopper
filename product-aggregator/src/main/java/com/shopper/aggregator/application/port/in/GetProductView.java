package com.shopper.aggregator.application.port.in;

import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductView;

public interface GetProductView {

    ProductView get(String productId, Market market, String customerId);
}
