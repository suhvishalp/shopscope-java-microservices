package com.shopscope.monolith.order;

import com.shopscope.monolith.SeedData;
import com.shopscope.monolith.customer.CustomerService;
import com.shopscope.monolith.inventory.InventoryService;
import com.shopscope.monolith.notification.NotificationService;
import com.shopscope.monolith.pricing.FlatRateTax;
import com.shopscope.monolith.pricing.PriceContext;
import com.shopscope.monolith.pricing.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lab 3 of Demo 1. Everything OrderService needs can be built here by hand, except the payment gateway:
 * it is created inside the constructor with {@code new}, so these tests can only use the gateway's own
 * rules (cards ending in 0000 are declined) to drive the outcome. That coupling is problem 2.
 */
class OrderServiceTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC);

    private InventoryService inventory;
    private OrderRepository orders;
    private NotificationService notifications;
    private OrderService service;

    @BeforeEach
    void wireByHand() {
        inventory = new InventoryService();
        SeedData.stock(inventory);
        CustomerService customers = new CustomerService();
        SeedData.customers(customers);
        orders = new OrderRepository();
        notifications = new NotificationService();
        PricingService pricing = new PricingService(SeedData.prices(), SeedData.discountRules(), new FlatRateTax("GST", 18));
        service = new OrderService(pricing, inventory, orders, customers, notifications, FIXED);
    }

    @Test
    void approvedPaymentPlacesTheOrderAndEmailsTheCustomer() {
        Basket basket = Basket.empty("c-1").add(new LineItem(SeedData.KETTLE, "Kettle", 1));

        Order order = service.checkout(basket, PriceContext.of("INR", "IN"), "4111111111111111");

        assertThat(order.placedAt()).isEqualTo(FIXED.instant());
        assertThat(orders.findById(order.id())).isPresent();
        assertThat(inventory.available(SeedData.KETTLE)).isEqualTo(1);
        assertThat(notifications.sent()).singleElement().satisfies(email -> assertThat(email.to()).isEqualTo("priya@example.com"));
    }

    @Test
    void declinedPaymentPlacesNoOrder() {
        Basket basket = Basket.empty("c-1").add(new LineItem(SeedData.KETTLE, "Kettle", 1));

        assertThatThrownBy(() -> service.checkout(basket, PriceContext.of("INR", "IN"), "4111111111110000"))
                .isInstanceOf(PaymentDeclinedException.class);

        assertThat(orders.count()).isZero();
        assertThat(notifications.sent()).isEmpty();
    }

    @Test
    void emptyBasketIsRejectedBeforeAnythingHappens() {
        assertThatThrownBy(() -> service.checkout(Basket.empty("c-1"), PriceContext.of("INR", "IN"), "4111111111111111"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
