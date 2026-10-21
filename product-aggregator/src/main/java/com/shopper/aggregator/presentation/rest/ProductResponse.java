package com.shopper.aggregator.presentation.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopper.aggregator.domain.ProductView;

record ProductResponse(String productId, String market, Product product, PriceView price, StockView stock,
                       CustomerView customer) {

    record Product(String name, String description, Map<String, String> specs, List<String> images) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PriceView(String status, String currency, BigDecimal basePrice, BigDecimal discount,
                     BigDecimal netPrice, BigDecimal vat, BigDecimal finalPrice) {

        static final PriceView UNAVAILABLE = new PriceView("UNAVAILABLE", null, null, null, null, null, null);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record StockView(String status, Integer quantity, String warehouse, Integer deliveryInDays) {

        static final StockView UNKNOWN = new StockView("UNKNOWN", null, null, null);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CustomerView(String status, String segment, List<String> preferences) {

        static final CustomerView STANDARD = new CustomerView("STANDARD", null, null);
    }

    static ProductResponse from(ProductView view) {
        var details = view.product();
        return new ProductResponse(view.productId(), view.market().code(),
                new Product(details.name(), details.description(), details.specs(), details.images()),
                view.price()
                        .map(p -> new PriceView("AVAILABLE", p.currency(), p.basePrice(), p.discount(), p.netPrice(),
                                p.vat(), p.finalPrice()))
                        .orElse(PriceView.UNAVAILABLE),
                view.stock()
                        .map(s -> new StockView("KNOWN", s.quantity(), s.warehouse(), s.deliveryInDays()))
                        .orElse(StockView.UNKNOWN),
                view.customer()
                        .map(c -> new CustomerView("PERSONALIZED", c.segment(), c.preferences()))
                        .orElse(CustomerView.STANDARD));
    }
}
