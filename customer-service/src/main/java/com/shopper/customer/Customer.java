package com.shopper.customer;

import java.util.List;

public record Customer(String id, String segment, List<String> preferences) {
}
