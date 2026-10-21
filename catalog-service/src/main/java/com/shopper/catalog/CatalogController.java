package com.shopper.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CatalogController {

    private final CatalogData data;

    CatalogController(CatalogData data) {
        this.data = data;
    }

    @GetMapping("/products/{productId}")
    ResponseEntity<Product> product(@PathVariable String productId,
                                    @RequestParam(defaultValue = "en-GB") String locale) {
        return ResponseEntity.of(data.find(productId, locale));
    }
}
