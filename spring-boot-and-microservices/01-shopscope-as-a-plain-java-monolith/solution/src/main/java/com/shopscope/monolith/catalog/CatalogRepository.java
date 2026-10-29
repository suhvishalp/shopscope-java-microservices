package com.shopscope.monolith.catalog;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The catalogue's "database": a map in memory. Everything the monolith stores lives in maps like
 * this one, inside the same process, with no transactions and no persistence. Demo 7 replaces it.
 */
public class CatalogRepository {

    private final Map<String, Product> bySku = new LinkedHashMap<>();

    public void save(Product product) {
        bySku.put(product.sku(), product);
    }

    public Optional<Product> findBySku(String sku) {
        return Optional.ofNullable(bySku.get(sku));
    }

    public List<Product> findAll() {
        return List.copyOf(bySku.values());
    }

    public Collection<Product> findByCategory(String category) {
        return bySku.values().stream().filter(p -> p.category().equals(category)).toList();
    }

    public int count() {
        return bySku.size();
    }
}
