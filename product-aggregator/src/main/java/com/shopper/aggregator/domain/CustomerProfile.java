package com.shopper.aggregator.domain;

import java.util.List;

public record CustomerProfile(String segment, List<String> preferences) {
}
