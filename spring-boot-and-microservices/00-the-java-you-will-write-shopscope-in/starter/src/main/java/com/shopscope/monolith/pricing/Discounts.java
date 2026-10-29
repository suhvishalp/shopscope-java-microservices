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
        // TODO(lab-2.3) one instanceof branch per line-level rule, each guarded by `&& r.sku().equals(line.sku())`:
        //   PercentageOff: percent of the line total
        //   AmountOff:     amount per unit, but never more than the line total (Money.min)
        //   BuyXGetYFree:  freeUnits = (quantity / (buy + free)) * free, times the unit price
        // Anything else is zero.
        return zero;
    }

    /** The discount a rule gives on the basket as a whole; zero for line-level rules. */
    public static Money basketDiscount(DiscountRule rule, Money subtotal) {
        // TODO(lab-2.4) BasketOver, when the subtotal is over the threshold: percent of the subtotal. Else zero.
        return Money.zero(subtotal.currency());
    }
}
