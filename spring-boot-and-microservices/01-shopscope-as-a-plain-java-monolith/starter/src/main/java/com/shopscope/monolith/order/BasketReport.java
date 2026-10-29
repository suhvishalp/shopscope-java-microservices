package com.shopscope.monolith.order;

import com.shopscope.monolith.catalog.CatalogService;
import com.shopscope.monolith.pricing.PricedLine;
import com.shopscope.monolith.pricing.Quote;
import com.shopscope.monolith.shared.Money;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Questions a basket gets asked, answered with streams. Each method is one pipeline: a source, some
 * intermediate operations, one terminal operation. No loops, no mutable accumulators.
 */
public final class BasketReport {

    private BasketReport() {
    }

    /** SKU to total quantity, merged across lines. */
    public static Map<String, Integer> quantityBySku(Basket basket) {
        return basket.lines().stream()
                .collect(Collectors.groupingBy(LineItem::sku, Collectors.summingInt(LineItem::quantity)));
    }

    /** Lines grouped by the product's category, looked up in the catalogue. */
    public static Map<String, List<LineItem>> linesByCategory(Basket basket, CatalogService catalog) {
        return basket.lines().stream()
                .collect(Collectors.groupingBy(line -> catalog.get(line.sku()).category()));
    }

    /** The line that costs the most, if the quote has any lines. */
    public static Optional<PricedLine> mostExpensiveLine(Quote quote) {
        return quote.lines().stream().max(Comparator.comparing(PricedLine::lineTotal));
    }

    /** Lines split into "over the threshold" (true) and "under" (false). */
    public static Map<Boolean, List<PricedLine>> partitionByLineTotal(Quote quote, Money threshold) {
        return quote.lines().stream()
                .collect(Collectors.partitioningBy(line -> line.lineTotal().isGreaterThan(threshold)));
    }

    /** Two results from one pass: the number of units and the average unit price. */
    public static Summary summary(Quote quote) {
        return quote.lines().stream().collect(Collectors.teeing(
                Collectors.summingInt(PricedLine::quantity),
                Collectors.mapping(PricedLine::lineTotal, Collectors.reducing(Money.zero(quote.total().currency()), Money::plus)),
                (units, subtotal) -> new Summary(units, subtotal)));
    }

    public record Summary(int units, Money subtotal) {}
}
