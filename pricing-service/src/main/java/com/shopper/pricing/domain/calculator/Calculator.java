package com.shopper.pricing.domain.calculator;

import com.shopper.pricing.domain.Money;

public sealed interface Calculator permits FixedCalculator, PercentageCalculator {

    Money calculate(Money subtotal);

    String formula();
}