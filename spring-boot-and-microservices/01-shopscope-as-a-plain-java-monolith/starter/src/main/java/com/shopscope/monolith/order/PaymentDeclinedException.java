package com.shopscope.monolith.order;

/** The payment provider said no. The basket is intact; the shopper may try another card. */
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String reason) {
        super("Payment declined: " + reason);
    }
}
