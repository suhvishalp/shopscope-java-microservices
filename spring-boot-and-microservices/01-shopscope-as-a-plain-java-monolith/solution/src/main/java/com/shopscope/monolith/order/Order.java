package com.shopscope.monolith.order;

import com.shopscope.monolith.pricing.PricedLine;
import com.shopscope.monolith.shared.Money;

import java.time.Instant;
import java.util.List;

/** The immutable record of a checkout that succeeded. */
public record Order(String id, String customerId, List<PricedLine> lines, Money total, String paymentId, Instant placedAt) {

    public Order {
        lines = List.copyOf(lines);
    }
}
