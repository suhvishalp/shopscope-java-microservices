package com.shopscope.monolith;

import com.shopscope.monolith.catalog.CatalogRepository;
import com.shopscope.monolith.catalog.CatalogService;
import com.shopscope.monolith.config.AppConfig;
import com.shopscope.monolith.customer.CustomerService;
import com.shopscope.monolith.http.ShopHttpServer;
import com.shopscope.monolith.inventory.InventoryService;
import com.shopscope.monolith.notification.NotificationService;
import com.shopscope.monolith.order.Basket;
import com.shopscope.monolith.order.LineItem;
import com.shopscope.monolith.order.Order;
import com.shopscope.monolith.order.OrderRepository;
import com.shopscope.monolith.order.OrderService;
import com.shopscope.monolith.pricing.PriceContext;
import com.shopscope.monolith.pricing.PriceList;
import com.shopscope.monolith.pricing.PricingService;
import com.shopscope.monolith.pricing.TaxRule;
import com.shopscope.monolith.pricing.TaxRules;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Clock;

/**
 * The composition root: the one place where the whole object graph is built, by hand, with {@code new},
 * in dependency order. Every service here has a constructor; every constructor needs things that were
 * constructed earlier; so the order of the lines below is load-bearing. Swap two and it does not compile.
 * Add a dependency to one service and this file changes. Demo 2 hands this job to a container.
 *
 * <p>Run it: {@code ./mvnw -q exec:java} (serves HTTP on the configured port) or
 * {@code ./mvnw -q exec:java -Dexec.args=--demo} (runs one scripted checkout and exits).
 */
public final class ShopScopeMonolith {

    private static final Logger LOG = System.getLogger(ShopScopeMonolith.class.getName());

    public static void main(String[] args) throws Exception {
        boolean demo = args.length > 0 && args[0].equals("--demo");

        // 1. Configuration, read by hand.
        AppConfig config = AppConfig.load();
        PriceContext context = PriceContext.of(config.currency(), config.country()).withChannel(config.channel());

        // 2. Stores (maps) and the data in them.
        CatalogRepository catalogRepository = new CatalogRepository();
        OrderRepository orderRepository = new OrderRepository();
        InventoryService inventory = new InventoryService();
        CustomerService customers = new CustomerService();
        PriceList priceList = SeedData.prices();
        SeedData.stock(inventory);
        SeedData.customers(customers);

        // 3. Services, in dependency order. Try moving OrderService above PricingService.
        CatalogService catalog = new CatalogService(catalogRepository);
        TaxRule taxRule = TaxRules.forCountry(config.country());
        PricingService pricing = new PricingService(priceList, SeedData.discountRules(), taxRule);
        NotificationService notifications = new NotificationService();
        OrderService orders = new OrderService(pricing, inventory, orderRepository, customers, notifications, Clock.systemUTC());

        // 4. The catalogue arrives last, slowly, as if from a supplier feed. Start-up order matters:
        //    the HTTP server must not accept requests before this has finished (Lab 8).
        seedCatalogue(catalogRepository, config.catalogSeedDelayMillis());

        if (demo) {
            runScriptedCheckout(catalog, orders, context);
            return;
        }

        // 5. The HTTP layer, started after everything it needs exists, and stopped on Ctrl+C.
        ShopHttpServer http = new ShopHttpServer(catalog, inventory, orders, context);
        http.start(config.port());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            http.stop();
            LOG.log(Level.INFO, "shutdown orders={0} emails={1}", orderRepository.count(), notifications.sent().size());
        }, "shutdown"));
        LOG.log(Level.INFO, "ShopScope monolith ready: http://localhost:{0}/api/products", String.valueOf(config.port()));
        Thread.currentThread().join();
    }

    private static void seedCatalogue(CatalogRepository catalogRepository, long delayMillis) throws InterruptedException {
        LOG.log(Level.INFO, "catalog.seed loading supplier feed...");
        Thread.sleep(delayMillis);
        SeedData.products(catalogRepository);
        LOG.log(Level.INFO, "catalog.seed loaded products={0}", catalogRepository.count());
    }

    private static void runScriptedCheckout(CatalogService catalog, OrderService orders, PriceContext context) {
        Basket basket = Basket.empty("c-1")
                .add(new LineItem(SeedData.TEA, catalog.get(SeedData.TEA).name(), 2))
                .add(new LineItem(SeedData.BISCUITS, catalog.get(SeedData.BISCUITS).name(), 3))
                .add(new LineItem(SeedData.KETTLE, catalog.get(SeedData.KETTLE).name(), 1));
        Order order = orders.checkout(basket, context, "4111111111111111");
        System.out.println();
        System.out.println("Order " + order.id());
        order.lines().forEach(line -> System.out.printf("  %-28s x%-3d %s%n", line.name(), line.quantity(), line.lineTotal()));
        System.out.println("  Total " + order.total() + "  (payment " + order.paymentId() + ")");
    }

    private ShopScopeMonolith() {
    }
}
