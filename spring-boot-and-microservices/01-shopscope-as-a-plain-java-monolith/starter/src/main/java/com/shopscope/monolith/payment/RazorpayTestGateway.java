package com.shopscope.monolith.payment;

import com.shopscope.monolith.shared.Money;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.UUID;

/**
 * A stand-in for Razorpay's test mode. Cards ending in {@code 0000} are declined, as are amounts over
 * one lakh; everything else is approved with a made-up payment id. No network, no secrets.
 */
public class RazorpayTestGateway implements PaymentGateway {

    private static final Logger LOG = System.getLogger(RazorpayTestGateway.class.getName());

    @Override
    public PaymentResult charge(Money amount, String cardNumber) {
        LOG.log(Level.INFO, "razorpay.charge amount={0} card=****{1}", amount, cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
        if (cardNumber.endsWith("0000")) {
            return new PaymentResult.Declined("card declined by issuer");
        }
        if (amount.amount().intValue() > 100_000) {
            return new PaymentResult.Declined("amount over limit");
        }
        return new PaymentResult.Approved("pay_" + UUID.randomUUID().toString().substring(0, 12));
    }
}
