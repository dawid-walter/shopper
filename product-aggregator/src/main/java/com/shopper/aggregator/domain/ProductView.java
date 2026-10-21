package com.shopper.aggregator.domain;

import java.util.Optional;

public record ProductView(String productId, Market market, ProductDetails product, Optional<Price> price,
                          Optional<Stock> stock, Optional<CustomerProfile> customer) {
}
