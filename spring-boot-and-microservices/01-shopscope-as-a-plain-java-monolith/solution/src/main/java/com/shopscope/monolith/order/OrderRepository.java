package com.shopscope.monolith.order;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Orders, in a map, in memory. Restart the process and they are gone. */
public class OrderRepository {

    private final Map<String, Order> byId = new LinkedHashMap<>();

    public void save(Order order) {
        byId.put(order.id(), order);
    }

    public Optional<Order> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<Order> findAll() {
        return List.copyOf(byId.values());
    }

    public int count() {
        return byId.size();
    }
}
