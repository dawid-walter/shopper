package com.shopper.pricing.application.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.shopper.pricing.application.port.in.PricingUseCase;
import com.shopper.pricing.domain.Market;
import com.shopper.pricing.domain.Money;
import com.shopper.pricing.domain.NotPricedException;
import com.shopper.pricing.domain.PriceQuote;
import com.shopper.pricing.domain.PricingContext;
import com.shopper.pricing.domain.component.ComponentVersion;
import com.shopper.pricing.domain.component.PriceComponent;
import com.shopper.pricing.domain.component.Tariff;
import com.shopper.pricing.domain.repository.ComponentRepository;

public class PricingService implements PricingUseCase {

    private final ComponentRepository components;
    private final Map<Market, Tariff> tariffs;
    private final Clock clock;

    public PricingService(ComponentRepository components, Map<Market, Tariff> tariffs, Clock clock) {
        this.components = components;
        this.tariffs = tariffs;
        this.clock = clock;
    }

    @Override
    public PriceQuote quote(PricingContext context) {
        Tariff tariff = tariffs.get(context.market());
        if (tariff == null) {
            throw new NotPricedException(context.productId(), context.market());
        }
        Money subtotal = Money.zero(context.market().currency());
        List<PriceQuote.Line> lines = new ArrayList<>();
        for (String componentId : tariff.componentIds()) {
            PriceComponent component = find(componentId);
            var version = component.versionFor(context);
            if (version.isPresent()) {
                var calculator = version.get().calculator();
                Money amount = calculator.calculate(subtotal);
                lines.add(new PriceQuote.Line(component.id(), component.role(), amount, calculator.formula()));
                subtotal = subtotal.plus(amount);
            }
        }
        return PriceQuote.from(context, lines);
    }

    @Override
    public PriceComponent addVersion(String componentId, NewVersion newVersion) {
        var version = new ComponentVersion(newVersion.calculator(), newVersion.validity(),
                newVersion.applicability(), clock.instant());
        PriceComponent updated = find(componentId).withVersion(version);
        components.save(updated);
        return updated;
    }

    private PriceComponent find(String componentId) {
        return components.find(componentId)
                .orElseThrow(() -> new NoSuchElementException("No price component " + componentId));
    }
}
