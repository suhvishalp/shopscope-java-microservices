package com.shopscope.monolith.shared;

import java.util.Currency;

/** Thrown when two {@link Money} values in different currencies meet in arithmetic. */
public class CurrencyMismatchException extends IllegalArgumentException {

    public CurrencyMismatchException(Currency left, Currency right) {
        super("Cannot combine " + left.getCurrencyCode() + " with " + right.getCurrencyCode());
    }
}
