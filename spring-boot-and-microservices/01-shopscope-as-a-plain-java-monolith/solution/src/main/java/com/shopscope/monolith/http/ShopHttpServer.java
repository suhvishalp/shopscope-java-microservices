package com.shopscope.monolith.http;

import com.shopscope.monolith.catalog.CatalogService;
import com.shopscope.monolith.catalog.Product;
import com.shopscope.monolith.catalog.ProductNotFoundException;
import com.shopscope.monolith.inventory.InventoryService;
import com.shopscope.monolith.inventory.OutOfStockException;
import com.shopscope.monolith.order.Basket;
import com.shopscope.monolith.order.LineItem;
import com.shopscope.monolith.order.Order;
import com.shopscope.monolith.order.OrderService;
import com.shopscope.monolith.order.PaymentDeclinedException;
import com.shopscope.monolith.pricing.PriceContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The monolith's HTTP layer, written by hand on the JDK's {@link HttpServer}. Routing is string
 * comparison, JSON is {@link Json}, status codes are chosen in each handler, and every exception type
 * is mapped by hand or falls through to a 500 with the stack trace in the body. Demo 5 replaces all of it.
 *
 * <p>Routes (the same paths the microservices will serve later):
 * <pre>
 *   GET  /api/products
 *   GET  /api/products/{sku}
 *   GET  /api/inventory/{sku}
 *   POST /api/checkout        {"customerId":"c-1","sku":"SKU-TEA-001","quantity":2,"card":"4111111111111111"}
 * </pre>
 */
public class ShopHttpServer {

    private static final Logger LOG = System.getLogger(ShopHttpServer.class.getName());

    private final CatalogService catalog;
    private final InventoryService inventory;
    private final OrderService orders;
    private final PriceContext context;
    private HttpServer server;

    public ShopHttpServer(CatalogService catalog, InventoryService inventory, OrderService orders, PriceContext context) {
        this.catalog = catalog;
        this.inventory = inventory;
        this.orders = orders;
        this.context = context;
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/products", this::products);
        server.createContext("/api/inventory", this::inventory);
        server.createContext("/api/checkout", this::checkout);
        server.start();
        LOG.log(Level.INFO, "http.start port={0}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            LOG.log(Level.INFO, "http.stop");
        }
    }

    private void products(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/api/products")) {
            List<Object> items = catalog.all().stream().map(this::productJson).map(Object.class::cast).toList();
            send(exchange, 200, Json.array(items));
            return;
        }
        String sku = path.substring("/api/products/".length());
        try {
            send(exchange, 200, Json.object(productJson(catalog.get(sku))));
        } catch (ProductNotFoundException e) {
            send(exchange, 404, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void inventory(HttpExchange exchange) throws IOException {
        String sku = exchange.getRequestURI().getPath().substring("/api/inventory/".length());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sku", sku);
        body.put("available", inventory.available(sku));
        send(exchange, 200, Json.object(body));
    }

    private void checkout(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        try {
            String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> fields = Json.parseFlatObject(raw);
            Product product = catalog.get(fields.get("sku"));
            Basket basket = Basket.empty(fields.get("customerId"))
                    .add(new LineItem(product.sku(), product.name(), Integer.parseInt(fields.get("quantity"))));
            Order order = orders.checkout(basket, context, fields.get("card"));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("orderId", order.id());
            body.put("total", order.total().toString());
            body.put("paymentId", order.paymentId());
            send(exchange, 201, Json.object(body));
        } catch (ProductNotFoundException | OutOfStockException | PaymentDeclinedException e) {
            send(exchange, 422, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (RuntimeException e) {
            // Everything else: a 500 with the exception in the body. Including NumberFormatException from
            // a quantity that was not a number, and NullPointerException from a missing field.
            LOG.log(Level.ERROR, "http.checkout failed", e);
            send(exchange, 500, "{\"error\":\"" + e + "\"}");
        }
    }

    private Map<String, Object> productJson(Product product) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("sku", product.sku());
        fields.put("name", product.name());
        fields.put("category", product.category());
        fields.put("description", product.description());
        return fields;
    }

    private static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
        LOG.log(Level.INFO, "http {0} {1} -> {2}", exchange.getRequestMethod(), exchange.getRequestURI().getPath(), status);
    }
}
