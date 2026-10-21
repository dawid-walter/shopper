package com.shopper.aggregator.application.port.out;

import java.util.Optional;

import com.shopper.aggregator.application.port.out.model.ProductDetails;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductId;

public interface CatalogPort {

    Optional<ProductDetails> find(ProductId productId, Market market);
}
