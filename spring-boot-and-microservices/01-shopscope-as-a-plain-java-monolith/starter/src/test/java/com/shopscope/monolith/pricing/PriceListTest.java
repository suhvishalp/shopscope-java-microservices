package com.shopscope.monolith.pricing;

import com.shopscope.monolith.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceListTest {

    private static final String KETTLE = "SKU-KET-001";

    private final PriceList prices = new PriceList(List.of(
            Price.base(KETTLE, Money.of("2499.00", "INR")),
            Price.forCountry(KETTLE, Money.of("2299.00", "INR"), "IN"),
            Price.forChannel(KETTLE, Money.of("2199.00", "INR"), "app"),
            Price.forCustomerGroup(KETTLE, Money.of("1999.00", "INR"), "wholesale"),
            Price.base(KETTLE, Money.of("29.00", "GBP"))));

    @Test
    void basePriceWhenNothingMoreSpecificApplies() {
        assertThat(prices.priceOrThrow(KETTLE, PriceContext.of("INR", "US"))).isEqualTo(Money.of("2499.00", "INR"));
    }

    @Test
    void countryBeatsBase() {
        assertThat(prices.priceOrThrow(KETTLE, PriceContext.of("INR", "IN"))).isEqualTo(Money.of("2299.00", "INR"));
    }

    @Test
    void channelBeatsCountry() {
        PriceContext context = PriceContext.of("INR", "IN").withChannel("app");
        assertThat(prices.priceOrThrow(KETTLE, context)).isEqualTo(Money.of("2199.00", "INR"));
    }

    @Test
    void customerGroupBeatsEverything() {
        PriceContext context = PriceContext.of("INR", "IN").withChannel("app").withCustomerGroup("wholesale");
        assertThat(prices.priceOrThrow(KETTLE, context)).isEqualTo(Money.of("1999.00", "INR"));
    }

    @Test
    void onlyPricesInTheShoppersCurrencyCount() {
        assertThat(prices.priceOrThrow(KETTLE, PriceContext.of("GBP", "GB"))).isEqualTo(Money.of("29.00", "GBP"));
    }

    @Test
    void findIsEmptyForAnUnknownSkuOrCurrency() {
        assertThat(prices.find("SKU-NOPE", PriceContext.of("INR", "IN"))).isEmpty();
        assertThat(prices.find(KETTLE, PriceContext.of("USD", "US"))).isEmpty();
    }

    @Test
    void priceOrThrowNamesTheSkuAndContext() {
        assertThatThrownBy(() -> prices.priceOrThrow(KETTLE, PriceContext.of("USD", "US")))
                .isInstanceOf(NoPriceException.class)
                .hasMessageContaining(KETTLE)
                .hasMessageContaining("USD/US");
    }
}
