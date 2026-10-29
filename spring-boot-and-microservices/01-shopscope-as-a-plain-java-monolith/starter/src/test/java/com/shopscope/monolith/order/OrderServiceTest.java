package com.shopscope.monolith.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Lab 3 of Demo 1: test OrderService in isolation. You will find that everything it needs can be built
 * here by hand, except one thing. See the guide.
 */
class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void wireByHand() {
        // TODO(lab-3.1) build an InventoryService, CustomerService, OrderRepository, NotificationService and
        // PricingService by hand (SeedData has the data), then construct the OrderService.
    }

    @Test
    @Disabled("TODO(lab-3.2)")
    void approvedPaymentPlacesTheOrderAndEmailsTheCustomer() {
        // TODO(lab-3.2) check out a kettle with card 4111111111111111: an order is saved, stock drops to 1, one e-mail is sent.
    }

    @Test
    @Disabled("TODO(lab-3.3)")
    void declinedPaymentPlacesNoOrder() {
        // TODO(lab-3.3) the only way to get a decline is a card ending in 0000, because the gateway is created
        // inside OrderService with new. Write the test, then write down what you could not do.
    }
}
