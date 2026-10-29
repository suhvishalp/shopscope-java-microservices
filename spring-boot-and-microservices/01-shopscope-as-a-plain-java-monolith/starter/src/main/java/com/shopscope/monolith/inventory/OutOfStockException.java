package com.shopscope.monolith.inventory;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String sku, int requested, int available) {
        super("Only " + available + " of " + sku + " in stock, " + requested + " requested");
    }
}
