package com.shopper.aggregator.application.port.out.model;

import java.util.List;
import java.util.Map;

public record ProductDetails(String name, String description, Map<String, String> specs, List<String> images) {
}
