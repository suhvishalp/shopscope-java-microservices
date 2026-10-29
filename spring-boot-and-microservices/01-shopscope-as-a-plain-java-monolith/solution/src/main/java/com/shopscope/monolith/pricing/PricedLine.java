package com.shopscope.monolith.pricing;

import com.shopscope.monolith.shared.Money;

/** A basket line with the price that was selected for it. */
public record PricedLine(String sku, String name, int quantity, Money unitPrice) {

    public Money lineTotal() {
        return unitPrice.times(quantity);
    }
}
