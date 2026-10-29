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
        if (this instanceof ForCountry scope) {
            return scope.country().equals(context.country());
        }
        if (this instanceof ForChannel scope) {
            return scope.channel().equals(context.channel());
        }
        if (this instanceof ForCustomerGroup scope) {
            return scope.customerGroup().equals(context.customerGroup());
        }
        return true; // Base
    }

    /** Higher wins when several prices apply. */
    default int precedence() {
        if (this instanceof ForCustomerGroup) {
            return 3;
        }
        if (this instanceof ForChannel) {
            return 2;
        }
        if (this instanceof ForCountry) {
            return 1;
        }
        return 0; // Base
    }
}
