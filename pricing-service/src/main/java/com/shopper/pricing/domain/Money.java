package com.shopper.pricing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount, String currency) {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public Money {
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalStateException("Cannot add " + other.currency + " to " + currency);
        }
        return new Money(amount.add(other.amount), currency);
    }

    public Money percent(BigDecimal rate) {
        return new Money(amount.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
