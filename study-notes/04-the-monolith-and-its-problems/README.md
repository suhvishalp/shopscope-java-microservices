# Module 04 — The monolith and its problems

**Study notes** · ~2–3 hours

> **Goal.** Read the ShopScope monolith of Demo 1 the way a reviewer would: name each thing that
> will not keep working, say precisely why, and know which later demo removes it. This module
> is the thread the whole track follows, so the problem register at the end is worth
> memorising.

**Prerequisites:** [Module 01](../01-modern-java-for-spring/) (sealed types, `Optional`,
exceptions), [Module 03](../03-data-and-the-network-before-frameworks/) §6 and §7 for the HTTP
layer. Have the Demo 1 starter open; every section points at a file.

---

## Contents

1. [What a monolith is, and is not](#1-what-a-monolith-is-and-is-not)
2. [The seven modules and their dependency graph](#2-the-seven-modules-and-their-dependency-graph)
3. [The composition root: wiring by hand](#3-the-composition-root-wiring-by-hand)
4. [Hidden dependencies and testability](#4-hidden-dependencies-and-testability)
5. [Configuration by hand](#5-configuration-by-hand)
6. [Cross-cutting code, pasted everywhere](#6-cross-cutting-code-pasted-everywhere)
7. [The shared in-memory store and the overselling bug](#7-the-shared-in-memory-store-and-the-overselling-bug)
8. [One process](#8-one-process)
9. [The hand-rolled HTTP layer](#9-the-hand-rolled-http-layer)
10. [Start-up order and shutdown](#10-start-up-order-and-shutdown)
11. [Two ways out: modular monolith and microservices](#11-two-ways-out-modular-monolith-and-microservices)
12. [The problem register](#12-the-problem-register)
13. [Self-check](#13-self-check)
14. [References](#14-references)

---

## 1. What a monolith is, and is not

A **monolith** is an application deployed as one unit: one build, one process, one thing to
start and stop. That is all the word means. It says nothing about code quality. A monolith can
be beautifully organised into modules with clean boundaries, and most successful systems started
as one, because a single process is the simplest thing that works: no network between modules,
one transaction across all the data, one log to read, one thing to deploy.

So the first thing to be honest about: **the problems in this module are problems of how this
monolith is built, not of monoliths.** Hand wiring, hidden dependencies, pasted logging, a
hand-rolled HTTP layer and a shared map pretending to be a database would be just as wrong in a
microservice. Several of them are fixed in Demos 2 to 11 without splitting anything. The one
problem that is genuinely about being a monolith, "one process", is the last to go, and only
after the modules are clean enough to move.

What the ShopScope monolith *is*: the whole backend in miniature. Seven modules that will each
become a service, behind the same `/api` paths those services will serve, so that the system
you see on day one is the system you finish with, just arranged differently.

---

## 2. The seven modules and their dependency graph

Under `com.shopscope.monolith`:

| Module | Owns | Key classes |
|---|---|---|
| `catalog` | products | `Product`, `CatalogRepository`, `CatalogService` |
| `pricing` | prices, discounts, tax, quotes | `PriceList`, `DiscountRule`, `Discounts`, `TaxRules`, `PricingService` |
| `inventory` | stock per SKU | `InventoryService` |
| `customer` | shoppers and their group | `Customer`, `CustomerService` |
| `order` | baskets, checkout, orders | `Basket`, `OrderService`, `OrderRepository` |
| `payment` | charging a card | `PaymentGateway`, `RazorpayTestGateway`, `PaymentResult` |
| `notification` | e-mail | `NotificationService` |

plus `shared` (`Money`), `config` (`AppConfig`), `http` (`ShopHttpServer`, `Json`) and the
root (`ShopScopeMonolith`, `SeedData`).

Draw the arrows by reading the imports of each service:

```
order ──> pricing ──> (shared)
order ──> inventory
order ──> customer
order ──> payment
order ──> notification        <── the arrow that should not exist
http  ──> catalog, inventory, order
pricing, inventory, customer, catalog, notification: depend on nothing else
```

Most arrows are fine. Checkout genuinely needs a quote, the stock, the customer and a payment;
those are its collaborators. The arrow from `order` to `notification` is different. Sending the
confirmation e-mail is not part of placing an order; it is something that should happen
*because* an order was placed. As written, `OrderService.checkout` calls
`notifications.orderConfirmation(customer, order)` directly, after the money has been taken.
If that call threw, the shopper would be charged and get an exception. The notification module
is a consequence, not a collaborator, and Demo 3 turns the call into an event
(`OrderPlaced`) that the notification module listens for.

The second thing the graph shows: `catalog` is not used by `order` at all. Checkout trusts the
SKU and name in the basket. That is a shortcut that later demos (12, 13) replace with a product
snapshot fetched at checkout.

---

## 3. The composition root: wiring by hand

Every service has a constructor that takes what it needs. Someone has to call those
constructors in the right order. In the monolith that someone is `ShopScopeMonolith.main`, the
**composition root**:

```java
CatalogService catalog = new CatalogService(catalogRepository);
TaxRule taxRule = TaxRules.forCountry(config.country());
PricingService pricing = new PricingService(priceList, SeedData.discountRules(), taxRule);
NotificationService notifications = new NotificationService();
OrderService orders = new OrderService(pricing, inventory, orderRepository, customers, notifications, Clock.systemUTC());
```

The order of these lines is **load-bearing**: `OrderService` needs a `PricingService`, so the
pricing line must come first. Swap them and it does not compile. For five services that is
manageable. For fifty, with some needing each other in long chains, `main` becomes a page that
nobody wants to touch and everybody must.

The real cost shows when a dependency is added. Lab 2 of Demo 1 injects a `Clock` into
`OrderService` so that tests can fix the time. That change touches:

1. `OrderService`: a new constructor parameter and a field.
2. `ShopScopeMonolith`: the `new OrderService(...)` call.
3. Every test that constructs an `OrderService`.

Three files for one new collaborator, and the compiler will point at each one. Now imagine the
dependency is added to `PricingService`, which `OrderService` and `QuoteBatch` and three tests
construct. The composition root is where a hand-wired application spends its maintenance
budget.

A container (Demo 2) does this job by reading the constructors: it sees that `OrderService`
needs a `PricingService`, finds or creates one, and builds the graph in the right order by
itself. Adding a parameter becomes a change in one file.

---

## 4. Hidden dependencies and testability

Open `OrderService`:

```java
private final PaymentGateway gateway = new RazorpayTestGateway(); // problem 2: a hidden dependency
```

Every other collaborator comes in through the constructor. This one is created inside the class.
That is a **hidden dependency**: nothing in `OrderService`'s signature says it talks to a payment
provider, and nothing outside the class can change which one.

The consequence is in `OrderServiceTest`. The test wires everything by hand, which is tedious
but possible. Then it wants to check what happens when a payment is declined, and the only way
to get a decline is to know that `RazorpayTestGateway` declines cards ending in `0000`. The
gateway's rules have leaked into the order tests. When the real Razorpay arrives in Demo 23
there is no "card ending in 0000", and every test that relied on it breaks, and there is no way
to make the real gateway decline on demand in a unit test anyway.

The fix is one line: take the `PaymentGateway` as a constructor parameter like everything else.
Then a test passes a fake gateway that declines when asked. That is **dependency injection** in
its entirety: do not create your collaborators, receive them. The container of Demo 2 makes it
the default; the principle is older than Spring and does not need it.

A rule you can apply to any class: if a constructor contains `new` for something that talks to
the outside world (a payment provider, a database, a clock, the network, the file system), the
class cannot be unit-tested, and the `new` should be a parameter.

---

## 5. Configuration by hand

`AppConfig` loads `shopscope.properties` from the classpath, optionally overlays a file named
in a system property, and parses each value where it is read:

```java
public int port() {
    return Integer.parseInt(properties.getProperty("server.port", "8080"));
}
```

What is missing: validation (a port of `abc` fails at the first request, not at start-up),
types (everything is a `String` until someone parses it), defaults in one place (each getter
invents its own), and any notion of environment. Running the shop for the UK means a second
file and a `-Dshopscope.config=...` flag that someone has to remember.

Tax rates are worse: `TaxRules.forCountry` is a `switch` with the rates in code. Changing GST
from 18% to 20% is a rebuild and a deploy.

Spring's `Environment` (Demo 3) reads properties, system properties and environment variables
in a defined order; profiles select per-environment values; `@ConfigurationProperties` (Demo 4)
binds them into a validated record at start-up; a config server (Demo 14) serves them to every
service from one Git repository. Each step replaces a line of `AppConfig` with nothing.

---

## 6. Cross-cutting code, pasted everywhere

Every service method starts with `long started = System.nanoTime();` and ends with a log line
that reports the elapsed time. The comment above the helper in `CatalogService` says it:

```java
// Copied into every service of the monolith. Demo 3 turns it into one aspect.
```

Timing, logging, auditing, security checks, transactions, retries: these are **cross-cutting
concerns**. They apply to many methods and have nothing to do with what any of those methods is
for. Pasting them in has the obvious cost (the same five lines in twenty places, each slightly
different, none of them tested) and a subtler one: the business logic is harder to read because
a third of each method is not business logic.

Count them in the monolith: `grep -rn "System.nanoTime" src/main/java`. Then imagine adding
"log the current user" to each. Aspect-oriented programming (Demo 3) lets you write the timing
once, as an `@Around` advice, and say which methods it applies to with a pointcut. The
services go back to being about their job.

---

## 7. The shared in-memory store and the overselling bug

Every repository in the monolith is a `Map` in memory. `InventoryService.stock` is a
`HashMap<String, Integer>`. There is no database, which means there is no transaction, which
means the following sequence in `OrderService.checkout` is a bug waiting for its first customer:

1. `pricing.quote(basket, context)`: fine.
2. `inventory.reserve(sku, quantity)` for each line: the map is decremented. The kettle goes from
   2 to 1.
3. `gateway.charge(total, card)`: the card is declined.
4. `throw new PaymentDeclinedException(reason)`.

Nothing in step 4 puts the kettle back. The shopper has no order; the shop has one fewer kettle
to sell. Do it twice and the last kettle is unsellable. You can watch it happen with two `curl`
calls in Lab 6, and `ProblemRegisterTest.problem6_aDeclinedPaymentLeavesStockUntouched` states
the correct behaviour and fails.

There are two problems in one here. The first is **ordering**: stock should be reserved
tentatively and confirmed only after payment, or released on failure. That is a design fix
(Demo 13's reservations). The second is **atomicity**: a checkout touches stock, orders and
the ledger, and either all of those changes should happen or none of them. With a database that
is a transaction, and Demo 10 wraps checkout in one so that a failure after the decrement rolls
the decrement back. With a map, there is nothing to roll back to.

A third problem is hiding behind them: there is also no **lock**. Two checkouts for the last
kettle, at the same moment, can both read `available = 1`, both pass the check, and both
decrement. Demo 10 demonstrates the race and fixes it with optimistic locking.

And the obvious one: restart the process and every order is gone.

---

## 8. One process

The monolith is one JVM. That has consequences that no amount of refactoring inside it will
change:

- **Scaling.** If checkout is slow under load, you run a second copy of the whole thing, catalogue
  browsing included, even though browsing was fine.
- **Failure.** A memory leak in the notification module takes down checkout. A bug that spins a
  thread in pricing starves the HTTP layer.
- **Deployment.** A one-line change to a tax rate is a full build and a full restart, during
  which nothing serves requests.
- **Teams.** Everyone commits to the same build. A broken test in `catalog` blocks a release of
  `order`.

These are the arguments for microservices, and they are real. They are also the arguments
people reach for too early. A single process with clean modules can serve a great deal of
traffic, and splitting it costs you network calls, partial failures, distributed data and an
operations burden that this track spends Demos 12 to 25 teaching. The right time to split is
when one of the four bullets above is actually hurting, and the right way is one module at a
time (section 11).

---

## 9. The hand-rolled HTTP layer

`ShopHttpServer` is the JDK's `com.sun.net.httpserver.HttpServer` with three handlers. It
works, and `http/monolith.http` proves it. It also shows, in sixty lines, what a web framework is
for:

- **Routing is string comparison.** `path.substring("/api/products/".length())` is how the SKU
  is read. A trailing slash breaks it.
- **The method is checked by hand** in each handler, or not at all (`/api/inventory` accepts
  anything).
- **JSON is the `Json` class**: an encoder, and a "parser" that reads one flat object with a
  regular expression. `{"quantity": "one"}` becomes a `NumberFormatException`; a nested object
  becomes a `NullPointerException`; both become a `500` with the exception's `toString()` in the
  body, which is the sort of thing a security review flags on page one.
- **Status codes are chosen in each handler**, and the mapping from exception to status is a
  `catch` in each handler. Add a new exception and every handler needs a new `catch`.
- **No content negotiation, no validation, no versioning, no documentation.**

Spring MVC (Demo 5) replaces the routing, method matching, parameter extraction and JSON with
annotations and Jackson; Demo 6 replaces the `catch` blocks with one `@RestControllerAdvice`
that turns every exception into a `ProblemDetail`. The `/api` paths do not change.

---

## 10. Start-up order and shutdown

`main` is also where start-up happens, and in the Demo 1 starter it happens in the wrong order:
the HTTP server starts, then the catalogue loads (slowly, as if from a supplier feed). For a
second and a half, `GET /api/products` returns `[]`. A load balancer that checked the port would
have sent traffic to an empty shop.

Shutdown is the mirror image. In the starter nothing happens on `Ctrl+C`: the server is not
stopped, nothing is flushed, nothing is reported. The solution adds a shutdown hook, and the
hook has to know about every resource that needs closing, in the right order. Every new
resource is a new line in the hook that someone will forget.

Lifecycle, the orderly start and stop of everything in an application, is something a
container does by itself: Spring starts beans in dependency order, calls their initialisation
callbacks, and on shutdown destroys them in reverse (Demo 2). Spring Boot adds readiness and
liveness states so that a load balancer knows when the shop is actually open (Demo 4, Demo 22).

---

## 11. Two ways out: modular monolith and microservices

A **modular monolith** keeps one process but enforces the module boundaries: each bounded
context in its own package or Maven module, with explicit rules about which may depend on
which, and communication between them through interfaces and events rather than direct calls
into each other's internals. Most of the problems above are fixed at this stage, without a
network in sight. ShopScope is a modular monolith by the end of Demo 11: Spring-wired, Spring
Boot, a real REST API, PostgreSQL, transactions, tests, one Maven module per context.

**Microservices** take the modules apart into separate processes with separate databases. The
benefits are the four bullets of section 8; the costs are everything in Demos 12 to 22. The
sensible way to get there is the **strangler pattern**: extract one module at a time, put a
client where the module used to be, and keep the rest running. Demo 12 extracts the catalogue
by hand, feeling every consequence (timeouts, error mapping, correlation ids, a second process
to run). Demo 13 extracts the remaining four the same way, pre-built, so the session can spend
its time on what is new: MySQL for orders, MongoDB for customers, and a checkout that now
spans four services and must cope when the third one fails.

The order matters. A monolith split before its modules are clean does not become
microservices; it becomes a distributed monolith, with all the coupling and a network in the
middle. Clean first, then split, and only the modules that need splitting.

---

## 12. The problem register

`PROBLEMS.md` in the Demo 1 solution. Fill it in during Lab 9; re-read it at the end of every
part.

| # | Problem | Where you saw it | Fixed in |
|---|---|---|---|
| 1 | **Modules call each other directly.** `OrderService` calls `NotificationService`; an e-mail failure is a checkout failure. The dependency graph has arrows that should not exist. | Lab 1, Lab 6 | Demo 3 (events), Demo 13 (separate processes) |
| 2 | **Wiring by hand, and a hidden dependency.** The composition root is a page of `new` in a fixed order; `OrderService` creates its own `RazorpayTestGateway`, so no test can replace it. | Lab 2, Lab 3 | Demo 2 (the container, constructor injection) |
| 3 | **Nothing is testable in isolation.** Every test wires the real object graph by hand, and the gateway's rules leak into order tests. | Lab 3 | Demo 2, Demo 11 |
| 4 | **Configuration by hand.** A properties file parsed where it is read, tax rates hard-coded in `TaxRules`, a second environment means a second file and a system property. | Lab 4 | Demo 3 (`Environment`, profiles), Demo 4 (`@ConfigurationProperties`), Demo 14 (config server) |
| 5 | **Cross-cutting code pasted everywhere.** Timing and audit logging copied into every service method. | Lab 5 | Demo 3 (one aspect), Demo 22 (observability) |
| 6 | **The shared in-memory store, and the overselling bug.** Stock is decremented before payment and never restored when payment fails. No transaction, because there is no database. | Lab 6, `ProblemRegisterTest` | Demo 7 (PostgreSQL), Demo 10 (transactions, locking), Demo 13 (reservations) |
| 7 | **One process.** Everything scales together, fails together and deploys together. | Lab 1, Lab 8 | Demo 12 and Demo 13 (extraction), Demo 16 (the gateway) |
| 8 | **The hand-rolled HTTP layer.** String routing, a JSON parser that misreads anything nested, status codes chosen by hand, a 500 with the exception in the body. | Lab 7 | Demo 5 (Spring MVC), Demo 6 (`ProblemDetail`) |
| 9 | **Start-up and shutdown by hand.** The order of `main` is load-bearing; a shutdown hook has to remember every resource. | Lab 8 | Demo 2 (lifecycle), Demo 4 (Boot) |

The goal of the track is an empty table. Notice how many rows are closed before Demo 12: the
monolith is not the problem; this monolith's construction is.

---

## 13. Self-check

1. Which arrow in the dependency graph should not exist, and what would you replace it with?
2. Why does the order of the lines in `ShopScopeMonolith.main` matter, and what tool makes it
   stop mattering?
3. Adding a `Clock` to `OrderService` touches three kinds of file. Name them.
4. What is a hidden dependency? Point at the one in `OrderService` and say what it stops you
   doing.
5. Walk through the overselling bug step by step. Which two separate problems does it contain?
6. Why does "no database" imply "no transaction"?
7. List two consequences of being one process that no refactoring inside the process can fix.
8. What does `ShopHttpServer.checkout` do with a `NumberFormatException`, and why is that
   wrong?
9. What is a modular monolith, and why does this track build one before extracting any
   service?
10. What is the strangler pattern?

---

## 14. References

- Dependency injection and the container: [Spring Framework reference, "The IoC Container"](https://docs.spring.io/spring-framework/reference/core/beans.html).
- Aspect-oriented programming: [Spring Framework reference, "Aspect Oriented Programming with Spring"](https://docs.spring.io/spring-framework/reference/core/aop.html).
- Application lifecycle in Spring Boot: [Spring Boot reference, "Application Availability"](https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.application-availability).
- The JDK HTTP server used by the monolith: [`com.sun.net.httpserver`](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.httpserver/com/sun/net/httpserver/package-summary.html).
- Shutdown hooks: [`Runtime.addShutdownHook`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Runtime.html#addShutdownHook(java.lang.Thread)).

**Used in:** Demo 01 (every lab), and re-read at the end of Parts 1, 2 and 3 as the register
empties.
