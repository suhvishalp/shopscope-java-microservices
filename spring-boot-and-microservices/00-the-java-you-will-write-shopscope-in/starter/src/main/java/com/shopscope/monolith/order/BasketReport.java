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
        // TODO(lab-3.1) groupingBy sku, summingInt quantity
        throw new UnsupportedOperationException("TODO(lab-3.1)");
    }

    /** Lines grouped by the product's category, looked up in the catalogue. */
    public static Map<String, List<LineItem>> linesByCategory(Basket basket, CatalogService catalog) {
        // TODO(lab-3.2) groupingBy the category the catalogue returns for the line's SKU
        throw new UnsupportedOperationException("TODO(lab-3.2)");
    }

    /** The line that costs the most, if the quote has any lines. */
    public static Optional<PricedLine> mostExpensiveLine(Quote quote) {
        // TODO(lab-3.3) max by line total; the result is an Optional because the quote may have no lines
        throw new UnsupportedOperationException("TODO(lab-3.3)");
    }

    /** Lines split into "over the threshold" (true) and "under" (false). */
    public static Map<Boolean, List<PricedLine>> partitionByLineTotal(Quote quote, Money threshold) {
        // TODO(lab-3.4) partitioningBy "line total greater than threshold"
        throw new UnsupportedOperationException("TODO(lab-3.4)");
    }

    /** Two results from one pass: the number of units and the average unit price. */
    public static Summary summary(Quote quote) {
        // TODO(lab-3.5) Collectors.teeing: summingInt quantity, and the line totals reduced with Money::plus
        throw new UnsupportedOperationException("TODO(lab-3.5)");
    }

    public record Summary(int units, Money subtotal) {}
}
