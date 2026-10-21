package com.shopper.aggregator.infrastructure.adapter.catalog;

import java.util.List;
import java.util.Map;

import org.springframework.resilience.annotation.Retryable;

import com.shopper.aggregator.application.port.out.CatalogPort;
import com.shopper.aggregator.application.port.out.FastUpstreamFailure;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductDetails;
import com.shopper.aggregator.domain.ProductNotFoundException;
import com.shopper.aggregator.infrastructure.adapter.UpstreamClient;

public class CatalogRestClient implements CatalogPort {

    private record CatalogProduct(String name, String description, Map<String, String> specs, List<String> images) {
    }

    private final UpstreamClient client;

    public CatalogRestClient(UpstreamClient client) {
        this.client = client;
    }

    @Override
    @Retryable(includes = FastUpstreamFailure.class, maxRetries = 1, delay = 0)
    public ProductDetails find(String productId, Market market) {
        return client.get(uri -> uri.path("/products/{id}").queryParam("locale", market.code()).build(productId),
                        CatalogProduct.class)
                .map(p -> new ProductDetails(p.name(), p.description(), p.specs(), p.images()))
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
