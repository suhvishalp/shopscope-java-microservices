package com.shopscope.monolith.pricing;

import com.shopscope.monolith.shared.Money;

/**
 * The promotions ShopScope can run. Sealed, like {@link PriceScope}: adding a fifth kind of rule is a
 * compile error in every {@code switch} that forgot it, which is exactly what you want.
 */
public sealed interface DiscountRule
        permits DiscountRule.PercentageOff, DiscountRule.AmountOff, DiscountRule.BuyXGetYFree, DiscountRule.BasketOver {

    /** The code printed on the quote, e.g. {@code TEA10}. */
    String code();

    /** 10% off every unit of one SKU. */
    record PercentageOff(String code, String sku, int percent) implements DiscountRule {}

    /** A fixed amount off every unit of one SKU. */
    record AmountOff(String code, String sku, Money amount) implements DiscountRule {}

    /** Buy {@code buy} units of a SKU, get {@code free} more for nothing ("3 for 2" is buy 2, free 1). */
    record BuyXGetYFree(String code, String sku, int buy, int free) implements DiscountRule {}

    /** A percentage off the whole basket once its subtotal passes a threshold. */
    record BasketOver(String code, Money threshold, int percent) implements DiscountRule {}
}
