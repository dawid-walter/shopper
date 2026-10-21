package com.shopper.availability;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StockController {

    private final StockData data;

    StockController(StockData data) {
        this.data = data;
    }

    @GetMapping("/stock/{productId}")
    ResponseEntity<StockLevel> stock(@PathVariable String productId, @RequestParam String market) {
        return ResponseEntity.of(data.find(productId, market));
    }
}
