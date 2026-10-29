package com.shopscope.monolith.pricing;

import com.shopscope.monolith.shared.Money;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * All prices, grouped by SKU, and the one rule that picks a price for a shopper: among the prices that
 * apply to the context, in the right currency, the most specific scope wins.
 */
public class PriceList {

    private final Map<String, List<Price>> bySku;

    public PriceList(List<Price> prices) {
        this.bySku = prices.stream().collect(Collectors.groupingBy(Price::sku, Collectors.toUnmodifiableList()));
    }

    public Optional<Price> find(String sku, PriceContext context) {
        return bySku.getOrDefault(sku, List.of()).stream()
                .filter(price -> price.value().currency().equals(context.currency()))
                .filter(price -> price.scope().appliesTo(context))
                .max(Comparator.comparingInt(price -> price.scope().precedence()));
    }

    public Money priceOrThrow(String sku, PriceContext context) {
        return find(sku, context).map(Price::value).orElseThrow(() -> new NoPriceException(sku, context));
    }

    public int size() {
        return bySku.values().stream().mapToInt(List::size).sum();
    }
}
