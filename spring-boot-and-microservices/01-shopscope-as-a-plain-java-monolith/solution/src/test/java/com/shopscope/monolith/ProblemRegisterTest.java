package com.shopscope.monolith;

import com.shopscope.monolith.customer.CustomerService;
import com.shopscope.monolith.inventory.InventoryService;
import com.shopscope.monolith.notification.NotificationService;
import com.shopscope.monolith.order.Basket;
import com.shopscope.monolith.order.LineItem;
import com.shopscope.monolith.order.OrderRepository;
import com.shopscope.monolith.order.OrderService;
import com.shopscope.monolith.order.PaymentDeclinedException;
import com.shopscope.monolith.pricing.FlatRateTax;
import com.shopscope.monolith.pricing.PriceContext;
import com.shopscope.monolith.pricing.PricingService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The problem register, as executable tests. Each one states the behaviour ShopScope should have and is
 * disabled with the demo that makes it pass. Enabling it early shows the bug; see PROBLEMS.md.
 */
class ProblemRegisterTest {

    @Test
    @Disabled("Problem 6 (the overselling bug): fixed in Demo 10 with a transaction")
    void problem6_aDeclinedPaymentLeavesStockUntouched() {
        InventoryService inventory = new InventoryService();
        SeedData.stock(inventory);
        CustomerService customers = new CustomerService();
        SeedData.customers(customers);
        PricingService pricing = new PricingService(SeedData.prices(), SeedData.discountRules(), new FlatRateTax("GST", 18));
        OrderService service = new OrderService(pricing, inventory, new OrderRepository(), customers, new NotificationService(), Clock.systemUTC());
        Basket basket = Basket.empty("c-1").add(new LineItem(SeedData.KETTLE, "Kettle", 1));

        assertThatThrownBy(() -> service.checkout(basket, PriceContext.of("INR", "IN"), "4111111111110000"))
                .isInstanceOf(PaymentDeclinedException.class);

        assertThat(inventory.available(SeedData.KETTLE)).as("stock after a declined payment").isEqualTo(2);
    }
}
