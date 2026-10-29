package com.shopscope.monolith.shared;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("enable in Lab 1")
class MoneyTest {

    @Test
    void twoMoneysWithTheSameValueAreEqual() {
        assertThat(Money.of("10", "INR")).isEqualTo(Money.of("10.00", "INR"));
        assertThat(Money.of("10", "INR")).hasSameHashCodeAs(Money.of("10.00", "INR"));
    }

    @Test
    void scaleFollowsTheCurrency() {
        assertThat(Money.of("10", "INR").amount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(Money.of("10", "JPY").amount()).isEqualTo(new BigDecimal("10"));
    }

    @Test
    void plusAddsExactly() {
        assertThat(Money.of("10.00", "INR").plus(Money.of("5.50", "INR"))).isEqualTo(Money.of("15.50", "INR"));
    }

    // TODO(lab-6.1) turn plusAddsExactly into a @ParameterizedTest with a @CsvSource of three rows
    // TODO(lab-6.2) add percentRoundsHalfEven as a @ParameterizedTest: 200.00/18 -> 36.00, 349.00/10 -> 34.90, 0.05/50 -> 0.02

    @Test
    void timesMultipliesByAQuantity() {
        assertThat(Money.of("349.00", "INR").times(3)).isEqualTo(Money.of("1047.00", "INR"));
    }

    @Test
    void refusesToMixCurrencies() {
        assertThatThrownBy(() -> Money.of("1", "INR").plus(Money.of("1", "GBP")))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("INR")
                .hasMessageContaining("GBP");
    }

    @Test
    void refusesNulls() {
        assertThatThrownBy(() -> new Money(null, Currency.getInstance("INR"))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void printsCurrencyThenAmount() {
        assertThat(Money.of("2499", "INR")).hasToString("INR 2499.00");
    }
}
