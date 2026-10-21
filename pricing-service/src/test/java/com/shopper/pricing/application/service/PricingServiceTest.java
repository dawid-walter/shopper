package com.shopper.pricing.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.shopper.pricing.application.port.in.PricingUseCase.NewVersion;
import com.shopper.pricing.domain.Market;
import com.shopper.pricing.domain.PriceQuote;
import com.shopper.pricing.domain.PricingContext;
import com.shopper.pricing.domain.calculator.PercentageCalculator;
import com.shopper.pricing.domain.component.Applicability;
import com.shopper.pricing.domain.component.Validity;
import com.shopper.pricing.infrastructure.persistence.InMemoryComponentRepository;
import com.shopper.pricing.infrastructure.persistence.SeedData;

class PricingServiceTest {

    private static final Instant JUNE = Instant.parse("2026-06-01T10:00:00Z");
    private static final Applicability P_1001_IN_PL =
            new Applicability(Set.of(Market.PL_PL), Set.of("P-1001"), Set.of());

    private PricingService pricing;

    @BeforeEach
    void setUp() {
        var components = new InMemoryComponentRepository();
        SeedData.loadComponents(components);
        pricing = new PricingService(components, SeedData.tariffs(), Clock.fixed(JUNE, ZoneOffset.UTC));
    }

    private PriceQuote quote(Market market, String customerId, String at) {
        return pricing.quote(new PricingContext("P-1001", market, customerId, Instant.parse(at)));
    }

    @Test
    void vatAndCurrencyFollowTheMarket() {
        PriceQuote poland = quote(Market.PL_PL, null, "2026-06-01T10:00:00Z");
        PriceQuote germany = quote(Market.DE_DE, null, "2026-06-01T10:00:00Z");

        assertThat(poland.finalPrice()).hasToString("123.00 PLN");
        assertThat(poland.lines()).extracting(PriceQuote.Line::componentId).containsExactly("list-price-pl", "vat-pl");
        assertThat(germany.finalPrice()).hasToString("29.75 EUR");
    }

    @Test
    void contractDiscountAppliesBeforeVat() {
        PriceQuote quote = quote(Market.PL_PL, "C-42", "2026-06-01T10:00:00Z");

        assertThat(quote.discount()).hasToString("-10.00 PLN");
        assertThat(quote.finalPrice()).hasToString("110.70 PLN");
    }

    @Test
    void flashSaleAppliesOnlyInsideItsWindow() {
        pricing.addVersion("flash-sale-pl", new NewVersion(new PercentageCalculator(new BigDecimal("-20")),
                Validity.between(Instant.parse("2026-06-01T10:00:00Z"), Instant.parse("2026-06-01T12:00:00Z")),
                P_1001_IN_PL));

        assertThat(quote(Market.PL_PL, null, "2026-06-01T11:00:00Z").discount()).hasToString("-20.00 PLN");
        assertThat(quote(Market.PL_PL, null, "2026-06-01T12:00:00Z").discount()).hasToString("0.00 PLN");
    }
}
