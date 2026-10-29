package com.shopscope.monolith.inventory;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;

/**
 * Stock per SKU, in a map. {@link #reserve} decrements it immediately and for good; there is no
 * reservation to confirm or release later, and no lock, so two concurrent checkouts can both pass the
 * check and both decrement. Demo 10 and Demo 13 rebuild this properly.
 */
public class InventoryService {

    private static final Logger LOG = System.getLogger(InventoryService.class.getName());

    private final Map<String, Integer> stock = new HashMap<>();

    public void setStock(String sku, int units) {
        stock.put(sku, units);
    }

    public int available(String sku) {
        return stock.getOrDefault(sku, 0);
    }

    public void reserve(String sku, int units) {
        long started = System.nanoTime();
        int available = available(sku);
        if (available < units) {
            throw new OutOfStockException(sku, units, available);
        }
        stock.put(sku, available - units);
        LOG.log(Level.INFO, "inventory.reserve sku={0} units={1} left={2} took={3}ms", sku, units, available - units, (System.nanoTime() - started) / 1_000_000);
    }

    public void release(String sku, int units) {
        stock.merge(sku, units, Integer::sum);
        LOG.log(Level.INFO, "inventory.release sku={0} units={1} now={2}", sku, units, available(sku));
    }
}
