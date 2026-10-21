package com.shopper.pricing.infrastructure.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.shopper.pricing.domain.component.PriceComponent;
import com.shopper.pricing.domain.repository.ComponentRepository;

public class InMemoryComponentRepository implements ComponentRepository {

    private final Map<String, PriceComponent> components = new ConcurrentHashMap<>();

    @Override
    public Optional<PriceComponent> find(String componentId) {
        return Optional.ofNullable(components.get(componentId));
    }

    @Override
    public void save(PriceComponent component) {
        components.put(component.id(), component);
    }
}
