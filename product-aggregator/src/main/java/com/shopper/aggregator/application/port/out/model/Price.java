package com.shopper.aggregator.application.port.out.model;

import java.math.BigDecimal;

public record Price(String currency, BigDecimal basePrice, BigDecimal discount, BigDecimal netPrice,
                    BigDecimal vat, BigDecimal finalPrice) {
}
