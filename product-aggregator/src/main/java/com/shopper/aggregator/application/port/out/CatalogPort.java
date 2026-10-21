package com.shopper.aggregator.application.port.out;

import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductDetails;

public interface CatalogPort {

    ProductDetails find(String productId, Market market);
}
