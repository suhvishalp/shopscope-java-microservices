package com.shopscope.monolith.catalog;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Optional;

/**
 * Catalogue lookups that cannot lie.
 *
 * <p>Two ways to ask for a product, for two kinds of caller:
 * <ul>
 *   <li>{@link #find(String)} returns an {@link Optional}: "it may not be there, and that is normal".
 *       A search box uses this.</li>
 *   <li>{@link #get(String)} returns the product or throws {@link ProductNotFoundException}:
 *       "it must be there, and if it is not, something upstream is wrong". Checkout uses this.</li>
 * </ul>
 * What this class never does is return {@code null}.
 */
public class CatalogService {

    private static final Logger LOG = System.getLogger(CatalogService.class.getName());

    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    public Optional<Product> find(String sku) {
        long started = System.nanoTime();
        Optional<Product> product = repository.findBySku(sku);
        LOG.log(Level.INFO, "catalog.find sku={0} found={1} took={2}ms", sku, product.isPresent(), elapsedMillis(started));
        return product;
    }

    public Product get(String sku) {
        long started = System.nanoTime();
        Product product = repository.findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
        LOG.log(Level.INFO, "catalog.get sku={0} took={1}ms", sku, elapsedMillis(started));
        return product;
    }

    public List<Product> all() {
        long started = System.nanoTime();
        List<Product> products = repository.findAll();
        LOG.log(Level.INFO, "catalog.all count={0} took={1}ms", products.size(), elapsedMillis(started));
        return products;
    }

    public List<Product> inCategory(String category) {
        return List.copyOf(repository.findByCategory(category));
    }

    // Copied into every service of the monolith. Demo 3 turns it into one aspect.
    private static long elapsedMillis(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
