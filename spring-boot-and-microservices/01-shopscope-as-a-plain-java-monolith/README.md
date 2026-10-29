# Demo 1 — ShopScope as a plain-Java monolith

**Demo guide** · ~120 minutes · you run the whole backend in one process, break it in nine ways, and write the list that the rest of the track works through

---

## What you ship today

A working online-store backend with no framework in it: seven modules (catalog, pricing, inventory,
customer, order, payment, notification) in one Java process, in-memory stores, a configuration file read
by hand, logging pasted into every method, and an HTTP layer written on the JDK's own `HttpServer`. You
can list products, check stock and place an order with `curl`.

It works. By the end of the session you will also have **the problem register**: a table of nine things
that are wrong with how it is built, each with the demo that fixes it. Every later demo of this track
closes at least one row. When the table is empty, ShopScope is a system of microservices.

By the end you will be able to answer, without hesitating:

- Why the order of the lines in `main()` is load-bearing, and what that says about the design
- Why `OrderService` cannot be unit-tested, and the one word that causes it
- What "cross-cutting" means, and how you can spot it with `grep`
- Why a declined payment leaves the kettle out of stock, and why there is no `rollback` to call
- What a hand-rolled HTTP layer gets wrong before you have written a single business rule
- The difference between a monolith, a modular monolith and microservices, in one sentence each

---

## Pre-reading

The theory for this demo is in the study notes; the demo assumes you have read it.

| Study notes | Read |
|---|---|
| [study-notes 04 — The monolith and its problems](../../study-notes/04-the-monolith-and-its-problems/) | in full |
| [study-notes 01 — Modern Java for Spring developers](../../study-notes/01-modern-java-for-spring/) | sealed types, Optional and exceptions, as a refresher |

