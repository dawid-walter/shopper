package com.shopper.pricing.domain.component;

import java.util.List;

import com.shopper.pricing.domain.Market;

public record Tariff(Market market, List<String> componentIds) {

    public Tariff {
        componentIds = List.copyOf(componentIds);
    }
}
