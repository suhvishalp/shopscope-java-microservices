package com.shopscope.monolith.catalog;

/** The caller asked for a SKU the catalogue does not have. Unchecked: the caller cannot fix it by retrying. */
public class ProductNotFoundException extends RuntimeException {

    private final String sku;

    public ProductNotFoundException(String sku) {
        super("No product with SKU " + sku);
        this.sku = sku;
    }

    public String sku() {
        return sku;
    }
}
