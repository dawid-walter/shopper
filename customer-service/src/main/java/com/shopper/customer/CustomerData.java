package com.shopper.customer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
class CustomerData {

    private final Map<String, Customer> customers = Map.of(
            "C-42", new Customer("C-42", "DEALER", List.of("fast-delivery", "invoice-monthly")),
            "C-7", new Customer("C-7", "WORKSHOP", List.of()));

    Optional<Customer> find(String customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }
}
