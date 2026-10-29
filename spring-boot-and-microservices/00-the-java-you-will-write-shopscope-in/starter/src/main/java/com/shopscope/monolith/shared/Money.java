package com.shopscope.monolith.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * An amount in one currency. A value object: immutable, compared by value, and impossible to
 * create in an invalid state (no null currency, no amount with more decimals than the currency has).
 *
 * <p>Every arithmetic method returns a new {@code Money}; nothing here ever changes.
 * Mixing currencies is a bug, so {@link #plus} and friends throw {@link CurrencyMismatchException}.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        // TODO(lab-1.1) refuse nulls, and normalise the scale to the currency's fraction digits (HALF_EVEN),
        // so that INR 10 and INR 10.00 are the same value. Records compare components, so scale matters.
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        // TODO(lab-1.2) same currency only; return a NEW Money, never mutate
        throw new UnsupportedOperationException("TODO(lab-1.2)");
    }

    public Money minus(Money other) {
        throw new UnsupportedOperationException("TODO(lab-1.2)");
    }

    public Money times(int quantity) {
        throw new UnsupportedOperationException("TODO(lab-1.2)");
    }

    /** {@code percent} of this amount, e.g. {@code INR 200.00.percent(18)} is {@code INR 36.00}. */
    public Money percent(int percent) {
        // TODO(lab-1.3) multiply, divide by 100, round HALF_EVEN to the currency's fraction digits
        throw new UnsupportedOperationException("TODO(lab-1.3)");
    }

    public Money min(Money other) {
        requireSameCurrency(other);
        return compareTo(other) <= 0 ? this : other;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }
}
