package com.shopper.aggregator.application.port.out.model;

import java.util.List;

public record CustomerProfile(String segment, List<String> preferences) {
}
