package com.shopscope.monolith.customer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Customers, in a map. Demo 13 moves them to MongoDB, where a customer becomes a document. */
public class CustomerService {

    private final Map<String, Customer> byId = new LinkedHashMap<>();

    public void register(Customer customer) {
        byId.put(customer.id(), customer);
    }

    public Optional<Customer> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Customer get(String id) {
        return find(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }
}
