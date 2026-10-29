package com.shopscope.monolith.order;

import com.shopscope.monolith.customer.Customer;
import com.shopscope.monolith.customer.CustomerService;
import com.shopscope.monolith.inventory.InventoryService;
import com.shopscope.monolith.notification.NotificationService;
import com.shopscope.monolith.payment.PaymentGateway;
import com.shopscope.monolith.payment.PaymentResult;
import com.shopscope.monolith.payment.PaymentResult.Approved;
import com.shopscope.monolith.payment.PaymentResult.Declined;
import com.shopscope.monolith.payment.RazorpayTestGateway;
import com.shopscope.monolith.pricing.PriceContext;
import com.shopscope.monolith.pricing.PricingService;
import com.shopscope.monolith.pricing.Quote;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Clock;
import java.util.UUID;

/**
 * Checkout: quote the basket, reserve the stock, take the money, record the order, tell the customer.
 *
 * <p>Read it once as a happy path. Then read it as the register of problems it is:
 * <ul>
 *   <li>It creates its own {@link PaymentGateway} with {@code new}, so no test can replace it
 *       (problem 2).</li>
 *   <li>It decrements stock before it knows whether the payment will succeed, and nothing puts the
 *       stock back when the payment fails (problem 6). There is no transaction to roll back, because
 *       there is no database: just maps.</li>
 *   <li>It calls the notification module directly, so an e-mail failure is a checkout failure
 *       (problem 1, the arrow that should not exist).</li>
 *   <li>Timing and logging are pasted in by hand (problem 5).</li>
 * </ul>
 */
public class OrderService {

    private static final Logger LOG = System.getLogger(OrderService.class.getName());

    private final PricingService pricing;
    private final InventoryService inventory;
    private final OrderRepository orders;
    private final CustomerService customers;
    private final NotificationService notifications;
    private final Clock clock;
    private final PaymentGateway gateway = new RazorpayTestGateway(); // problem 2: a hidden dependency

    public OrderService(PricingService pricing,
                        InventoryService inventory,
                        OrderRepository orders,
                        CustomerService customers,
                        NotificationService notifications,
                        Clock clock) {
        this.pricing = pricing;
        this.inventory = inventory;
        this.orders = orders;
        this.customers = customers;
        this.notifications = notifications;
        this.clock = clock;
    }

    public Order checkout(Basket basket, PriceContext context, String cardNumber) {
        long started = System.nanoTime();
        if (basket.isEmpty()) {
            throw new IllegalArgumentException("Cannot check out an empty basket");
        }
        Customer customer = customers.get(basket.customerId());
        Quote quote = pricing.quote(basket, context.withCustomerGroup(customer.customerGroup()));

        // Step 2: reserve stock. If step 3 fails, this is never undone. Problem 6.
        for (LineItem line : basket.lines()) {
            inventory.reserve(line.sku(), line.quantity());
        }

        // Step 3: take the money.
        PaymentResult result = gateway.charge(quote.total(), cardNumber);
        if (result instanceof Declined declined) {
            throw new PaymentDeclinedException(declined.reason());
        }
        Approved approved = (Approved) result;
        Order order = new Order(UUID.randomUUID().toString(), customer.id(), quote.lines(), quote.total(), approved.paymentId(), clock.instant());
        orders.save(order);
        notifications.orderConfirmation(customer, order); // problem 1: a direct call into another module

        LOG.log(Level.INFO, "order.checkout order={0} total={1} took={2}ms", order.id(), order.total(), (System.nanoTime() - started) / 1_000_000);
        return order;
    }
}
