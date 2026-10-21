package com.shopper.aggregator.application.port.in.model;

import java.util.Optional;

import com.shopper.aggregator.application.port.out.model.CustomerProfile;
import com.shopper.aggregator.application.port.out.model.Price;
import com.shopper.aggregator.application.port.out.model.ProductDetails;
import com.shopper.aggregator.application.port.out.model.Stock;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductId;

public record ProductView(ProductId productId, Market market, ProductDetails product, Optional<Price> price,
                          Optional<Stock> stock, Optional<CustomerProfile> customer) {
}
