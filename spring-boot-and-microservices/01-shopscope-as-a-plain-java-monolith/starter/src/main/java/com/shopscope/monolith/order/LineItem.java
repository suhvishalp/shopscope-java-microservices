package com.shopscope.monolith.order;

import java.util.Objects;

/** One line of a basket: a SKU and how many. The name is a snapshot, so a renamed product does not rewrite old baskets. */
public record LineItem(String sku, String name, int quantity) {

    public LineItem {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(name, "name");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive, was " + quantity);
        }
    }
}
