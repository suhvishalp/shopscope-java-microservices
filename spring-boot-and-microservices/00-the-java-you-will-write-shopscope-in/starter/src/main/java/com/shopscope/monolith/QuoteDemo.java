package com.shopscope.monolith;

import com.shopscope.monolith.catalog.CatalogRepository;
import com.shopscope.monolith.catalog.CatalogService;
import com.shopscope.monolith.order.Basket;
import com.shopscope.monolith.order.BasketReport;
import com.shopscope.monolith.order.LineItem;
import com.shopscope.monolith.pricing.PriceContext;
import com.shopscope.monolith.pricing.PricingService;
import com.shopscope.monolith.pricing.Quote;
import com.shopscope.monolith.pricing.TaxRules;

/**
 * Demo 0's runnable: price one basket and print it. No HTTP, no services, just the domain.
 * Run with {@code ./mvnw -q exec:java -Dexec.mainClass=com.shopscope.monolith.QuoteDemo}
 * or {@code java -cp target/classes com.shopscope.monolith.QuoteDemo}.
 */
public final class QuoteDemo {

    public static void main(String[] args) {
        CatalogRepository repository = new CatalogRepository();
        SeedData.products(repository);
        CatalogService catalog = new CatalogService(repository);
        PricingService pricing = new PricingService(SeedData.prices(), SeedData.discountRules(), TaxRules.forCountry("IN"));

        Basket basket = Basket.empty("c-1")
                .add(new LineItem(SeedData.TEA, catalog.get(SeedData.TEA).name(), 2))
                .add(new LineItem(SeedData.BISCUITS, catalog.get(SeedData.BISCUITS).name(), 3))
                .add(new LineItem(SeedData.KETTLE, catalog.get(SeedData.KETTLE).name(), 1));

        Quote quote = pricing.quote(basket, PriceContext.of("INR", "IN").withChannel("web"));

        System.out.println();
        quote.lines().forEach(line -> System.out.printf("  %-28s x%-3d %10s%n", line.name(), line.quantity(), line.lineTotal()));
        System.out.printf("  %-33s %10s%n", "Subtotal", quote.subtotal());
        System.out.printf("  %-33s %10s  %s%n", "Discount", quote.discount(), quote.appliedCodes());
        System.out.printf("  %-33s %10s%n", "Tax", quote.tax());
        System.out.printf("  %-33s %10s%n", "Total", quote.total());
        System.out.println("  " + BasketReport.summary(quote));
    }

    private QuoteDemo() {
    }
}
