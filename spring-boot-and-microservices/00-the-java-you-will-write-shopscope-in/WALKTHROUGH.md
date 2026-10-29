# Demo 0 — Walkthrough: the finished code

Read this after the session, with `../01-shopscope-as-a-plain-java-monolith/starter` open: that is Demo 0
finished (plus the monolith modules Demo 1 adds, which you can ignore here). The packages are described
in dependency order: each one uses only the ones above it.

```
shared ← pricing ← catalog ← order ← concurrent
                            jdbc (uses pricing and shared)
SeedData, QuoteDemo (use everything)
```

Nothing in this code is mutable after construction, and nothing here knows about Spring, a database or
HTTP. That is deliberate: it is the part of ShopScope that survives every later demo unchanged, and the
part a service can take with it when it is extracted in Demo 12.

---

## `shared`: Money

**`Money`** is a record of a `BigDecimal` and a `java.util.Currency`. Three decisions make it a value
object rather than a pair of fields:

- The compact constructor refuses `null` and rewrites the scale to the currency's fraction digits with
  `HALF_EVEN`. Because records compare components, the scale had to be normalised: `INR 10` and
  `INR 10.00` arrive as different `BigDecimal`s and must leave as equal `Money`s.
- Every operation returns a new `Money`. `plus`, `minus`, `times`, `percent` and `min` never assign a
  field. This is what lets `PricingService` be shared by a thousand threads in Lab 7 without a lock.
- Arithmetic between currencies throws `CurrencyMismatchException` from one private
  `requireSameCurrency`, called at the top of every two-operand method. `times(int)` has no second
  currency and no check.

```java
public Money percent(int percent) {
    return new Money(amount.multiply(BigDecimal.valueOf(percent))
            .divide(BigDecimal.valueOf(100), currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN), currency);
}
```

`divide` is given a scale and a rounding mode explicitly; without them a non-terminating result throws.
`compareTo` is implemented so `Comparator.comparing(PricedLine::lineTotal)` works, and `toString` is
overridden to `INR 2499.00` because the record default is unreadable in logs and test output.

**`CurrencyMismatchException`** extends `IllegalArgumentException`: unchecked, and semantically "you
called this with the wrong argument".

---

## `pricing`: scopes, prices, rules and the quote

**`PriceContext`** is who is asking: currency and country are required; channel and customer group may be
`null`. The `withChannel` and `withCustomerGroup` methods return new contexts, in the same spirit as
`Money`. There is no `Optional` in the record: `Optional` is a return type, and a record component is a
field.

**`PriceScope`** is the sealed family of four nested records. `appliesTo(PriceContext)` and
`precedence()` are default methods on the interface that branch with `instanceof` patterns. The
non-null value is always on the left of `equals`, so a scope compared against a `null` channel answers
`false` instead of throwing.

> **Java 17 vs 21.** On 21 and later both methods are one exhaustive `switch`:
> `return switch (this) { case Base b -> true; case ForCountry c -> c.country().equals(context.country()); ... }`.
> The compiler checks that every permitted record has a case and no `default` is needed. The 17 form in
> the code is equivalent; it just cannot be checked for completeness, which is why `return true; // Base`
> carries a comment.

**`Price`** ties a SKU, a `Money` and a scope together, refuses negative values, and has one static
factory per scope so `SeedData` reads as a table.

**`PriceList`** groups prices by SKU at construction (`Collectors.groupingBy(Price::sku, toUnmodifiableList())`)
and answers the one question that matters:

```java
public Optional<Price> find(String sku, PriceContext context) {
    return bySku.getOrDefault(sku, List.of()).stream()
            .filter(price -> price.value().currency().equals(context.currency()))
            .filter(price -> price.scope().appliesTo(context))
            .max(Comparator.comparingInt(price -> price.scope().precedence()));
}
```

Filter by currency, filter by applicability, take the highest precedence. `priceOrThrow` is the
exception-returning twin for callers that cannot proceed without a price, and `NoPriceException` names
both the SKU and the currency/country so the message is useful.

