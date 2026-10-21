package com.shopper.aggregator.presentation.rest;

import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopper.aggregator.application.port.in.GetProductView;
import com.shopper.aggregator.domain.CustomerId;
import com.shopper.aggregator.domain.Market;
import com.shopper.aggregator.domain.ProductId;
import com.shopper.aggregator.domain.ProductView;

import io.micrometer.core.instrument.MeterRegistry;

@RestController
class ProductController {

    private final GetProductView getProductView;
    private final MeterRegistry meters;

    ProductController(GetProductView getProductView, MeterRegistry meters) {
        this.getProductView = getProductView;
        this.meters = meters;
    }

    @GetMapping("/products/{productId}")
    ProductResponse product(@PathVariable String productId,
                            @RequestParam String market,
                            @RequestParam(required = false) String customerId) {
        var customer = Optional.ofNullable(customerId).map(CustomerId::new);
        ProductView view = getProductView.get(new ProductId(productId), new Market(market), customer);
        countDegradedSections(view, customer.isPresent());
        return ProductResponse.from(view);
    }

    private void countDegradedSections(ProductView view, boolean customerRequested) {
        if (view.price().isEmpty()) {
            degraded("price");
        }
        if (view.stock().isEmpty()) {
            degraded("stock");
        }
        if (customerRequested && view.customer().isEmpty()) {
            degraded("customer");
        }
    }

    private void degraded(String section) {
        meters.counter("aggregator.sections.degraded", "section", section).increment();
    }
}
