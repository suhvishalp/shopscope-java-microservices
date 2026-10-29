package com.shopscope.monolith.pricing;

import com.shopscope.monolith.SeedData;
import com.shopscope.monolith.order.Basket;
import com.shopscope.monolith.order.LineItem;
import com.shopscope.monolith.shared.Money;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("enable in Lab 4 (needs priceOrThrow)")
class PricingServiceTest {

    private final PricingService pricing = new PricingService(SeedData.prices(), SeedData.discountRules(), new FlatRateTax("GST", 18));
    private final PriceContext india = PriceContext.of("INR", "IN").withChannel("web");

    @Test
    void quotesEveryLineAtItsSelectedPrice() {
        Basket basket = Basket.empty("c-1")
                .add(new LineItem(SeedData.TEA, "Tea", 2))
                .add(new LineItem(SeedData.KETTLE, "Kettle", 1));

        Quote quote = pricing.quote(basket, india);

        assertThat(quote.lines()).extracting(PricedLine::unitPrice)
                .containsExactly(Money.of("349.00", "INR"), Money.of("2299.00", "INR")); // kettle: the IN price
        assertThat(quote.subtotal()).isEqualTo(Money.of("2997.00", "INR"));
    }

    @Test
    void appliesDiscountsThenTaxOnTheNetAmount() {
        Basket basket = Basket.empty("c-1").add(new LineItem(SeedData.TEA, "Tea", 2)); // 698.00, TEA10 takes 69.80

        Quote quote = pricing.quote(basket, india);

        assertThat(quote.discount()).isEqualTo(Money.of("69.80", "INR"));
        assertThat(quote.appliedCodes()).containsExactly("TEA10");
        assertThat(quote.tax()).isEqualTo(Money.of("113.08", "INR")); // 18% of 628.20
        assertThat(quote.total()).isEqualTo(Money.of("741.28", "INR"));
    }

    @Test
    void basketDiscountKicksInOverTheThreshold() {
        Basket basket = Basket.empty("c-1").add(new LineItem(SeedData.KETTLE, "Kettle", 3)); // 6897.00

        Quote quote = pricing.quote(basket, india);

        assertThat(quote.appliedCodes()).containsExactly("BIG-BASKET-5");
        assertThat(quote.discount()).isEqualTo(Money.of("344.85", "INR"));
    }

    @Test
    void wholesaleCustomersGetTheirOwnTeaPrice() {
        Basket basket = Basket.empty("c-2").add(new LineItem(SeedData.TEA, "Tea", 1));

        Quote quote = pricing.quote(basket, india.withCustomerGroup("wholesale"));

        assertThat(quote.lines().get(0).unitPrice()).isEqualTo(Money.of("299.00", "INR"));
    }

    @Test
    void failsLoudlyWhenAProductHasNoPrice() {
        Basket basket = Basket.empty("c-1").add(new LineItem("SKU-NOPE", "Mystery", 1));

        assertThatThrownBy(() -> pricing.quote(basket, india)).isInstanceOf(NoPriceException.class);
    }
}
