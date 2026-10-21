package com.shopper.pricing.presentation.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import com.shopper.pricing.application.port.in.PricingUseCase.NewVersion;
import com.shopper.pricing.domain.Market;
import com.shopper.pricing.domain.calculator.PercentageCalculator;
import com.shopper.pricing.domain.component.Applicability;
import com.shopper.pricing.domain.component.Validity;

record AddVersionRequest(BigDecimal rate, Instant validFrom, Instant validTo,
                         Set<String> markets, Set<String> products, Set<String> customers) {

    NewVersion toNewVersion() {
        return new NewVersion(
                new PercentageCalculator(rate),
                new Validity(validFrom, validTo),
                new Applicability(
                        orEmpty(markets).stream().map(Market::of).collect(Collectors.toSet()),
                        orEmpty(products),
                        orEmpty(customers)));
    }

    private static Set<String> orEmpty(Set<String> values) {
        return values != null ? values : Set.of();
    }
}
