package com.shopscope.monolith.notification;

import com.shopscope.monolith.customer.Customer;
import com.shopscope.monolith.order.Order;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * "Sends" e-mail by printing it and keeping a copy. Synchronous: checkout waits for it, and if it threw,
 * checkout would fail after the money was taken. Demo 3 decouples it with an event; Demo 24 makes it real.
 */
public class NotificationService {

    private static final Logger LOG = System.getLogger(NotificationService.class.getName());

    private final List<Email> sent = new ArrayList<>();

    public void orderConfirmation(Customer customer, Order order) {
        Email email = new Email(customer.email(),
                "Your ShopScope order " + order.id().substring(0, 8),
                "Hi " + customer.name() + ", thanks for your order of " + order.lines().size() + " line(s), total " + order.total() + ".");
        sent.add(email);
        LOG.log(Level.INFO, "notification.send to={0} subject=\"{1}\"", email.to(), email.subject());
    }

    public List<Email> sent() {
        return List.copyOf(sent);
    }
}
