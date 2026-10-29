package com.shopscope.monolith.pricing;

import com.shopscope.monolith.order.Basket;
import com.shopscope.monolith.shared.Money;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

/**
 * Turns a basket into a {@link Quote}: select a price per line, apply the discount rules, add tax.
 * Streams and collectors do the arithmetic; the rules are data, not code paths.
 */
public class PricingService {

    private static final Logger LOG = System.getLogger(PricingService.class.getName());

    private final PriceList priceList;
    private final List<DiscountRule> rules;
    private final TaxRule taxRule;

    public PricingService(PriceList priceList, List<DiscountRule> rules, TaxRule taxRule) {
        this.priceList = priceList;
        this.rules = List.copyOf(rules);
        this.taxRule = taxRule;
    }

    public Quote quote(Basket basket, PriceContext context) {
        long started = System.nanoTime();
        Money zero = Money.zero(context.currency());

        List<PricedLine> lines = basket.lines().stream()
                .map(line -> new PricedLine(line.sku(), line.name(), line.quantity(), priceList.priceOrThrow(line.sku(), context)))
                .toList();

        Money subtotal = lines.stream().map(PricedLine::lineTotal).reduce(zero, Money::plus);

        Money lineDiscounts = rules.stream()
                .flatMap(rule -> lines.stream().map(line -> Discounts.lineDiscount(rule, line)))
                .reduce(zero, Money::plus);
        Money basketDiscounts = rules.stream()
                .map(rule -> Discounts.basketDiscount(rule, subtotal))
                .reduce(zero, Money::plus);
        Money discount = lineDiscounts.plus(basketDiscounts).min(subtotal);

        List<String> appliedCodes = rules.stream()
                .filter(rule -> !lines.stream().map(line -> Discounts.lineDiscount(rule, line)).reduce(zero, Money::plus).isZero()
                        || !Discounts.basketDiscount(rule, subtotal).isZero())
                .map(DiscountRule::code)
                .toList();

        Money net = subtotal.minus(discount);
        Money tax = taxRule.taxOn(net);
        Quote quote = new Quote(lines, subtotal, discount, tax, net.plus(tax), appliedCodes);

        // Copied into every service of the monolith. Demo 3 turns it into one aspect.
        LOG.log(Level.INFO, "pricing.quote lines={0} total={1} took={2}ms", lines.size(), quote.total(), (System.nanoTime() - started) / 1_000_000);
        return quote;
    }
}
