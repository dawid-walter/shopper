package com.shopper.pricing.infrastructure.persistence;

import static com.shopper.pricing.domain.Market.DE_DE;
import static com.shopper.pricing.domain.Market.NL_NL;
import static com.shopper.pricing.domain.Market.PL_PL;
import static com.shopper.pricing.domain.component.ComponentRole.DISCOUNT;
import static com.shopper.pricing.domain.component.ComponentRole.LIST_PRICE;
import static com.shopper.pricing.domain.component.ComponentRole.TAX;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.shopper.pricing.domain.Market;
import com.shopper.pricing.domain.Money;
import com.shopper.pricing.domain.calculator.Calculator;
import com.shopper.pricing.domain.calculator.FixedCalculator;
import com.shopper.pricing.domain.calculator.PercentageCalculator;
import com.shopper.pricing.domain.component.Applicability;
import com.shopper.pricing.domain.component.ComponentRole;
import com.shopper.pricing.domain.component.ComponentVersion;
import com.shopper.pricing.domain.component.PriceComponent;
import com.shopper.pricing.domain.component.Tariff;
import com.shopper.pricing.domain.component.Validity;
import com.shopper.pricing.domain.repository.ComponentRepository;

public final class SeedData {

    private static final Instant SEEDED_AT = Instant.parse("2026-01-01T00:00:00Z");

    private SeedData() {
    }

    public static void loadComponents(ComponentRepository components) {
        components.save(component("list-price-pl", "List price PL", LIST_PRICE,
                new FixedCalculator(Money.of("100.00", "PLN")), Validity.always(),
                new Applicability(Set.of(PL_PL), Set.of("P-1001"), Set.of())));
        components.save(component("list-price-de", "List price DE", LIST_PRICE,
                new FixedCalculator(Money.of("25.00", "EUR")), Validity.always(),
                new Applicability(Set.of(DE_DE), Set.of("P-1001"), Set.of())));
        components.save(component("list-price-nl", "List price NL", LIST_PRICE,
                new FixedCalculator(Money.of("26.00", "EUR")), Validity.always(),
                new Applicability(Set.of(NL_NL), Set.of("P-1001"), Set.of())));
        components.save(component("flash-sale-pl", "Black Friday PL", DISCOUNT,
                percent("-20"),
                Validity.between(Instant.parse("2026-11-27T00:00:00Z"), Instant.parse("2026-11-30T00:00:00Z")),
                new Applicability(Set.of(PL_PL), Set.of("P-1001"), Set.of())));
        components.save(component("contract-discount", "Customer contract discount", DISCOUNT,
                percent("-10"), Validity.always(),
                new Applicability(Set.of(), Set.of(), Set.of("C-42"))));
        components.save(vat("vat-pl", PL_PL, "23"));
        components.save(vat("vat-de", DE_DE, "19"));
        components.save(vat("vat-nl", NL_NL, "21"));
    }

    public static Map<Market, Tariff> tariffs() {
        return Map.of(
                PL_PL, new Tariff(PL_PL, List.of("list-price-pl", "flash-sale-pl", "contract-discount", "vat-pl")),
                DE_DE, new Tariff(DE_DE, List.of("list-price-de", "contract-discount", "vat-de")),
                NL_NL, new Tariff(NL_NL, List.of("list-price-nl", "contract-discount", "vat-nl")));
    }

    private static PriceComponent vat(String id, Market market, String rate) {
        return component(id, "VAT " + market.code(), TAX, percent(rate), Validity.always(),
                new Applicability(Set.of(market), Set.of(), Set.of()));
    }

    private static PercentageCalculator percent(String rate) {
        return new PercentageCalculator(new BigDecimal(rate));
    }

    private static PriceComponent component(String id, String name, ComponentRole role, Calculator calculator,
                                            Validity validity, Applicability applicability) {
        return PriceComponent.create(id, name, role,
                new ComponentVersion(calculator, validity, applicability, SEEDED_AT));
    }
}
