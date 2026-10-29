package com.shopscope.monolith.customer;

import java.util.Objects;

/** A registered shopper. The customer group ("retail", "wholesale") selects their prices. */
public record Customer(String id, String email, String name, String customerGroup) {

    public Customer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(name, "name");
        customerGroup = customerGroup == null ? "retail" : customerGroup;
    }
}
