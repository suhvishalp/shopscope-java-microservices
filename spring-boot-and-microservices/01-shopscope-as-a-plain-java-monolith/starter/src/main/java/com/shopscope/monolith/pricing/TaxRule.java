package com.shopscope.monolith.pricing;

import com.shopscope.monolith.shared.Money;

/** Tax on a net amount. One implementation for now; Demo 3 selects one per region with profiles. */
public interface TaxRule {

    String name();

    Money taxOn(Money net);
}
