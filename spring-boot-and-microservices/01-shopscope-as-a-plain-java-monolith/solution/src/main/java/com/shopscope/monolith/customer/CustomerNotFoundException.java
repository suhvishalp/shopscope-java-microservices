package com.shopscope.monolith.customer;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String id) {
        super("No customer with id " + id);
    }
}
