package com.shopscope.monolith.pricing;

import com.shopscope.monolith.shared.Money;

import java.util.List;

/** What a basket costs for one shopper, line by line and in total. */
public record Quote(List<PricedLine> lines, Money subtotal, Money discount, Money tax, Money total, List<String> appliedCodes) {

    public Quote {
        lines = List.copyOf(lines);
        appliedCodes = List.copyOf(appliedCodes);
    }
}
