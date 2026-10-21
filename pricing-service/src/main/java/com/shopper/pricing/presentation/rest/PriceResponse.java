package com.shopper.pricing.presentation.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shopper.pricing.domain.PriceQuote;

record PriceResponse(String productId, String market, String currency, BigDecimal basePrice,
                     BigDecimal discount, BigDecimal netPrice, BigDecimal vat, BigDecimal finalPrice,
                     List<Line> breakdown, Instant pricedAt) {

    record Line(String component, String role, BigDecimal amount, String formula) {
    }

    static PriceResponse from(PriceQuote quote) {
        return new PriceResponse(quote.productId(), quote.market().code(), quote.finalPrice().currency(),
                quote.basePrice().amount(), quote.discount().amount(), quote.netPrice().amount(),
                quote.vat().amount(), quote.finalPrice().amount(),
                quote.lines().stream()
                        .map(line -> new Line(line.componentId(), line.role().name(), line.amount().amount(),
                                line.formula()))
                        .toList(),
                quote.pricedAt());
    }
}
