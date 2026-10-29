package com.shopscope.monolith.concurrent;

import com.shopscope.monolith.SeedData;
import com.shopscope.monolith.order.Basket;
import com.shopscope.monolith.order.LineItem;
import com.shopscope.monolith.pricing.FlatRateTax;
import com.shopscope.monolith.pricing.PriceContext;
import com.shopscope.monolith.pricing.PricingService;
import com.shopscope.monolith.pricing.Quote;
import com.shopscope.monolith.shared.Money;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("enable in Lab 7")
class QuoteBatchTest {

    @Test
    void quotesAThousandBasketsAndKeepsTheirOrder() {
        PricingService pricing = new PricingService(SeedData.prices(), List.of(), new FlatRateTax("GST", 18));
        List<Basket> baskets = IntStream.rangeClosed(1, 1000)
                .mapToObj(i -> Basket.empty("c-" + i).add(new LineItem(SeedData.BISCUITS, "Biscuits", i)))
                .toList();

        List<Quote> quotes = QuoteBatch.quoteAll(pricing, baskets, PriceContext.of("INR", "IN"));

        assertThat(quotes).hasSize(1000);
        assertThat(quotes.get(0).subtotal()).isEqualTo(Money.of("129.00", "INR"));
        assertThat(quotes.get(999).subtotal()).isEqualTo(Money.of("129000.00", "INR"));
    }
}
