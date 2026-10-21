package com.shopper.pricing.domain.calculator;

import java.math.BigDecimal;

import com.shopper.pricing.domain.Money;

public record PercentageCalculator(BigDecimal rate) implements Calculator {

    @Override
    public Money calculate(Money subtotal) {
        return subtotal.percent(rate);
    }

    @Override
    public String formula() {
        return "f(subtotal) = subtotal × " + rate.stripTrailingZeros().toPlainString() + "%";
    }
}
