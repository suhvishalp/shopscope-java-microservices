package com.shopscope.monolith.payment;

import com.shopscope.monolith.shared.Money;

/** The port to a payment provider. Demo 23 puts the real Razorpay behind it. */
public interface PaymentGateway {

    PaymentResult charge(Money amount, String cardNumber);
}
