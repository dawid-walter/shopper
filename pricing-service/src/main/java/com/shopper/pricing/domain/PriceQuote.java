package com.shopper.pricing.domain;

import static com.shopper.pricing.domain.component.ComponentRole.DISCOUNT;
import static com.shopper.pricing.domain.component.ComponentRole.LIST_PRICE;
import static com.shopper.pricing.domain.component.ComponentRole.TAX;

import java.time.Instant;
import java.util.List;

import com.shopper.pricing.domain.component.ComponentRole;

public record PriceQuote(String productId, Market market, Money basePrice, Money discount, Money netPrice,
                         Money vat, Money finalPrice, List<Line> lines, Instant pricedAt) {

    public record Line(String componentId, ComponentRole role, Money amount, String formula) {
    }

    public static PriceQuote from(PricingContext context, List<Line> lines) {
        if (!hasLine(lines, LIST_PRICE) || !hasLine(lines, TAX)) {
            throw new NotPricedException(context.productId(), context.market());
        }
        String currency = context.market().currency();
        Money base = sum(lines, LIST_PRICE, currency);
        Money discount = sum(lines, DISCOUNT, currency);
        Money vat = sum(lines, TAX, currency);
        Money net = base.plus(discount);
        return new PriceQuote(context.productId(), context.market(), base, discount, net, vat, net.plus(vat),
                List.copyOf(lines), context.at());
    }

    private static boolean hasLine(List<Line> lines, ComponentRole role) {
        return lines.stream().anyMatch(line -> line.role() == role);
    }

    private static Money sum(List<Line> lines, ComponentRole role, String currency) {
        return lines.stream()
                .filter(line -> line.role() == role)
                .map(Line::amount)
                .reduce(Money.zero(currency), Money::plus);
    }
}
