package com.shopper.pricing.domain.calculator;

import com.shopper.pricing.domain.Money;

public record FixedCalculator(Money amount) implements Calculator {

    @Override
    public Money calculate(Money subtotal) {
        return amount;
    }

    @Override
    public String formula() {
        return "f() = " + amount;
    }
}
