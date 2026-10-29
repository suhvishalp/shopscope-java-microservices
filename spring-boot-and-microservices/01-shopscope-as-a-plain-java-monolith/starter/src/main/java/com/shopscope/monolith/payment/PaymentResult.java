package com.shopscope.monolith.payment;

/** What a charge attempt came back with. Two cases, and a {@code switch} must handle both. */
public sealed interface PaymentResult permits PaymentResult.Approved, PaymentResult.Declined {

    record Approved(String paymentId) implements PaymentResult {}

    record Declined(String reason) implements PaymentResult {}
}
