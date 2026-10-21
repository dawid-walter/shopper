package com.shopper.pricing.domain.component;

import java.time.Instant;

public record Validity(Instant from, Instant to) {

    public Validity {
        if (from == null) {
            throw new IllegalArgumentException("Validity needs a start");
        }
        if (to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("Validity start must precede its end");
        }
    }

    public static Validity always() {
        return new Validity(Instant.EPOCH, null);
    }

    public static Validity from(Instant from) {
        return new Validity(from, null);
    }

    public static Validity between(Instant from, Instant to) {
        return new Validity(from, to);
    }

    public boolean contains(Instant moment) {
        return !moment.isBefore(from) && (to == null || moment.isBefore(to));
    }
}
