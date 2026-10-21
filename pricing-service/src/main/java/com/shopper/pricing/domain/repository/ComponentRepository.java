package com.shopper.pricing.domain.repository;

import java.util.Optional;

import com.shopper.pricing.domain.component.PriceComponent;

public interface ComponentRepository {

    Optional<PriceComponent> find(String componentId);

    void save(PriceComponent component);
}
