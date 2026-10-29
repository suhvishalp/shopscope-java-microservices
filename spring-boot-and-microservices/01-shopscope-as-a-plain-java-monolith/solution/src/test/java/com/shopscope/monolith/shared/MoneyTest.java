package com.shopscope.monolith.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({
            "10.00, 5.50, 15.50",
            "0.10, 0.20, 0.30",
            "999.99, 0.01, 1000.00"
    })
    void plusAddsExactly(String left, String right, String expected) {
        assertThat(Money.of(left, "INR").plus(Money.of(right, "INR"))).isEqualTo(Money.of(expected, "INR"));
    }

    @ParameterizedTest(name = "{1}% of {0} is {2}")
    @CsvSource({
            "200.00, 18, 36.00",
            "349.00, 10, 34.90",
            "0.05, 50, 0.02"
    })
    void percentRoundsHalfEven(String amount, int percent, String expected) {
        assertThat(Money.of(amount, "INR").percent(percent)).isEqualTo(Money.of(expected, "INR"));
    }

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
