package com.shopscope.monolith.pricing;

import java.util.Currency;
import java.util.Objects;

/**
 * Who is asking for a price: the currency and country always, the sales channel and the customer
 * group when known. Price selection (see {@link PriceScope}) picks the most specific price that fits.
 */
public record PriceContext(Currency currency, String country, String channel, String customerGroup) {

    public PriceContext {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(country, "country");
    }

    public static PriceContext of(String currencyCode, String country) {
        return new PriceContext(Currency.getInstance(currencyCode), country, null, null);
    }

    public PriceContext withChannel(String channel) {
        return new PriceContext(currency, country, channel, customerGroup);
    }

    public PriceContext withCustomerGroup(String customerGroup) {
        return new PriceContext(currency, country, channel, customerGroup);
    }
}
