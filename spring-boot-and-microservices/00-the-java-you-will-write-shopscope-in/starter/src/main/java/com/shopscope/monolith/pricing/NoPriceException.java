package com.shopscope.monolith.pricing;

/** No price fits this SKU and context. A catalogue that lists a product without a price is a data bug. */
public class NoPriceException extends RuntimeException {

    public NoPriceException(String sku, PriceContext context) {
        super("No price for SKU " + sku + " in " + context.currency().getCurrencyCode() + "/" + context.country());
    }
}
