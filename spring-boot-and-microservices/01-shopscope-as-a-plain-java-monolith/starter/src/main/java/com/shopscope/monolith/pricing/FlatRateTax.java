package com.shopscope.monolith.pricing;

import com.shopscope.monolith.shared.Money;

/** A single percentage, e.g. GST 18% in India, VAT 20% in the UK. */
public record FlatRateTax(String name, int percent) implements TaxRule {

    @Override
    public Money taxOn(Money net) {
        return net.percent(percent);
    }
}
