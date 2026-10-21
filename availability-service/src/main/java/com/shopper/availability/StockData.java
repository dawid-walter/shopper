package com.shopper.availability;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
class StockData {

    private record Key(String productId, String market) {
    }

    private final Map<Key, StockLevel> stock = Map.of(
            new Key("P-1001", "pl-PL"), new StockLevel("P-1001", "pl-PL", 42, "Poznań", 1),
            new Key("P-1001", "de-DE"), new StockLevel("P-1001", "de-DE", 7, "Hamburg", 2),
            new Key("P-1001", "nl-NL"), new StockLevel("P-1001", "nl-NL", 0, "Zwolle", 5));

    Optional<StockLevel> find(String productId, String market) {
        return Optional.ofNullable(stock.get(new Key(productId, market)));
    }
}
