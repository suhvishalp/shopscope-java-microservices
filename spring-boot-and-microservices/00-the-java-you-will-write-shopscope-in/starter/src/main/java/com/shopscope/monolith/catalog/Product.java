package com.shopscope.monolith.catalog;

import java.util.Objects;

/** What the catalogue knows about a product. Prices live in the pricing module, stock in inventory. */
public record Product(String sku, String name, String category, String description) {

    public Product {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        description = description == null ? "" : description;
    }
}
