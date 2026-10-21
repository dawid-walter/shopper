package com.shopper.pricing.application.port.in;

import com.shopper.pricing.domain.PriceQuote;
import com.shopper.pricing.domain.PricingContext;
import com.shopper.pricing.domain.calculator.Calculator;
import com.shopper.pricing.domain.component.Applicability;
import com.shopper.pricing.domain.component.PriceComponent;
import com.shopper.pricing.domain.component.Validity;

public interface PricingUseCase {

    PriceQuote quote(PricingContext context);

    PriceComponent addVersion(String componentId, NewVersion newVersion);

    record NewVersion(Calculator calculator, Validity validity, Applicability applicability) {
    }
}
