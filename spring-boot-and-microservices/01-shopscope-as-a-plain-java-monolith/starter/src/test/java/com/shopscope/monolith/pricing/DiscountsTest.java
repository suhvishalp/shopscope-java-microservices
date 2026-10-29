package com.shopscope.monolith.pricing;

import com.shopscope.monolith.pricing.DiscountRule.AmountOff;
import com.shopscope.monolith.pricing.DiscountRule.BasketOver;
import com.shopscope.monolith.pricing.DiscountRule.BuyXGetYFree;
import com.shopscope.monolith.pricing.DiscountRule.PercentageOff;
import com.shopscope.monolith.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscountsTest {

    private static final PricedLine TEA_X3 = new PricedLine("SKU-TEA-001", "Tea", 3, Money.of("349.00", "INR"));
    private static final PricedLine BISCUITS_X7 = new PricedLine("SKU-BIS-001", "Biscuits", 7, Money.of("129.00", "INR"));

    @Test
    void percentageOffAppliesToItsSkuOnly() {
        PercentageOff tea10 = new PercentageOff("TEA10", "SKU-TEA-001", 10);
        assertThat(Discounts.lineDiscount(tea10, TEA_X3)).isEqualTo(Money.of("104.70", "INR"));
        assertThat(Discounts.lineDiscount(tea10, BISCUITS_X7)).isEqualTo(Money.zero(TEA_X3.unitPrice().currency()));
    }

    @Test
    void amountOffIsPerUnitAndNeverMoreThanTheLine() {
        AmountOff rule = new AmountOff("TEA-OFF", "SKU-TEA-001", Money.of("400.00", "INR"));
        assertThat(Discounts.lineDiscount(rule, TEA_X3)).isEqualTo(TEA_X3.lineTotal());
    }

    @Test
    void threeForTwoGivesOneFreePerThree() {
        BuyXGetYFree rule = new BuyXGetYFree("3FOR2", "SKU-BIS-001", 2, 1);
        assertThat(Discounts.lineDiscount(rule, BISCUITS_X7)).isEqualTo(Money.of("258.00", "INR")); // 7 units: 2 free
    }

    @Test
    void basketOverAppliesAboveTheThresholdOnly() {
        BasketOver rule = new BasketOver("BIG5", Money.of("5000.00", "INR"), 5);
        assertThat(Discounts.basketDiscount(rule, Money.of("6000.00", "INR"))).isEqualTo(Money.of("300.00", "INR"));
        assertThat(Discounts.basketDiscount(rule, Money.of("5000.00", "INR"))).isEqualTo(Money.zero(rule.threshold().currency()));
        assertThat(Discounts.lineDiscount(rule, TEA_X3)).isEqualTo(Money.zero(rule.threshold().currency()));
    }
}
