package com.shopper.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
class CatalogData {

    private static final String FALLBACK_LANGUAGE = "en";

    private record Translation(String name, String description) {
    }

    private record Entry(Map<String, Translation> translations, Map<String, String> specs, List<String> images) {
    }

    private final Map<String, Entry> products = Map.of(
            "P-1001", new Entry(
                    Map.of(
                            "en", new Translation("Oil filter", "Spin-on oil filter for tractor engines."),
                            "pl", new Translation("Filtr oleju", "Filtr oleju do silników ciągników."),
                            "de", new Translation("Ölfilter", "Anschraub-Ölfilter für Traktormotoren."),
                            "nl", new Translation("Oliefilter", "Opschroef-oliefilter voor tractormotoren.")),
                    Map.of("thread", "M20x1.5", "height", "142 mm", "diameter", "93 mm"),
                    List.of("https://images.example.com/P-1001/front.jpg")));

    Optional<Product> find(String productId, String locale) {
        Entry entry = products.get(productId);
        if (entry == null) {
            return Optional.empty();
        }
        String language = locale.split("-")[0];
        if (!entry.translations().containsKey(language)) {
            language = FALLBACK_LANGUAGE;
        }
        Translation translation = entry.translations().get(language);
        return Optional.of(new Product(productId, language, translation.name(), translation.description(),
                entry.specs(), entry.images()));
    }
}
