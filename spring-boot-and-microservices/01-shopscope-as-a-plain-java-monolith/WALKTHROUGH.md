# Demo 1 — Walkthrough: the finished code

Read this after the session, with `solution/` open. It explains the monolith module by module, in the
order the composition root builds it, and says for each part which problem of the register it carries.
Demo 0's domain pieces (money, prices, rules, baskets, catalogue lookups) are explained in
[Demo 0's walkthrough](../00-the-java-you-will-write-shopscope-in/WALKTHROUGH.md); this one covers what
Demo 1 adds.

## The shape

```
com.shopscope.monolith
├── ShopScopeMonolith     main(): configuration, stores, services, the catalogue feed, HTTP, shutdown
├── SeedData              the data every run starts with
├── shared                Money
├── catalog               products
├── pricing               prices, scopes, rules, quotes, tax
├── inventory             stock
├── customer              customers
├── order                 baskets, orders, checkout
├── payment               the gateway port and its test stand-in
├── notification          e-mail
├── config                AppConfig
└── http                  ShopHttpServer, Json
```

Seven business modules, two technical ones (`config`, `http`), one shared value type. Each business
module is a package with a service and, where it stores anything, a map. That layout survives the
whole track: in Demo 5 each package becomes a Maven module; in Demos 12 and 13 each Maven module becomes
a service. The names do not change.

## `ShopScopeMonolith`: the composition root

```java
AppConfig config = AppConfig.load();
PriceContext context = PriceContext.of(config.currency(), config.country()).withChannel(config.channel());

CatalogRepository catalogRepository = new CatalogRepository();
OrderRepository orderRepository = new OrderRepository();
InventoryService inventory = new InventoryService();
CustomerService customers = new CustomerService();
PriceList priceList = SeedData.prices();
SeedData.stock(inventory);
SeedData.customers(customers);

CatalogService catalog = new CatalogService(catalogRepository);
TaxRule taxRule = TaxRules.forCountry(config.country());
PricingService pricing = new PricingService(priceList, SeedData.discountRules(), taxRule);
NotificationService notifications = new NotificationService();
OrderService orders = new OrderService(pricing, inventory, orderRepository, customers, notifications, Clock.systemUTC());

seedCatalogue(catalogRepository, config.catalogSeedDelayMillis());
```

Five numbered sections: configuration, stores, services, the slow catalogue feed, then HTTP. The order
inside section 3 is the dependency graph: `PricingService` needs a `PriceList` and a `TaxRule`,
`OrderService` needs `PricingService`, so the lines come in that order and the compiler enforces it.

Two decisions are worth noticing. `PriceContext` is built once from configuration and handed to the
HTTP layer: the monolith serves one store in one currency, which is problem 4 in a different costume.
And `Clock.systemUTC()` is the only "infrastructure" object injected; it exists because Lab 2 needed a
seam for time, and it is the first dependency in the code base that was added for a test's sake. There
will be many more.

The `--demo` branch runs one scripted checkout and returns, which is how the guide verifies without a
browser. The server branch starts HTTP after the feed has loaded (Lab 8), installs a shutdown hook that
stops the server and reports the in-memory counts, and parks the main thread with `join()`.

## `SeedData`: one data set for the whole track

Four products, eleven prices across INR and GBP and four scopes, three discount rules, stock levels that
make the kettle scarce (2) and the biscuits plentiful (100), and two customers in two groups. The same
SKUs appear in every later demo, in Flyway migrations and in test fixtures, so a learner who remembers
that `SKU-KET-001` has two in stock can predict the outcome of every overselling example in the track.

## `inventory`: stock in a map, and the check-then-act

```java
public void reserve(String sku, int units) {
    int available = available(sku);
    if (available < units) {
        throw new OutOfStockException(sku, units, available);
    }
    stock.put(sku, available - units);
}
```

Four lines, two bugs. The one the guide reproduces: the decrement is immediate and permanent, so a
checkout that fails after this line has lost a unit. The one the challenge hints at: `available` is
read, compared and written in three steps with no lock, so two threads can both pass the check.
`release` exists and nothing calls it; that is deliberate, so Lab 6's "just call release" temptation has
something to call.

## `customer`: customers in a map

The smallest module: a record with a `customerGroup` defaulting to `retail`, a service with `find`
(Optional) and `get` (throws), the same pair as `CatalogService`. Its only job in Demo 1 is to give
`OrderService` a customer group to price with. In Demo 13 this module moves to MongoDB and gains an
embedded address book; the `find`/`get` pair survives unchanged.

## `payment`: a port and a stand-in

```java
public interface PaymentGateway {
    PaymentResult charge(Money amount, String cardNumber);
}

public sealed interface PaymentResult permits Approved, Declined { ... }
```

The one place in Demo 1 where the design is right on purpose. `PaymentGateway` is an interface (a port),
`PaymentResult` is a sealed pair so a caller must handle both outcomes, and `RazorpayTestGateway` is a
fake with two rules (cards ending `0000` decline, amounts over a lakh decline) and no network. The design
is right and the *use* is wrong: `OrderService` creates the gateway itself, so the port is never used as
a port. Demo 2 fixes the use; Demo 23 puts the real Razorpay behind the same interface and nothing above
it changes.

## `notification`: e-mail that is a log line

`orderConfirmation(customer, order)` builds an `Email` record, appends it to a list and logs it. The
list exists so tests can assert "one e-mail was sent" without reading logs. It is synchronous and in the
checkout call path, which means a failure here would fail a checkout whose payment has already been
taken. Demo 3 turns the call into an `OrderPlaced` event; Demo 24 makes it a real e-mail through Resend
with an outbox so it cannot be lost or sent twice.

## `order`: checkout, and most of the register

`Order` is a record: id, customer, priced lines, total, payment id, time. `OrderRepository` is a map.
`OrderService.checkout` is the method the guide spends three labs on:

```java
Customer customer = customers.get(basket.customerId());
Quote quote = pricing.quote(basket, context.withCustomerGroup(customer.customerGroup()));

for (LineItem line : basket.lines()) {
    inventory.reserve(line.sku(), line.quantity());          // step 2: never undone
}

PaymentResult result = gateway.charge(quote.total(), cardNumber);   // step 3: the hidden gateway
if (result instanceof Declined declined) {
    throw new PaymentDeclinedException(declined.reason());
}
Approved approved = (Approved) result;
Order order = new Order(UUID.randomUUID().toString(), customer.id(), quote.lines(), quote.total(), approved.paymentId(), clock.instant());
orders.save(order);
notifications.orderConfirmation(customer, order);          // a direct call that should be an event
```

The `instanceof` branch on the sealed result is the Java 17 form; on Java 21 it is a `switch` with two
cases and no `default`. Either way the compiler knows there are exactly two outcomes.

The javadoc on the class lists the problems in the order they appear in the method, and the guide's labs
follow the same order. The `history` method the guide asks you to add in Lab 5 is in the solution too,
without timing, as the example of plumbing that gets forgotten.

## `config`: `AppConfig`

Loads `/shopscope.properties` from the classpath, then merges a file named by `-Dshopscope.config`
if present. Each getter parses its own value with a default invented on the spot. The important thing it
does *not* do: validate at start-up. A bad port fails when `port()` is first called, after the catalogue
has loaded. Boot's `@ConfigurationProperties` with `@Validated` (Demo 4) fails before anything starts
and names the property.

## `http`: `ShopHttpServer` and `Json`

`ShopHttpServer` registers three contexts on a JDK `HttpServer` and dispatches on path strings.
`products` handles both the list and the single resource by inspecting the path; `checkout` reads the
body, parses it with `Json.parseFlatObject`, builds a one-line basket and calls `OrderService`. Errors are
mapped in two `catch` blocks: the three domain exceptions become `422`, everything else becomes `500` with
`e.toString()` in the body.

`Json` encodes with insertion-ordered maps (`LinkedHashMap`, so field order is stable) and "parses" with
one regular expression that finds `"key":"string"` or `"key":123` pairs anywhere in the text. That is
why a nested `{"customer":{"id":"c-1"}}` yields an `id` field and no `customerId`: the pattern has no
idea of depth. It is deliberately the smallest parser that makes the happy path work, so the failure
paths are as visible as possible.

## The tests

`OrderServiceTest` wires five collaborators by hand to test one method, and documents in its javadoc the
one it cannot replace. `ProblemRegisterTest` holds one disabled test that states the correct stock after
a declined payment; Demo 10 enables it. Every other test is Demo 0's, unchanged.

`src/test/resources/logging.properties` raises the test log level to `WARNING`, because the monolith logs
every call and the test report was unreadable. The logging is problem 5; the properties file is the
monolith's own medicine applied to its own tests.

## `PROBLEMS.md`

The register, nine rows, with the lab that exposed each problem and the demo that closes it. It is the
only file in this demo that later demos edit instead of replace: each one strikes through the rows it
closes.

## What Demo 2 changes

`ShopScopeMonolith.main` shrinks to three lines: create an `AnnotationConfigApplicationContext` from a
`@Configuration` class, get the `ShopHttpServer` bean, start it. The `new` lines move into `@Bean`
methods, which can be in any order. `OrderService` gets `PaymentGateway` through its constructor, and
`OrderServiceTest` hands it a two-line fake that declines on demand. `NotificationService` is still
called directly; that is Demo 3's event. The HTTP layer, the maps and the overselling bug are untouched:
Demo 2 is about wiring, and only wiring.
