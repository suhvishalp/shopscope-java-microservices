package com.shopscope.monolith.order;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What a shopper intends to buy. Immutable: {@link #add} returns a new basket, which is what makes it
 * safe to hand around, cache and quote concurrently (Demo 0, Lab 7).
 */
public record Basket(String customerId, List<LineItem> lines) {

    public Basket {
        Objects.requireNonNull(customerId, "customerId");
        lines = List.copyOf(lines);
    }

    public static Basket empty(String customerId) {
        return new Basket(customerId, List.of());
    }

    public Basket add(LineItem line) {
        List<LineItem> merged = new ArrayList<>(lines);
        int existing = indexOf(line.sku());
        if (existing >= 0) {
            LineItem current = merged.get(existing);
            merged.set(existing, new LineItem(current.sku(), current.name(), current.quantity() + line.quantity()));
        } else {
            merged.add(line);
        }
        return new Basket(customerId, merged);
    }

    public int totalQuantity() {
        return lines.stream().mapToInt(LineItem::quantity).sum();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    private int indexOf(String sku) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).sku().equals(sku)) {
                return i;
            }
        }
        return -1;
    }
}
