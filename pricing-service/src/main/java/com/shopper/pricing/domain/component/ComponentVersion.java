package com.shopper.pricing.domain.component;

import java.time.Instant;

import com.shopper.pricing.domain.PricingContext;
import com.shopper.pricing.domain.calculator.Calculator;

public record ComponentVersion(Calculator calculator, Validity validity, Applicability applicability,
                               Instant definedAt) {

    boolean appliesTo(PricingContext context) {
        return validity.contains(context.at()) && applicability.appliesTo(context);
    }

    boolean sameConfigurationAs(ComponentVersion other) {
        return calculator.equals(other.calculator)
                && validity.equals(other.validity)
                && applicability.equals(other.applicability);
    }
}
