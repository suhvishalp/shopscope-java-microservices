package com.shopscope.monolith.pricing;

import com.shopscope.monolith.shared.Money;

import java.util.Objects;

/** One price for one SKU in one scope. A price list holds many of these per SKU. */
public record Price(String sku, Money value, PriceScope scope) {

    public Price {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(scope, "scope");
        if (value.isNegative()) {
            throw new IllegalArgumentException("A price cannot be negative: " + value);
        }
    }

    public static Price base(String sku, Money value) {
        return new Price(sku, value, new PriceScope.Base());
    }

    public static Price forCountry(String sku, Money value, String country) {
        return new Price(sku, value, new PriceScope.ForCountry(country));
    }

    public static Price forChannel(String sku, Money value, String channel) {
        return new Price(sku, value, new PriceScope.ForChannel(channel));
    }

    public static Price forCustomerGroup(String sku, Money value, String customerGroup) {
        return new Price(sku, value, new PriceScope.ForCustomerGroup(customerGroup));
    }
}
