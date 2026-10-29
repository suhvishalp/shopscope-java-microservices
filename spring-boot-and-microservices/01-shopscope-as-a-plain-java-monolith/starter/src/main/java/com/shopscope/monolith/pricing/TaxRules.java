package com.shopscope.monolith.pricing;

/**
 * Tax per country, hard-coded. Changing a rate means a rebuild; adding a country means a code change.
 * Problem 4 of the register. Demo 3 replaces this with profiles and properties.
 */
public final class TaxRules {

    private TaxRules() {
    }

    public static TaxRule forCountry(String country) {
        return switch (country) {
            case "IN" -> new FlatRateTax("GST", 18);
            case "GB" -> new FlatRateTax("VAT", 20);
            case "US" -> new FlatRateTax("Sales tax (at checkout)", 0);
            default -> throw new IllegalArgumentException("No tax rule for country " + country);
        };
    }
}
