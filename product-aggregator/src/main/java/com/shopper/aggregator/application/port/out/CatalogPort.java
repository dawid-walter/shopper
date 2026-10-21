package com.shopper.aggregator.application.port.out;

import java.util.Optional;

import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductId;
import com.shopper.aggregator.domain.ProductDetails;

public interface CatalogPort {

    Optional<ProductDetails> find(ProductId productId, Market market);
}
