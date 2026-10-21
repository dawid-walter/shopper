package com.shopper.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CustomerController {

    private final CustomerData data;

    CustomerController(CustomerData data) {
        this.data = data;
    }

    @GetMapping("/customers/{customerId}")
    ResponseEntity<Customer> customer(@PathVariable String customerId) {
        return ResponseEntity.of(data.find(customerId));
    }
}
