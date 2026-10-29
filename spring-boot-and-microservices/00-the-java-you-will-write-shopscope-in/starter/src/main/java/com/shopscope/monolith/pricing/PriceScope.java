package com.shopscope.monolith.pricing;

/**
 * Where a price applies. A sealed hierarchy: these four cases are all the cases, and the compiler
 * knows it. On Java 17 we branch with {@code instanceof} patterns; on Java 21 and later the same
 * code becomes an exhaustive {@code switch} with no {@code default} (see the study note).
 *
 * <p>Precedence, most specific first: customer group, then channel, then country, then base.
 */
public sealed interface PriceScope
        permits PriceScope.Base, PriceScope.ForCountry, PriceScope.ForChannel, PriceScope.ForCustomerGroup {

    /** The fallback price: applies to everyone in this currency. */
    record Base() implements PriceScope {}

    record ForCountry(String country) implements PriceScope {}

    record ForChannel(String channel) implements PriceScope {}

    record ForCustomerGroup(String customerGroup) implements PriceScope {}

    /** Does this scope fit the shopper described by {@code context}? */
    default boolean appliesTo(PriceContext context) {
        // TODO(lab-2.1) Base applies to everyone; the other three apply when their component matches the context.
        // Use instanceof pattern matching: `if (this instanceof ForCountry scope) { ... scope.country() ... }`.
        throw new UnsupportedOperationException("TODO(lab-2.1)");
    }

    /** Higher wins when several prices apply. */
    default int precedence() {
        // TODO(lab-2.2) customer group 3, channel 2, country 1, base 0
        throw new UnsupportedOperationException("TODO(lab-2.2)");
    }
}