The full demo-to-notes map is in [study-notes/README.md](../../study-notes/README.md#mapping-to-the-shopscope-demos).

---

## Before the demo (10 minutes)

1. **Have the repository and a JDK.** JDK 17 or newer (`java -version`). The code compiles for 17 and runs
   unchanged on 21 and 25.

   ```bash
   git clone https://github.com/suhvishalp/shopscope-java-microservices.git
   cd shopscope-java-microservices/spring-boot-and-microservices/01-shopscope-as-a-plain-java-monolith/starter
   ./mvnw -q verify
   ```

   The build downloads Maven and the dependencies the first time (a minute on a normal connection) and ends
   quietly. Every test passes; two are skipped on purpose and you will write them today.

2. **Run it once, both ways.**

   ```bash
   ./mvnw -q exec:java -Dexec.args=--demo      # one scripted checkout, printed, then exit
   ./mvnw -q exec:java                          # the HTTP server on http://localhost:8080, Ctrl+C to stop
   ```

   With the server running, in a second terminal: `curl -s localhost:8080/api/products`. If you see four
   products, you are ready. If you see `[]`, wait two seconds and try again: that is Lab 8, not a problem
   with your machine.

3. **Have `curl` or an HTTP client.** IntelliJ's HTTP client and the VS Code REST Client both run the
   requests in `http/monolith.http`. The guide shows `curl`.

**No Docker today.** Everything lives in memory, which is one of the problems.

---

## How this guide works

Every lab has the same shape:

| | |
|---|---|
| **Problem** | What actually hurts, concretely |
| **Concept** | The idea that explains it, plainly |
| **Steps** | What to do, in order, with the file named |
| **Verify** | The specific thing you should see |
| **Watch out** | The mistake you are most likely to make, and why |
| **Challenge** | Two minutes, optional, harder |
| **In the real world** | Where this shows up on a real team |

Starter files carry numbered markers, `// TODO(lab-2.1)`, so you always know where you are. Search for
them (`Ctrl/Cmd+Shift+F`) if you get lost.

**This demo is different from the others in one way:** most of its code is pre-built. Demo 0 built the
domain (money, prices, rules, baskets); this starter adds the other modules, the configuration loader
and the HTTP layer, finished. Your job today is to read, run, break and measure, and to type the few
things that make the problems visible. From Demo 2 on, you type the fixes.

**The finished version of this demo is the next demo's starter.** If you fall behind, open
`../02-the-container-takes-over-the-wiring/starter` and diff. (For this review build, the finished
version is also in `./solution`.)

---

## Orientation — what is in the box (5 min)

```
starter/
├── pom.xml                       one module, no framework; JUnit and AssertJ for tests, the PostgreSQL driver for Demo 0's Lab 8
├── PROBLEMS.md                   the problem register. Empty. Lab 9 fills it in.
├── http/monolith.http            the requests you will send today
└── src/main/java/com/shopscope/monolith/
    ├── ShopScopeMonolith.java    main(): the composition root. Read this first.
    ├── SeedData.java             four products, their prices, three discount rules, stock, two customers
    ├── shared/                   Money (Demo 0)
    ├── catalog/                  Product, CatalogRepository (a map), CatalogService (Demo 0)
    ├── pricing/                  prices, scopes, discount rules, PricingService, TaxRules (Demo 0, plus TaxRules)
    ├── order/                    Basket and LineItem (Demo 0); Order, OrderRepository, OrderService (new)
    ├── inventory/                InventoryService: stock in a map
    ├── customer/                 Customer, CustomerService: customers in a map
    ├── payment/                  PaymentGateway, PaymentResult, RazorpayTestGateway: a stand-in for Razorpay's test mode
    ├── notification/             NotificationService: "sends" e-mail by logging it
    ├── config/                   AppConfig: shopscope.properties, parsed by hand
    └── http/                     ShopHttpServer and Json: the HTTP layer, by hand
```

Three things to notice before we start:

**`ShopScopeMonolith.main` is sixty lines of `new`.** Every service is constructed there, in an order
that cannot be changed. That file is the composition root, and it is today's main character.

**Every store is a `Map`.** `CatalogRepository`, `OrderRepository`, `InventoryService.stock`,
`CustomerService.byId`: four maps, one process. There is no database, so there is no transaction, so
there is nothing to roll back. Remember that for Lab 6.

**The routes are already the real ones.** `/api/products`, `/api/inventory/{sku}`, `/api/checkout`: the
same paths the microservices serve at the end of the track. The contract stays; everything behind it changes.

---

## Lab 1 — Tour: the seven modules, their dependencies, and the arrows that should not exist (15 min)

### Problem

Seven packages, twenty-odd classes. Before you can judge the design you need the map: which module
depends on which, and which of those dependencies are a choice and which are an accident.

### Concept

**A module is a package that owns one kind of thing.** `catalog` owns products, `inventory` owns stock,
`order` owns baskets and orders. Inside a module, classes may know each other freely. Between modules,
every dependency is an arrow in a graph, and the shape of that graph is the architecture, whatever the
diagrams say.

Two rules tell a good arrow from a bad one. **Point towards stability:** depending on `shared.Money` is
fine because `Money` will never change for business reasons; depending on `notification` from `order`
is not, because e-mail changes every quarter. **Point towards the thing you need the answer from:**
checkout needs a price, so `order → pricing` is right; checkout does not need an e-mail to have been
sent, so `order → notification` is a direct call that should be an event.

### Steps

1. **Draw the graph.** On paper or in a text file, one box per package under `com.shopscope.monolith`.
   For each box, open its classes and list every `import com.shopscope.monolith.<other>` you see. Draw an
   arrow per import. `shared` is imported by everyone; draw it once at the bottom.

2. **Find the two arrows that should not exist.** One of them is `order → notification`. Open
   `order/OrderService.java` and find the line with the comment `problem 1`. Read the javadoc at the top
   of the class while you are there; it is the agenda for the day.

3. **Find the arrow that is missing.** `http` depends on `catalog`, `inventory` and `order`. Which module
   does `http/ShopHttpServer.java` *not* import, even though it answers `/api/checkout` with a total?
   (Pricing. The server never sees a price; `OrderService` does the pricing. Decide whether that is right.)

### Verify

Your graph has `order` with the most outgoing arrows (pricing, inventory, customer, notification,
payment) and `shared` with the most incoming. You have marked `order → notification` and
`order → payment` (via `new`, Lab 3) as the two to fix.

### Watch out

**Counting imports of `java.*` as dependencies.** Only `com.shopscope.monolith.*` imports are module
arrows. `java.util.Map` is not architecture.

**Thinking the graph is wrong because it has many arrows.** `order` must depend on four things; that is
what checkout is. The question is never "how many arrows", it is "which direction, and is it a call or
an event".

### Challenge

`inventory` has no outgoing arrows at all. Is that because it is well designed, or because it does too
little? What would it need to import to support "reserve now, confirm after payment"?

### In the real world

Teams draw this graph with a tool (ArchUnit, in the Testing track) and make the build fail when a new
arrow appears. In a monolith the compiler allows every arrow; the discipline has to come from somewhere
else. That is the main argument people make for microservices, and it is a weaker argument than it sounds
(you can enforce it inside one process), which is why Demos 2 to 11 stay in one process.

---

## Lab 2 — The composition root: wire the object graph by hand, then add one dependency and count the files you touch (20 min)

### Problem

`OrderService` stamps orders with `Instant.now()`. The test you will write in Lab 3 needs a fixed time.
The fix is a two-line change in one class: inject a `Clock`. Let us see what that two-line change costs
in a hand-wired system.

### Concept

**The composition root is the one place where the object graph is built.** In this monolith it is
`main()`. Every service declares what it needs in its constructor; `main()` satisfies every constructor,
in dependency order, with `new`. That is dependency injection, done by hand, and there is nothing wrong
with it at this size.

What is wrong is the ripple. Add a constructor parameter to a service and three things change: the
service, the composition root, and every test that constructs the service. The compiler finds all of
them for you, which is the good news; the bad news is that it is always all of them. A container
(Demo 2) does not remove the dependency; it removes the ripple.

### Steps

1. **`TODO(lab-2.1)`, in `order/OrderService.java`.** Add a `Clock` parameter to the constructor and
   keep it in a field:

   ```java
   import java.time.Clock;
   // ...
   private final Clock clock;

   public OrderService(PricingService pricing,
                       InventoryService inventory,
                       OrderRepository orders,
                       CustomerService customers,
                       NotificationService notifications,
                       Clock clock) {
       // ... existing assignments ...
       this.clock = clock;
   }
   ```

2. **`TODO(lab-2.2)`, same file.** Replace `Instant.now()` with `clock.instant()` and remove the
   `java.time.Instant` import.

3. **`TODO(lab-2.3)`, in `ShopScopeMonolith.java`.** The build is now broken; read the compiler error
   before you fix it. Then pass `Clock.systemUTC()` as the last argument and import `java.time.Clock`.

4. **Count.** `git status` or your editor's changed-files list. Two files for a change whose business
   meaning is "make time injectable". In Lab 3 it becomes three.

5. **Now try to reorder `main()`.** Move the `OrderService orders = ...` line above `PricingService
   pricing = ...`. Read the error. Put it back. The order of those lines is the dependency graph of Lab 1,
   written as statements.

### Verify

`./mvnw -q verify` passes. `./mvnw -q exec:java -Dexec.args=--demo` prints the same order as before.
Nothing has changed for the customer; two files changed for the developer.

### Watch out

**Reaching for `Clock.systemDefaultZone()`.** Orders are timestamps, not wall clocks; use UTC and convert
at the edge. This is a small habit with a large payoff the first time two regions share a database.

**Making `clock` a static field "so nothing else has to change".** You would have removed the ripple by
making the dependency invisible. Invisible dependencies are the subject of Lab 3.

### Challenge

The composition root also decides *which* `TaxRule` is used, by calling `TaxRules.forCountry(config.country())`.
How many files change if the UK store needs a different rule for food? (Hint: start in `pricing/TaxRules.java`.)

### In the real world

Every Spring Boot application still has a composition root. It is the container, and `@Configuration`
classes are the lines of `main()` with the ordering worked out for you. Teams who have never wired a graph
by hand tend to treat the container as magic; the point of this lab is that it is bookkeeping.

---

## Lab 3 — Try to unit-test `OrderService`: the hidden `new RazorpayTestGateway()` (15 min)

### Problem

You want a test that checks out a kettle and asserts three things: an order is saved, stock dropped by
one, one e-mail went out. Everything `OrderService` needs has a constructor you can call. Write the test
and see where it stops being yours.

### Concept

**A dependency you can see in the constructor can be replaced. A dependency created with `new` inside
the class cannot.** `OrderService` takes five collaborators through its constructor and creates the sixth,
the payment gateway, itself: `private final PaymentGateway gateway = new RazorpayTestGateway();`.

That one `new` means every test of checkout is also a test of the gateway's rules. You cannot make a
payment fail except by knowing that the fake gateway declines cards ending in `0000`. When Demo 23 puts the
real Razorpay behind that interface, the same line would make every test call the internet.

The fix is one you already know from Lab 2: move it to the constructor. The reason it is not done here is
that this lab is about feeling the problem, and because the fix is Demo 2's first lab.

### Steps

1. **`TODO(lab-3.1)`, in `src/test/java/.../order/OrderServiceTest.java`.** Build the collaborators in
   `wireByHand()`. `SeedData` has the data:

   ```java
   inventory = new InventoryService();
   SeedData.stock(inventory);
   CustomerService customers = new CustomerService();
   SeedData.customers(customers);
   orders = new OrderRepository();
   notifications = new NotificationService();
   PricingService pricing = new PricingService(SeedData.prices(), SeedData.discountRules(), new FlatRateTax("GST", 18));
   service = new OrderService(pricing, inventory, orders, customers, notifications, FIXED);
   ```

   Add fields for `inventory`, `orders` and `notifications`, and a fixed clock:
   `Clock.fixed(Instant.parse("2026-10-29T10:00:00Z"), ZoneOffset.UTC)`. This is file number three for
   the Lab 2 change.

2. **`TODO(lab-3.2)`.** Remove `@Disabled` and write the happy path:

   ```java
   Basket basket = Basket.empty("c-1").add(new LineItem(SeedData.KETTLE, "Kettle", 1));
   Order order = service.checkout(basket, PriceContext.of("INR", "IN"), "4111111111111111");

   assertThat(order.placedAt()).isEqualTo(FIXED.instant());
   assertThat(orders.findById(order.id())).isPresent();
   assertThat(inventory.available(SeedData.KETTLE)).isEqualTo(1);
   assertThat(notifications.sent()).singleElement()
           .satisfies(email -> assertThat(email.to()).isEqualTo("priya@example.com"));
   ```

3. **`TODO(lab-3.3)`.** Now the decline. Try to write it *without* knowing anything about
   `RazorpayTestGateway`. You cannot: there is no way to hand `OrderService` a gateway that says no. Use
   the only lever you have, a card ending in `0000`, and write the words "coupled to the gateway's rules" in
   a comment above the test:

   ```java
   assertThatThrownBy(() -> service.checkout(basket, PriceContext.of("INR", "IN"), "4111111111110000"))
           .isInstanceOf(PaymentDeclinedException.class);
   assertThat(orders.count()).isZero();
   assertThat(notifications.sent()).isEmpty();
   ```

### Verify

`./mvnw -q verify`: both tests pass. Open `OrderServiceTest` and count what you typed to get a service
under test: eight lines of wiring for a three-line test. Open `RazorpayTestGateway` and confirm the `0000`
rule your test now depends on.

### Watch out

**"I will just make the gateway a constructor parameter now."** You can, and Demo 2 does. But do the
Lab 2 count first: service, main, test. Then ask how the Lab 7 HTTP handler would obtain the gateway.
The ripple is the lesson.

**Asserting on log output.** `NotificationService.sent()` exists so you do not have to. If the only way
to observe a behaviour is a log line, the class needs a seam.

### Challenge

`PricingService` is also built by hand in the test, with real seed prices. Is that a problem? When would
it become one? (Answer: when a price change in `SeedData` breaks an order test that was never about prices.)

### In the real world

Grep a legacy code base for `= new .*Service(` and `= new .*Client(` inside fields. Each hit is a class
that cannot be tested without the thing it creates. That grep is often the first day of a migration to
Spring, and the list it produces is the migration plan.

---

## Lab 4 — Configuration by hand: the properties loader, hard-coded tax rates, and what a second environment costs (15 min)

### Problem

The UK store needs to run the same code in GBP with VAT. Today that is a text file, a system property
and a code change.

### Concept

**Configuration is every value that differs between environments and must not require a rebuild.** Port,
currency, country, tax rates, feed delays. The monolith keeps them in `shopscope.properties`, loads the
file in `AppConfig.load()`, and parses each value where it is read: `Integer.parseInt` in `port()`,
`Long.parseLong` in `catalogSeedDelayMillis()`. There is no validation (a port of `"eighty"` fails at
start-up with a stack trace from inside a getter), no typing, and the tax rates are not in the file at all:
they are a `switch` in `pricing/TaxRules.java`.

### Steps

1. **Read `config/AppConfig.java`** top to bottom. Note the two places a value can come from (classpath,
   then an override file named by `-Dshopscope.config=`) and that the override is *merged*, not replaced.

2. **Make a UK environment.** Create `uk.properties` next to `pom.xml`:

   ```properties
   server.port=8081
   shop.country=GB
   shop.currency=GBP
   ```

   Run it: `./mvnw -q exec:java -Dexec.args=--demo -Dshopscope.config=uk.properties`. The quote is in GBP
   with 20% VAT. Nothing in the code mentioned the UK; `TaxRules.forCountry("GB")` already had a branch.

3. **Now add a country that is not in the `switch`.** Change `shop.country=DE`. Run it. Read the exception.
   To ship Germany you edit `TaxRules.java` and rebuild. Tax rates are configuration pretending to be code.

4. **Break the port.** `server.port=eighty`. Run without `--demo`. Where does it fail, and how far into
   start-up? (In `port()`, after the catalogue has loaded, with a `NumberFormatException` that names no
   property.)

### Verify

Three runs: GBP with VAT, a failure for DE naming "No tax rule for country DE", a `NumberFormatException`
for the port. Delete `uk.properties` afterwards or add it to `.gitignore`; it is yours, not the project's.

### Watch out

**Putting the override file inside `src/main/resources`.** It would be packaged into the jar and win in
every environment. Environment files live outside the artifact; that is the whole idea.

**Fixing `TaxRules` now by reading the rate from the properties file.** It is the right instinct and the
wrong moment; Demo 3 does it with profiles, which also solves "the UK rule for food", and Demo 4 types
and validates it with `@ConfigurationProperties`.

### Challenge

List every value in the code base that should be configuration and is not. Start with `RazorpayTestGateway`
(the decline limit) and `SeedData` (is seed data configuration?).

### In the real world

Twelve-factor's third rule is "config in the environment". Spring Boot implements it with a precedence
list eighteen levels deep (command line beats environment variables beat profile files beat defaults).
Demo 4 shows the list; this lab is why it exists.

---

## Lab 5 — Cross-cutting by hand: timing and audit logging copied into every service method (10 min)

### Problem

Every service method starts with `long started = System.nanoTime();` and ends with a log line that
computes elapsed milliseconds. The business logic is in the middle, with a frame of plumbing around it.

### Concept

**A cross-cutting concern is behaviour that applies to many methods and belongs to none of them.** Timing,
logging, security checks, transactions, caching, retries. Written by hand, the same four lines appear in
every method, drift apart as people edit them, and get forgotten in new methods. Written once as an
*aspect* (Demo 3), they apply to every method that matches a pattern, and the business code is only
business code.

### Steps

1. **Count the copies.** From the starter folder:

   ```bash
   grep -rn "System.nanoTime()" src/main/java | wc -l
   grep -rn "took=" src/main/java
   ```

2. **Find the drift.** `CatalogService` has a private `elapsedMillis` helper; `PricingService` and
   `OrderService` do the division inline; `InventoryService.release` has no timing at all. Three styles of
   the same concern in four classes.

3. **Add a method and forget.** Add `public List<Order> history(String customerId)` to `OrderService`
   (filter `orders.findAll()` by customer id, return the list). Do not add timing. Nobody will notice until
   the one day it is slow. That is the failure mode: not wrong logs, missing ones.

### Verify

Your grep counts match what you find by reading. Your new `history` method compiles and has no plumbing,
which is both correct and, in this monolith, inconsistent.

### Watch out

**Treating logging as free.** `LOG.log(Level.INFO, "...", sku, found, elapsed)` builds the message only if
INFO is enabled, which is why the arguments are passed separately. String concatenation inside the call
would pay the cost even when the level is off.

### Challenge

What else is cross-cutting in this code base and not yet written at all? (Authorisation: nothing checks
that `c-1` may see `c-1`'s orders. Validation: `quantity` is checked in `LineItem`, nowhere else.)

### In the real world

Observability teams ask for one thing: the same fields on every log line and every span. Hand-written
timing never achieves that across forty services. Demo 22 gets it from Micrometer and OpenTelemetry with
zero lines in business code.

---

## Lab 6 — The shared in-memory store and the overselling bug: a payment that fails after stock was decremented (20 min)

### Problem

Two kettles in stock. A customer checks out one with a card that gets declined. How many kettles are in
stock now?

### Concept

**Checkout is four writes that must all happen or none:** reserve stock, take payment, save the order, send
the confirmation. `OrderService.checkout` does them in that order, in plain code, against maps. When step
two fails, step one has already happened and nothing undoes it. There is no `rollback()` to call because
there is no transaction, and there is no transaction because there is no database: a `HashMap` has no
idea what an "all or nothing" is.

This is the single most expensive class of bug in commerce systems, and the monolith has it in the first
hundred lines. Demo 7 gives the modules a database, Demo 10 wraps checkout in a transaction, Demo 13 turns
"decrement" into "reserve, then confirm or release" across services.

### Steps

1. **Reproduce it over HTTP.** Start the server (`./mvnw -q exec:java`) and, in another terminal:

   ```bash
   curl -s localhost:8080/api/inventory/SKU-KET-001
   curl -s -X POST localhost:8080/api/checkout \
        -d '{"customerId":"c-1","sku":"SKU-KET-001","quantity":1,"card":"4111111111110000"}'
   curl -s localhost:8080/api/inventory/SKU-KET-001
   ```

   Available: 2, then a 422 "Payment declined", then available: 1. Stop the server.

2. **`TODO(lab-6.1)`, in `src/test/java/.../ProblemRegisterTest.java`.** Write the behaviour ShopScope
   *should* have, as a test:

   ```java
   @Test
   void problem6_aDeclinedPaymentLeavesStockUntouched() {
       InventoryService inventory = new InventoryService();
       SeedData.stock(inventory);
       CustomerService customers = new CustomerService();
       SeedData.customers(customers);
       PricingService pricing = new PricingService(SeedData.prices(), SeedData.discountRules(), new FlatRateTax("GST", 18));
       OrderService service = new OrderService(pricing, inventory, new OrderRepository(), customers, new NotificationService(), Clock.systemUTC());
       Basket basket = Basket.empty("c-1").add(new LineItem(SeedData.KETTLE, "Kettle", 1));

       assertThatThrownBy(() -> service.checkout(basket, PriceContext.of("INR", "IN"), "4111111111110000"))
               .isInstanceOf(PaymentDeclinedException.class);

       assertThat(inventory.available(SeedData.KETTLE)).as("stock after a declined payment").isEqualTo(2);
   }
   ```

3. **Run it and watch it fail.** `./mvnw -q verify`: `expected: 2 but was: 1`. That failure is the bug,
   stated precisely. Do not fix it.

4. **Park it.** Add `@Disabled("Problem 6 (the overselling bug): fixed in Demo 10 with a transaction")`
   above the test. The register is now executable: Demo 10 removes the annotation and the test goes green.

5. **Read the two-tab version.** `InventoryService.reserve` checks `available < units` and then writes.
   Two requests arriving together both pass the check. That is a second bug in the same four lines, and it
   needs a lock, not a transaction. Demo 10 again.

### Verify

`./mvnw -q verify` is green with the new test skipped. `curl` showed 2, then 1, with no order created.

### Watch out

**"Just call `inventory.release()` in the `Declined` branch."** Try it: it fixes this path and leaves the
one where `orders.save` throws, and the one where the JVM dies between the two lines. Compensation written
by hand is a list that is never complete; a transaction is the list.

**Fixing it by charging first and reserving after.** Now a successful payment can meet an empty shelf.
You have moved the bug, not removed it.

### Challenge

Make the bug happen with two threads: two checkouts of the last kettle at once, both with good cards.
Use `QuoteBatch`'s executor idea with two `Callable`s. How often do both succeed?

### In the real world

Every e-commerce team has a story about the day a promotion sold more units than existed. The fix is
always the same three things, in this order: a database, a transaction around the write set, and a
reservation model with a timeout. This track teaches them in that order.

---

## Lab 7 — The hand-rolled HTTP layer: run it, call it with curl, break it with malformed JSON (15 min)

### Problem

`ShopHttpServer` is 120 lines and answers four routes. It took an afternoon to write and it is already
wrong in ways a customer will find in a minute.

### Concept

**An HTTP layer has six jobs, and none of them are your business:** route a path to code, parse the
body, validate it, call the domain, serialise the response, choose a status code and map errors to it.
The monolith does all six by hand: string comparison for routes, a regular expression for JSON
(`Json.parseFlatObject`), no validation, a `switch` of `catch` blocks for errors, and a `500` whose body
is the exception's `toString()`. Spring MVC (Demo 5) does the first five with annotations and Jackson,
and Demo 6 gives the sixth one shape (`ProblemDetail`).

### Steps

1. **Start the server** and run the requests in `http/monolith.http`, or with `curl`:

   ```bash
   curl -s -i localhost:8080/api/products/SKU-NOPE | head -1          # 404, chosen by hand
   curl -s -X POST localhost:8080/api/checkout \
        -d '{"customerId":"c-1","sku":"SKU-KET-001","quantity":"one","card":"4111111111111111"}'
   ```

   A `500` whose body is `java.lang.NumberFormatException: For input string: "one"`. The client learns
   our language and our class names. A `422` with "quantity must be a number" is what it should get.

2. **Send a nested object.** `{"customer":{"id":"c-1"},"sku":"SKU-KET-001","quantity":1,"card":"4111111111111111"}`.
   The regex parser reads `"id":"c-1"` as a top-level field and `customerId` is missing: a `500` with a
   `NullPointerException` deep inside `CustomerService`. Open `http/Json.java` and find the pattern that
   makes this inevitable.

3. **Send the wrong method.** `curl -s -i -X PUT localhost:8080/api/products`. A `405` because someone
   remembered to check; now try `PUT /api/inventory/SKU-KET-001`. (Nobody checked: it answers as if it
   were a `GET`.)

4. **Find the status codes.** `grep -n "send(exchange, " src/main/java/com/shopscope/monolith/http/ShopHttpServer.java`.
   Every number was chosen by a person on a particular afternoon.

### Verify

You have seen a `404`, a `422`, two different `500`s and an inconsistent `405`. You can point at the line
in `Json.java` that misreads nested JSON (the `FIELD` pattern) and at the `catch (RuntimeException e)`
that turns every programming error into a customer-facing stack trace.

### Watch out

**Rating this layer on the happy path.** It serves the four products perfectly. HTTP layers are judged on
the other paths.

**Blaming `HttpServer`.** The JDK's server is fine; it is a socket with a dispatcher. Everything wrong
here is the code written on top of it, and the list of what it lacks is the Demo 5 syllabus.

### Challenge

Make the parser read `"quantity": 1` with a space after the colon. Then `"quantity":1.5`. Each fix is
another regex; stop when you can see where this ends.

### In the real world

Nobody writes this layer by hand on purpose. But teams inherit it, in older code bases and in "lightweight"
services that started as a script. Being able to list exactly what a framework replaces is how you make
the case to replace it.

---

## Lab 8 — Start-up and shutdown by hand: ordering, and a resource that never closes (10 min)

### Problem

Start the server and immediately ask for the products. You get `[]`. Two seconds later you get four.
Press Ctrl+C and nothing says what happened to the orders in memory.

### Concept

**Start-up is a dependency graph too, with time in it.** The catalogue "feed" takes 1.5 seconds to load
(`catalog.seed.delay-ms` in the properties, simulating a supplier file). `main()` starts the HTTP server
first and then loads the catalogue, so for 1.5 seconds the server answers questions about an empty
catalogue. Nothing is wrong with any line; the order of two lines is wrong.

**Shutdown is the same graph, reversed.** Stop accepting requests, finish the ones in flight, flush what
must be flushed, close what must be closed. By hand that is a shutdown hook that has to remember every
resource; forget one and the JVM exits with work half done, or does not exit at all.

### Steps

1. **See the window.** `./mvnw -q exec:java` and, within a second, `curl -s localhost:8080/api/products`.
   `[]`. Again after two seconds: four products.

2. **`TODO(lab-8.1)`, in `ShopScopeMonolith.java`.** Move `seedCatalogue(...)` above `http.start(...)`
   so the server only starts once the data it serves exists. Keep the `--demo` branch working. Re-run
   step 1: the first request blocks for a moment (the server is not listening yet) and then returns four
   products. "Not yet listening" is the right answer; "listening and wrong" is not.

3. **`TODO(lab-8.2)`.** Add a shutdown hook after `http.start(...)`:

   ```java
   Runtime.getRuntime().addShutdownHook(new Thread(() -> {
       http.stop();
       LOG.log(Level.INFO, "shutdown orders={0} emails={1}", orderRepository.count(), notifications.sent().size());
   }, "shutdown"));
   ```

   Run, place an order with `curl`, press Ctrl+C. The last log line reports one order and one e-mail
   that will never exist again.

### Verify

No empty-list window at start-up; a `shutdown orders=1 emails=1` line at Ctrl+C.

### Watch out

**Doing real work in a shutdown hook.** Hooks run concurrently, with no ordering, and the JVM gives up on
them after a while. Flushing a counter to a log is fine; "save everything to disk" in a hook is how data is
lost. Demo 4's Boot has `SmartLifecycle` phases and graceful shutdown for the real version.

**Starting the server late and calling it a fix.** It is one, for this symptom. A readiness probe
(Demo 21) is the general answer: the process is up, and separately it is ready.

### Challenge

Make the seed take ten seconds and watch Kubernetes-style behaviour in your head: a load balancer that
sends traffic the moment the port opens would have served `[]` for ten seconds. How would you tell it to
wait?

### In the real world

Start-up order bugs are the ones that only appear in production, because locally the data is already
there. Boot's lifecycle and the readiness probe exist to make "ready" a fact the platform can see, not a
timing you hope for.

---

## Lab 9 — The problem register: every pain in this demo, and the demo that fixes it (10 min)

### Problem

You have seen nine problems. Without a written list they become "the monolith is bad", which is wrong
and unhelpful. With one, each later demo has a reason to exist before it starts.

### Concept

**The register is the contract between this demo and the rest of the track.** Each row: a problem,
where you saw it, which demo closes it. It is re-read at the end of every part. The goal of the track
is an empty table, and the test you parked in Lab 6 is the first row that can be checked by a machine.

### Steps

1. **`TODO(lab-9.1)`, in `PROBLEMS.md`.** Fill in the table. Use your own words for the problem column;
   here is the reference version to check against:

   | # | Problem | Where you saw it | Fixed in |
   |---|---|---|---|
   | 1 | Modules call each other directly (order → notification) | Lab 1, Lab 6 | Demo 3, Demo 13 |
   | 2 | Wiring by hand, and a hidden `new` dependency | Lab 2, Lab 3 | Demo 2 |
   | 3 | Nothing is testable in isolation | Lab 3 | Demo 2, Demo 11 |
   | 4 | Configuration by hand, tax rates in code | Lab 4 | Demo 3, Demo 4, Demo 14 |
   | 5 | Cross-cutting code pasted everywhere | Lab 5 | Demo 3, Demo 22 |
   | 6 | Shared in-memory store; the overselling bug | Lab 6 | Demo 7, Demo 10, Demo 13 |
   | 7 | One process | Lab 1, Lab 8 | Demo 12, Demo 13, Demo 16 |
   | 8 | The hand-rolled HTTP layer | Lab 7 | Demo 5, Demo 6 |
   | 9 | Start-up and shutdown by hand | Lab 8 | Demo 2, Demo 4 |

2. **Mark the executable row.** Next to row 6 write "`ProblemRegisterTest`". When a later demo closes a
   row that can be tested, it adds a test there and removes the `@Disabled`.

3. **Say what is not on the list.** The monolith is *not* slow, *not* unreadable, and *not* too big.
   Write that under the table. Half the microservice migrations in the industry are started for those
   three reasons, and this track is not one of them.

### Verify

`PROBLEMS.md` has nine rows and a sentence about what is fine. Compare with `solution/PROBLEMS.md`.

### Watch out

**Writing "use Spring" as the fix for every row.** Row 6 is fixed by a database and a transaction, which
is not Spring. Row 7 is fixed by extraction, which is not Spring either. Spring makes the fixes cheaper;
it is not the fix.

### Challenge

Add a tenth row for something you found that this guide did not. Security is the obvious one: nothing
checks who is asking. Which demo fixes it?

### In the real world

Architecture reviews that produce a register like this get funded; reviews that produce "we should move
to microservices" get a workshop. The register is also the retrospective: at the end of the track you
read it again and ask which fixes were worth their cost.

---

## Wrap-up — what you can now do

- Draw the dependency graph of a code base from its imports, and tell a call that should be an event
- Explain the composition root, and count the cost of a new dependency in a hand-wired system
- Spot a hidden dependency (`new` in a field) and say exactly why it blocks testing
- Separate configuration from code, and name what is still code in disguise
- Recognise cross-cutting concerns by their duplication
- Reproduce a lost-update bug over HTTP and state it as a failing test
- List what an HTTP framework does, by having done it by hand
- Order start-up and shutdown correctly, and explain why "ready" is not "started"
- Keep a problem register and use it to justify every change that follows

## Next demo

[Demo 2 — The container takes over the wiring](../02-the-container-takes-over-the-wiring/): the sixty
lines of `new` become `@Configuration` and `@Bean`, `OrderService` gets its gateway through the
constructor, and the first test with a fake payment provider goes green. Rows 2, 3 and 9 of the register.

---

## Interview questions

1. **What is a composition root?** The one place in an application where the object graph is assembled.
   In plain Java it is `main()`; in Spring it is the container, configured by `@Configuration` classes.
2. **What is dependency injection, without a framework?** Passing a class's collaborators in through its
   constructor instead of creating them inside it. The framework only automates who calls the constructor.
3. **Why is a `new` inside a field a testability problem?** The dependency cannot be replaced, so every
   test of the class is also a test of that dependency, with its rules, its network calls and its state.
4. **Constructor injection or field injection?** Constructor: the dependencies are visible, required and
   final, the class can be built in a test without a container, and a circular dependency fails at start-up
   instead of at first use.
5. **What is a cross-cutting concern? Give three.** Behaviour that applies to many methods and belongs to
   none: logging, timing, transactions, security, caching, retries.
6. **Why can an in-memory map not be "transactional"?** Transactions are a property of a store that can
   undo a group of writes together. A map has single writes and no undo; atomicity across several maps
   would need a log and a lock written by hand.
7. **What is the lost-update race in `InventoryService.reserve`?** Check-then-act: two threads read
   `available = 2`, both pass `available < 1`, both write `1`. Fixed with a lock or an atomic
   compare-and-set, not with a transaction alone.
8. **Monolith, modular monolith, microservices: one sentence each.** One process with modules that may
   call anything; one process whose modules may only call through declared boundaries; several processes
   whose boundaries are the network.
9. **What does a web framework do that this HTTP layer did by hand?** Routing, body parsing and
   serialisation, validation, error-to-status mapping, content negotiation, and the parts nobody writes
   by hand at all (HTTP/2, compression, keep-alive handling).
10. **What is the difference between "started" and "ready"?** Started: the process is running and the
    port is open. Ready: it can answer correctly, because the data and connections it needs exist. Load
    balancers must route on the second, which is why readiness probes exist.
11. **Why keep configuration out of the artifact?** So one build can run in every environment and a
    production change is a config change, not a release. It is also why secrets never live in the jar.
12. **Give one reason not to split this monolith into services yet.** The modules do not have clean
    boundaries or tests; splitting now would make the network enforce boundaries that should first be
    enforced in code. Demos 2 to 11 do that first.

## Troubleshooting

**`./mvnw: permission denied`.** `chmod +x mvnw`, or run `sh mvnw ...`. Git on Windows sometimes drops the
execute bit; `mvnw.cmd` works there.

**`Address already in use` on 8080.** Another process has the port. `lsof -i :8080` to find it, or set
`server.port=8090` in a local override file (Lab 4).

**`curl` prints nothing.** The server is still loading the catalogue (Lab 8), or you ran `exec:java` with
`--demo`, which exits after one checkout.

**The build downloads for a long time the first time.** The wrapper fetches Maven 3.9.16 and the
dependencies once into `~/.m2`. Later builds take seconds.

**`java -version` says 11.** The project needs 17 or newer. Install a JDK 17+ and point `JAVA_HOME` at it;
`./mvnw -v` shows which one Maven is using.
