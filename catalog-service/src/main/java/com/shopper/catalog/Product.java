package com.shopper.catalog;

import java.util.List;
import java.util.Map;

public record Product(String id, String language, String name, String description,
                      Map<String, String> specs, List<String> images) {
}