**`DiscountRule`** is the second sealed family: `PercentageOff`, `AmountOff` and `BuyXGetYFree` are
line-level and carry a SKU; `BasketOver` is basket-level and carries a threshold. Each carries a `code`
that ends up on the quote. **`Discounts`** is a utility with two static methods, `lineDiscount` and
`basketDiscount`, each an `instanceof` chain with a guard on the SKU or the threshold and a zero
fallthrough. The three-for-two arithmetic is integer division on purpose:
`(quantity / (buy + free)) * free`.

**`PricedLine`** is a basket line with the price that was selected for it; `lineTotal()` is the only
method. **`Quote`** is the result record; its compact constructor copies both lists so a caller cannot
change a quote after the fact.

**`TaxRule`** is an interface with a name and `taxOn(Money)`; **`FlatRateTax`** is a record implementing
it; **`TaxRules.forCountry`** is a `switch` expression over country codes that returns 18% GST for India,
20% VAT for the UK and 0 for the US. It is hard-coded, and Demo 3 replaces it with profiles; for Demo 0 it
is simply a factory.

**`PricingService`** is the one class here with real logic, and it is all streams:

```java
List<PricedLine> lines = basket.lines().stream()
        .map(line -> new PricedLine(line.sku(), line.name(), line.quantity(), priceList.priceOrThrow(line.sku(), context)))
        .toList();
Money subtotal = lines.stream().map(PricedLine::lineTotal).reduce(zero, Money::plus);
Money lineDiscounts = rules.stream()
        .flatMap(rule -> lines.stream().map(line -> Discounts.lineDiscount(rule, line)))
        .reduce(zero, Money::plus);
```

Select a price per line, fold the totals, apply every rule to every line (`flatMap` turns the rules-by-lines
grid into one stream of discounts), fold the basket rules, cap the discount at the subtotal with
`Money.min`, tax the net, and record which codes did something. The constructor copies the rule list
(`List.copyOf`), so the service is immutable like everything else. The `System.Logger` line at the end is
the first instance of "timing pasted into a method" that Demo 1 names as a problem and Demo 3 removes.

---

## `catalog`: lookups that cannot lie

**`Product`** is a record with a compact constructor that defaults a `null` description to `""`.
**`CatalogRepository`** is an insertion-ordered map with `save`, `findBySku`, `findAll` and
`findByCategory`; the one line that matters is `Optional.ofNullable(bySku.get(sku))`, the boundary where
a map's `null` becomes an honest `Optional`.

**`CatalogService`** offers the two signatures the study note describes:

```java
public Optional<Product> find(String sku)   // absence is normal
public Product get(String sku)             // absence is a bug: ProductNotFoundException
```

`get` is `find`'s result with `orElseThrow(() -> new ProductNotFoundException(sku))`. The exception keeps
the SKU in a field; Demo 6 reads it when it maps the exception to an HTTP 404. `inCategory` returns an
immutable copy, never the repository's own collection.

---

## `order`: the basket and its reports

**`LineItem`** is a SKU, a name snapshot and a positive quantity; the compact constructor rejects zero and
negative quantities. The name is copied onto the line so that renaming a product later does not rewrite
old baskets.

**`Basket`** is a record of a customer id and a copied list of lines. `add` returns a new basket, merging
quantities when the SKU already appears; `empty(customerId)` is the starting point. `BasketReportTest`
proves the merge: four `add` calls produce three lines.

**`BasketReport`** is five static methods, one collector each: `groupingBy` with `summingInt`,
`groupingBy` on a catalogue lookup, `max` with a comparator (returning `Optional`), `partitioningBy`, and
`teeing` two collectors into a `Summary` record. None has a loop or a mutable accumulator; each is the
canonical use of its collector.

---

## `concurrent`: the batch

**`QuoteBatch.quoteAll`** is the executor-and-futures shape: one `Callable<Quote>` per basket,
`invokeAll` on a fixed pool sized from the available processors, `join` each future in order, and
`shutdown()` in `finally`. `join` unwraps `ExecutionException` to its cause so a failing quote fails the
batch with the real exception, and re-sets the interrupt flag on `InterruptedException`.

