package com.shopscope.monolith.pricing;

import com.shopscope.monolith.pricing.DiscountRule.AmountOff;
import com.shopscope.monolith.pricing.DiscountRule.BasketOver;
import com.shopscope.monolith.pricing.DiscountRule.BuyXGetYFree;
import com.shopscope.monolith.pricing.DiscountRule.PercentageOff;
import com.shopscope.monolith.shared.Money;

/**
 * What each rule takes off. Pattern matching with {@code instanceof} over the sealed {@link DiscountRule}:
 * each branch binds the rule to its concrete type and reads the record's components. (On Java 21 and
 * later this is one {@code switch} with record patterns and {@code when} guards.)
 */
public final class Discounts {

    private Discounts() {
    }

    /** The discount a rule gives on one priced line; zero when the rule is not about this line. */
    public static Money lineDiscount(DiscountRule rule, PricedLine line) {
        Money zero = Money.zero(line.unitPrice().currency());
        if (rule instanceof PercentageOff r && r.sku().equals(line.sku())) {
            return line.lineTotal().percent(r.percent());
        }
        if (rule instanceof AmountOff r && r.sku().equals(line.sku())) {
            return r.amount().times(line.quantity()).min(line.lineTotal());
        }
        if (rule instanceof BuyXGetYFree r && r.sku().equals(line.sku())) {
            int freeUnits = (line.quantity() / (r.buy() + r.free())) * r.free();
            return line.unitPrice().times(freeUnits);
        }
        return zero; // another SKU, or a basket-level rule
    }

    /** The discount a rule gives on the basket as a whole; zero for line-level rules. */
    public static Money basketDiscount(DiscountRule rule, Money subtotal) {
        if (rule instanceof BasketOver r && subtotal.isGreaterThan(r.threshold())) {
            return subtotal.percent(r.percent());
        }
        return Money.zero(subtotal.currency());
    }
}
