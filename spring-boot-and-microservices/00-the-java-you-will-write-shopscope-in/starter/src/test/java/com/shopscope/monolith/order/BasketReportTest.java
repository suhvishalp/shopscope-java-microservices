package com.shopscope.monolith.order;

import com.shopscope.monolith.SeedData;
import com.shopscope.monolith.catalog.CatalogRepository;
import com.shopscope.monolith.catalog.CatalogService;
import com.shopscope.monolith.pricing.FlatRateTax;
import com.shopscope.monolith.pricing.PriceContext;
import com.shopscope.monolith.pricing.PricedLine;
import com.shopscope.monolith.pricing.PricingService;
import com.shopscope.monolith.pricing.Quote;
import com.shopscope.monolith.shared.Money;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@Disabled("enable in Lab 3")
class BasketReportTest {

    private final Basket basket = Basket.empty("c-1")
            .add(new LineItem(SeedData.TEA, "Tea", 2))
            .add(new LineItem(SeedData.MUG, "Mug", 1))
            .add(new LineItem(SeedData.KETTLE, "Kettle", 1))
            .add(new LineItem(SeedData.TEA, "Tea", 1)); // merged into the first tea line

    @Test
    void addMergesLinesForTheSameSku() {
        assertThat(basket.lines()).hasSize(3);
        assertThat(BasketReport.quantityBySku(basket)).containsOnly(entry(SeedData.TEA, 3), entry(SeedData.MUG, 1), entry(SeedData.KETTLE, 1));
    }

    @Test
    void groupsLinesByCatalogueCategory() {
        CatalogRepository repository = new CatalogRepository();
        SeedData.products(repository);

        Map<String, List<LineItem>> byCategory = BasketReport.linesByCategory(basket, new CatalogService(repository));

        assertThat(byCategory).containsOnlyKeys("tea", "kitchen");
        assertThat(byCategory.get("kitchen")).extracting(LineItem::sku).containsExactly(SeedData.MUG, SeedData.KETTLE);
    }

    @Test
    void answersQuestionsAboutAQuoteInOnePass() {
        Quote quote = new PricingService(SeedData.prices(), List.of(), new FlatRateTax("GST", 18))
                .quote(basket, PriceContext.of("INR", "IN"));

        assertThat(BasketReport.mostExpensiveLine(quote)).map(PricedLine::sku).hasValue(SeedData.KETTLE);
        assertThat(BasketReport.partitionByLineTotal(quote, Money.of("1000.00", "INR")).get(true)).hasSize(2);
        assertThat(BasketReport.summary(quote)).isEqualTo(new BasketReport.Summary(5, Money.of("3845.00", "INR")));
    }
}
