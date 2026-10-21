package com.shopper.pricing.domain.component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.shopper.pricing.domain.PricingContext;

public record PriceComponent(String id, String name, ComponentRole role, List<ComponentVersion> versions) {

    private static final Comparator<ComponentVersion> LATEST_START_WINS =
            Comparator.comparing((ComponentVersion v) -> v.validity().from())
                    .thenComparing(ComponentVersion::definedAt);

    public PriceComponent {
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("Component " + id + " needs at least one version");
        }
        versions = List.copyOf(versions);
    }

    public static PriceComponent create(String id, String name, ComponentRole role, ComponentVersion first) {
        return new PriceComponent(id, name, role, List.of(first));
    }

    public Optional<ComponentVersion> versionFor(PricingContext context) {
        return versions.stream()
                .filter(v -> v.appliesTo(context))
                .max(LATEST_START_WINS);
    }

    public PriceComponent withVersion(ComponentVersion newVersion) {
        if (versions.stream().anyMatch(v -> v.sameConfigurationAs(newVersion))) {
            throw new IllegalArgumentException("Component " + id + " already has this version");
        }
        List<ComponentVersion> next = new ArrayList<>(versions);
        next.add(newVersion);
        return new PriceComponent(id, name, role, next);
    }
}