> **Java 17 vs 21.** On 21 and later the executor becomes
> `Executors.newVirtualThreadPerTaskExecutor()` and the `try`/`finally` becomes try-with-resources,
> because `ExecutorService` is `AutoCloseable` from 19. Nothing else changes. A thousand platform threads
> would be a problem; a thousand virtual threads are not, and the pool size stops being a tuning
> decision. The track stays on the 17 form so every participant can run it.

---

## `jdbc`: one trip to the database by hand

**`JdbcPriceLoader.load`** is the four JDBC objects in try-with-resources: `Connection` from
`DriverManager`, a `PreparedStatement` with one bound parameter, a `ResultSet` walked with `next()`, and a
row-to-`Price` mapping that goes through the private `scope(type, value)` factory. The SQL is a text block.
Resources close in reverse order even when an exception is thrown. There is no connection pool, no
transaction and no mapping framework; Demo 7 adds all three without you writing them.

**`schema.sql`** is the matching DDL and seed data: `product` and `price`, a foreign key, two `check`
constraints, an index on `(sku, currency)`, and the insert rows that mirror `SeedData.prices()`. The
PostgreSQL driver is `runtime` scope in the POM: nothing in `src/main` imports it, and it is on the
classpath when the loader runs through Maven.

---

## `SeedData` and `QuoteDemo`

**`SeedData`** is the one place the four products, eleven prices and three rules are written down. The
constants `TEA`, `MUG`, `KETTLE`, `BISCUITS` are used by every test so a typo in a SKU is a compile error.
The prices are chosen to exercise every scope: the kettle has a base and an India price, the mug a base
and an app price, tea a base and a wholesale price, and every product has a GBP base price for the
currency filter. In Demo 1 the same class gains `stock` and `customers`.

**`QuoteDemo.main`** builds a catalogue, a pricing service and a three-line basket, quotes it and prints
the lines, subtotal, discount with its codes, tax and total, then the `BasketReport.summary`. It is the
main class named in the jar manifest and the exec plugin, so `java -jar` and `./mvnw exec:java` both run
it. The printed total, `INR 3758.54`, is the number Lab 5 asks you to check by hand.

---

## The tests

One class per lab, all `@Disabled` in the starter and enabled lab by lab:

| Class | Lab | What it pins down |
|---|---|---|
| `MoneyTest` | 1, 6 | scale normalisation, equality, exact addition and rounding (parameterized), currency mismatch, nulls, `toString` |
| `PriceListTest` | 2, 4 | every rung of the precedence ladder, the currency filter, `Optional` on unknown input, the exception message |
| `DiscountsTest` | 2 | each rule applies to its SKU only, amount-off is capped, three-for-two counts groups, basket-over needs the threshold |
| `PricingServiceTest` | 4 | selected prices per line, discount then tax, the basket rule, the wholesale price, failing loudly on a missing price |
| `CatalogServiceTest` | 4 | `Optional` from `find`, the named exception and its `sku` field from `get`, category filtering |
| `BasketReportTest` | 3 | line merging, every collector, the `teeing` summary |
| `QuoteBatchTest` | 7 | a thousand baskets quoted in order |

The POM points surefire at `src/test/resources/logging.properties`, which raises the JUL level to
`WARNING` so the services' per-call log lines do not drown the test report. The tests are plain JUnit and
AssertJ; there is no Spring test context to start, and the whole suite runs in under a second.

---

## What Demo 1 adds

Demo 1 surrounds this code with the rest of the backend, in the same process and the same package:
`inventory` (stock per SKU, in a map), `customer` (registered shoppers), `payment` (a `PaymentGateway`
port and a Razorpay stand-in), `notification` (e-mails collected in a list), `order.OrderService` (the
checkout that calls all of them), `config.AppConfig` (a properties loader), `http.ShopHttpServer` and
`http.Json` (an HTTP layer on the JDK's `HttpServer` with hand-rolled JSON), and `ShopScopeMonolith`, the
composition root that builds the whole graph with `new`. Nothing you wrote today changes. What changes is
that you can now see what is missing around it, and Demo 1's job is to write that list down.
