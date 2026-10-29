# Demo 0 — The Java you will write ShopScope in

**Demo guide** · ~205 minutes · you build the domain pieces of ShopScope in modern Java

---

## What you ship today

The pieces every later demo is built from, in plain Java with no framework in sight: `Money` as a value
object that cannot be wrong, a price list that picks the right price for a shopper out of four competing
scopes, discount rules as a sealed family, a basket and the reports a basket gets asked for, a catalogue
whose lookups never return `null`, a batch of a thousand quotes priced in parallel, and one trip each to
a relational database, a document database and an HTTP API before any library hides them.

By the end you will be able to answer, without hesitating:

- Why `Money` is a record, and why `INR 10` and `INR 10.00` must be equal
- What a sealed interface buys you that an ordinary interface does not
- What `instanceof` pattern matching is, and what Java 21 adds on top of it
- Which collector to reach for: `groupingBy`, `partitioningBy`, `teeing` or `reduce`
- When a method returns `Optional`, when it throws, and why it never returns `null`
- What `./mvnw verify` actually does, phase by phase, and what a BOM is
- Why an executor and a list of futures is the shape of every batch, on any Java version
- What an ORM, a document store and a web framework are each hiding from you

---

## Pre-reading

The theory for this demo is in the study notes; the demo assumes you have read it.

| Study notes | Read |
|---|---|
| [study-notes 01 — Modern Java for Spring developers](../../study-notes/01-modern-java-for-spring/) | in full |
| [study-notes 02 — Build and test: Maven, JUnit, AssertJ](../../study-notes/02-build-and-test-maven-junit-assertj/) | in full |
| [study-notes 03 — Data and the network before frameworks](../../study-notes/03-data-and-the-network-before-frameworks/) | in full |

The full demo-to-notes map is in [study-notes/README.md](../../study-notes/README.md#mapping-to-the-shopscope-demos).

---

## Before the demo (10 minutes)

1. **Clone the track and open the starter.**

   ```bash
   git clone https://github.com/suhvishalp/shopscope-java-microservices.git
   cd shopscope-java-microservices/spring-boot-and-microservices/00-the-java-you-will-write-shopscope-in/starter
   ```

2. **Check your JDK.** Anything from 17 upwards works; the code is compiled for 17 and runs unchanged on
   21 and 25.

   ```bash
   java -version
   ```

3. **Build once.** The first run downloads Maven itself and the dependencies; after that it is seconds.

   ```bash
   ./mvnw -q verify
   ```

   It must end without an error. Every test is skipped at this point; that is correct, and Lab 1 explains why.

4. **Open the folder in IntelliJ IDEA or VS Code** (with the Java extension pack). Both read the `pom.xml`
   and set the JDK level from it.

5. **Docker Desktop** is needed only for Labs 8 and 9 (a PostgreSQL and a MongoDB container). If you cannot
   run Docker, do those two labs as reading.

---

## How this guide works

Every lab has the same shape:

| | |
|---|---|
| **Problem** | What actually hurts, concretely |
| **Concept** | The idea that fixes it, explained plainly |
| **Steps** | What to type, in order, with the file named |
| **Verify** | The specific thing you should see |
| **Watch out** | The error you are most likely to hit, and why |
| **Challenge** | Two minutes, optional, harder |
| **In the real world** | Where this shows up on a real team |

Starter files carry numbered markers, `// TODO(lab-1.2)`, so you always know where you are. Search for
them (`Ctrl/Cmd+Shift+F`) if you get lost.

**Tests are the verify steps.** The starter ships every test class marked `@Disabled("enable in Lab N")`.
When a lab tells you to, delete that annotation and run the tests. Red means the lab is not finished; green
means it is. `./mvnw -q verify` stays green between labs because the unfinished tests are still disabled.

**The finished version of this demo is the next demo's starter.** If you fall behind, open
`../01-shopscope-as-a-plain-java-monolith/starter` and diff: every file you touch today is there, finished.

---

## Orientation — what is in the box (5 min)

```
starter/
├── pom.xml                  ← one file: Java level, every library version, four plugins. Lab 5 reads it.
├── mvnw, mvnw.cmd, .mvn/    ← the Maven wrapper: a Maven that pins its own version
├── docs/lab-9-first-document.js       ← the MongoDB shell script for Lab 9
├── http/lab-10-http-essentials.http   ← the requests for Lab 10
└── src/
    ├── main/java/com/shopscope/monolith/
    │   ├── shared/        Money, CurrencyMismatchException            ← Lab 1
    │   ├── pricing/       PriceScope, Price, PriceList, PriceContext,
    │   │                  DiscountRule, Discounts, PricedLine, Quote,
    │   │                  PricingService, TaxRule, FlatRateTax, TaxRules ← Labs 2 and 4
    │   ├── catalog/       Product, CatalogRepository, CatalogService   ← Lab 4
    │   ├── order/         LineItem, Basket, BasketReport               ← Lab 3
    │   ├── concurrent/    QuoteBatch                                   ← Lab 7
    │   ├── jdbc/          JdbcPriceLoader                              ← Lab 8
    │   ├── SeedData       the four products, their prices, three rules
    │   └── QuoteDemo      a main() that prices one basket               ← Lab 5
    ├── main/resources/sql/schema.sql  ← the price table for Lab 8
    └── test/java/...                  ← one test class per lab, all @Disabled for now
```

Three things to notice before we start:

**The package is `com.shopscope.monolith`.** Demo 1 adds the rest of the monolith around these classes.
From Demo 12, `catalog`, `pricing` and `order` each leave this package for a service of their own; the
code you write today goes with them.

**Every type you write today is a record, an interface or a small class with no setters.** Nothing in
this package can be changed after it is built. That is not a style preference; it is what makes Lab 7
possible without a single lock.

**`SeedData` is the same four products for the whole track.** Tea, a mug, a kettle and biscuits, with
prices in INR and GBP. When a later demo shows a product listing, these are the products.

---

## Lab 1 — Records, immutability and Money as a value object (25 min)

### Problem

Open `src/main/java/com/shopscope/monolith/shared/Money.java`. The record is declared, the factory methods
exist, and every arithmetic method throws `UnsupportedOperationException`. Now open
`src/test/java/com/shopscope/monolith/shared/MoneyTest.java` and read `twoMoneysWithTheSameValueAreEqual`:
it insists that `Money.of("10", "INR")` equals `Money.of("10.00", "INR")`. With the constructor as it stands
that test fails, because `new BigDecimal("10")` and `new BigDecimal("10.00")` are not equal. Money that
compares by the accident of how it was typed is money you cannot put in a `Set`, a map key or an assertion.

### Concept

**A record is a class whose state is its identity.** You declare the components; the compiler writes the
constructor, the accessors, `equals`, `hashCode` and `toString` from them. Two records with equal
components are equal. There are no setters, and the fields are final.

```java
public record Money(BigDecimal amount, Currency currency) { }
```

**A compact constructor validates and normalises.** It has no parameter list; it runs before the fields
are assigned, and whatever you assign to the parameter names is what gets stored. That is where `null`
is refused and where the scale is fixed:

```java
public Money {
    Objects.requireNonNull(amount, "amount");
    amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
}
```

**A value object never changes; it returns a new one.** `plus` does not add to this `Money`; it returns
the sum. The caller keeps the original. Because nothing changes, a `Money` can be shared between threads,
cached, and used as a map key without a second thought.

**Mixing currencies is a bug, not a conversion.** `INR 10 + GBP 10` has no answer; it has an exception.

### Steps

All steps are in **`src/main/java/com/shopscope/monolith/shared/Money.java`**.

1. **`TODO(lab-1.1)` — the compact constructor.** Replace the empty body with:

   ```java
   public Money {
       Objects.requireNonNull(amount, "amount");
       Objects.requireNonNull(currency, "currency");
       // Normalise the scale so INR 10 and INR 10.00 are the same value (records compare components).
       amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
   }
   ```

   `getDefaultFractionDigits()` is 2 for INR and GBP and 0 for JPY, so the currency decides the scale.
   `HALF_EVEN` is banker's rounding, the default for money in most finance code.

2. **`TODO(lab-1.2)` — arithmetic that returns a new value.** Replace the three throwing methods:

   ```java
   public Money plus(Money other) {
       requireSameCurrency(other);
       return new Money(amount.add(other.amount), currency);
   }

   public Money minus(Money other) {
       requireSameCurrency(other);
       return new Money(amount.subtract(other.amount), currency);
   }

   public Money times(int quantity) {
       return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
   }
   ```

   `requireSameCurrency` is already there at the bottom of the file; it throws
   `CurrencyMismatchException`. Note what `times` does not do: it does not check the currency, because
   there is only one.

3. **`TODO(lab-1.3)` — a percentage, rounded.**

   ```java
   public Money percent(int percent) {
       return new Money(amount.multiply(BigDecimal.valueOf(percent))
               .divide(BigDecimal.valueOf(100), currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN), currency);
   }
   ```

   `divide` without a scale and rounding mode throws on a non-terminating result; always give both.

### Verify

Open `MoneyTest.java` and delete the `@Disabled("enable in Lab 1")` line above the class. Run:

```bash
./mvnw -q test -Dtest=MoneyTest
```

Seven green tests, including `refusesNulls` (step 1), `refusesToMixCurrencies` (step 2) and
`printsCurrencyThenAmount`, which proves that `toString` was overridden to read `INR 2499.00` rather than
the record default `Money[amount=2499.00, currency=INR]`.

### Watch out

**`amount.setScale(...)` without assigning it back.** `BigDecimal` is immutable too: `setScale` returns a
new value. In the compact constructor you must write `amount = amount.setScale(...)`; the test
`scaleFollowsTheCurrency` catches the mistake.

**`requireNonNull` after `setScale`.** Order matters: a `null` amount reaches `setScale` first and you get
a `NullPointerException` without your message. Check, then normalise.

**Comparing `BigDecimal` with `equals`.** Outside this record, remember that `new BigDecimal("1.0").equals(new BigDecimal("1.00"))`
is `false`; `compareTo` is how you compare numerically. Inside the record the scale is fixed, which is why
the record's generated `equals` is safe.

### Challenge

Add `Money.of(long minorUnits, String currencyCode)` that builds `INR 12.50` from `1250`. The
`currency.getDefaultFractionDigits()` tells you where to put the point. The finished code in Demo 1 does
not have it; see if your version survives the JPY case.

### In the real world

Every payment provider's API takes amounts in minor units with a currency code next to them, and every
serious codebase has a `Money` type for exactly the reason this lab shows: a `double` cannot represent
`0.10`, and a bare `BigDecimal` does not know its currency. Spring's own `Money` support (JSR 354) is the
same idea with more machinery.

---

## Lab 2 — Sealed interfaces and pattern matching: price scopes with a precedence, and discount rules (25 min)

### Problem

A kettle has four INR prices in `SeedData`: a base price, a cheaper one for India, a cheaper one again on
the app, and a wholesale price. A wholesale customer in India on the app should pay the wholesale price;
a retail customer on the website should pay the India price. Open `PriceScope.java`: `appliesTo` and
`precedence` throw. Open `Discounts.java`: both methods return zero for everything. Until those four
methods exist, every price is the base price and no promotion does anything.

### Concept

**A sealed interface lists its implementations.** `PriceScope` `permits` exactly four records. Nobody can
add a fifth outside this file, so code that handles all four handles every case there will ever be.

```java
public sealed interface PriceScope
        permits PriceScope.Base, PriceScope.ForCountry, PriceScope.ForChannel, PriceScope.ForCustomerGroup {
    record Base() implements PriceScope {}
    record ForCountry(String country) implements PriceScope {}
    ...
}
```

The cases are records nested in the interface, which keeps the family in one file and gives each case
its data: a country scope carries a country, a base scope carries nothing.

**Pattern matching with `instanceof` tests and binds in one step.** On Java 17 this is the tool:

```java
if (this instanceof ForCountry scope) {
    return scope.country().equals(context.country());
}
```

`scope` is a `ForCountry` inside the block, no cast needed. A guard can ride along:
`if (rule instanceof PercentageOff r && r.sku().equals(line.sku()))`.

**Precedence is a number.** The most specific scope wins: customer group over channel over country over
base. Give each a rank and the price list picks the maximum (Lab 4 finishes that method).

> **On Java 21 and later** the same `appliesTo` becomes one `switch` over `this` with a `case` per record,
> and the compiler refuses to compile it if a case is missing, with no `default` needed. That is the payoff
> of `sealed`. The code in this track stays on 17 so nobody is blocked; the study note shows the 21 form.

### Steps

1. **`TODO(lab-2.1)` — `PriceScope.appliesTo`** in `src/main/java/com/shopscope/monolith/pricing/PriceScope.java`:

   ```java
   default boolean appliesTo(PriceContext context) {
       if (this instanceof ForCountry scope) {
           return scope.country().equals(context.country());
       }
       if (this instanceof ForChannel scope) {
           return scope.channel().equals(context.channel());
       }
       if (this instanceof ForCustomerGroup scope) {
           return scope.customerGroup().equals(context.customerGroup());
       }
       return true; // Base
   }
   ```

   `context.channel()` may be `null` (a shopper with no channel). `"app".equals(null)` is `false`, which is
   the right answer, and the reason the constant is on the left.

2. **`TODO(lab-2.2)` — `PriceScope.precedence`**, same file:

   ```java
   default int precedence() {
       if (this instanceof ForCustomerGroup) {
           return 3;
       }
       if (this instanceof ForChannel) {
           return 2;
       }
       if (this instanceof ForCountry) {
           return 1;
       }
       return 0; // Base
   }
   ```

3. **`TODO(lab-2.3)` — line-level discounts** in `Discounts.java`. Replace the `return zero;` under the
   comment with the three branches, keeping the final `return zero;`:

   ```java
   if (rule instanceof PercentageOff r && r.sku().equals(line.sku())) {
       return line.lineTotal().percent(r.percent());
   }
   if (rule instanceof AmountOff r && r.sku().equals(line.sku())) {
       return r.amount().times(line.quantity()).min(line.lineTotal());
   }
   if (rule instanceof BuyXGetYFree r && r.sku().equals(line.sku())) {
       int freeUnits = (line.quantity() / (r.buy() + r.free())) * r.free();
       return line.unitPrice().times(freeUnits);
   }
   return zero; // another SKU, or a basket-level rule
   ```

   Three for two is `buy 2, free 1`: seven biscuits is two full groups of three, so two are free.
   `.min(line.lineTotal())` keeps an amount-off discount from going negative.

4. **`TODO(lab-2.4)` — the basket-level rule**, same file:

   ```java
   if (rule instanceof BasketOver r && subtotal.isGreaterThan(r.threshold())) {
       return subtotal.percent(r.percent());
   }
   return Money.zero(subtotal.currency());
   ```

### Verify

Delete `@Disabled("enable in Lab 2")` from `DiscountsTest.java` and run
`./mvnw -q test -Dtest=DiscountsTest`: four green.

Now delete the `@Disabled` from `PriceListTest.java` too and run it. **Expect one green test and six red
ones.** `findIsEmptyForAnUnknownSkuOrCurrency` passes, which proves `appliesTo` and `precedence` work
through `PriceList.find`. The other six call `priceOrThrow`, which still throws `TODO(lab-4.3)`; Lab 4
turns them green. Leave `PricingServiceTest` disabled until then, whatever its hint says.

### Watch out

**`scope.country().equals(context.country())` the other way round.** `context.country()` is never `null`
(the record refuses it), but `context.channel()` and `context.customerGroup()` are. Put the value you know
is non-null on the left of `equals`.

**Forgetting the guard.** `rule instanceof PercentageOff r` without `&& r.sku().equals(line.sku())` gives
every line in the basket the tea discount. `percentageOffAppliesToItsSkuOnly` is there to catch it.

**`BuyXGetYFree` with integer division.** `(7 / 3) * 1` is `2`, which is right; `7 / 3 * 1.0` is not what
you want. Keep it in `int`.

### Challenge

Add a fifth rule, `FreeShippingOver(String code, Money threshold)`, to the `permits` list. Compile. Nothing
else complains, because `Discounts` has a catch-all `return zero`. Now imagine the Java 21 `switch` form
without a `default`: the compiler would point at every place that needs to learn about the new rule. That
is the trade-off the catch-all makes.

### In the real world

Price selection by customer group, channel and country is exactly how commerce platforms resolve a price,
and the precedence order here matches theirs. Sealed hierarchies are how modern Java code models "one of
these kinds": payment results, parse results, command types. Spring Framework 7 itself uses them.

---

## Lab 3 — Lambdas, streams and collectors over a basket (30 min)

### Problem

Open `order/BasketReport.java`. Five questions a basket gets asked, five methods that throw. The
questions are the ones a back-office screen asks: how many of each SKU, lines by category, the most
expensive line, which lines are big, and how many units at what subtotal. Each could be a `for` loop with
a `HashMap` and a running total. Each would be ten lines, and each would be subtly different.

### Concept

**A stream is a pipeline: a source, zero or more intermediate operations, one terminal operation.** The
source is `basket.lines().stream()`. The intermediate operations (`map`, `filter`, `sorted`) describe what
to do to each element and run lazily. The terminal operation (`collect`, `reduce`, `max`, `toList`) pulls
the elements through and produces the answer.

**Collectors are the terminal operations that build structures.**

| You want | Collector |
|---|---|
| A map from a key to the elements with that key | `groupingBy(keyFn)` |
| A map from a key to a number per key | `groupingBy(keyFn, summingInt(fn))` |
| A map with exactly two keys, `true` and `false` | `partitioningBy(predicate)` |
| Two results from one pass | `teeing(collectorA, collectorB, merge)` |
| One value folded from all elements | `reduce(identity, accumulator)` |

**`reduce` with `Money::plus` needs an identity.** `Money.zero(currency)` is the identity for addition;
the accumulator is a method reference to the `plus` you wrote in Lab 1. That is why `Money` had to be a
value object: `reduce` calls `plus` many times and relies on it never changing its inputs.

**Method references name a method instead of wrapping it.** `LineItem::sku` is `line -> line.sku()`;
`Money::plus` is `(a, b) -> a.plus(b)`.

### Steps

All steps are in **`src/main/java/com/shopscope/monolith/order/BasketReport.java`**. Replace each
`throw new UnsupportedOperationException(...)` with the `return` shown.

1. **`TODO(lab-3.1)` — quantity by SKU.**

   ```java
   return basket.lines().stream()
           .collect(Collectors.groupingBy(LineItem::sku, Collectors.summingInt(LineItem::quantity)));
   ```

2. **`TODO(lab-3.2)` — lines by category.** The category is not on the line; the catalogue has it.

   ```java
   return basket.lines().stream()
           .collect(Collectors.groupingBy(line -> catalog.get(line.sku()).category()));
   ```

3. **`TODO(lab-3.3)` — the most expensive line.**

   ```java
   return quote.lines().stream().max(Comparator.comparing(PricedLine::lineTotal));
   ```

   `max` returns an `Optional` because an empty stream has no maximum. The method signature already says
   so; the caller has to decide what an empty quote means.

4. **`TODO(lab-3.4)` — big lines and small lines.**

   ```java
   return quote.lines().stream()
           .collect(Collectors.partitioningBy(line -> line.lineTotal().isGreaterThan(threshold)));
   ```

5. **`TODO(lab-3.5)` — two answers in one pass.**

   ```java
   return quote.lines().stream().collect(Collectors.teeing(
           Collectors.summingInt(PricedLine::quantity),
           Collectors.mapping(PricedLine::lineTotal, Collectors.reducing(Money.zero(quote.total().currency()), Money::plus)),
           (units, subtotal) -> new Summary(units, subtotal)));
   ```

   `teeing` runs both collectors over the same elements and hands both results to the merge function.
   The second collector is `mapping` (line to its total) feeding `reducing` (fold with `plus`).

Before you run the tests, open `pricing/PricingService.java` and read `quote`. It is the same toolkit
applied to pricing: `map` each line to a `PricedLine`, `reduce` the totals with `Money::plus`, `flatMap`
the rules over the lines. You do not write it today; you should be able to read every line of it.

### Verify

Delete `@Disabled("enable in Lab 3")` from `BasketReportTest.java`. The third test prices a basket through
`PricingService`, which needs `priceOrThrow` from Lab 4, so for now run the first two by name:

```bash
./mvnw -q test -Dtest='BasketReportTest#addMergesLinesForTheSameSku+groupsLinesByCategoryCatalogueCategory'
```

If the selector syntax fights you, run the whole class and accept one red test until Lab 4.
`addMergesLinesForTheSameSku` also proves something about `Basket.add`: the fourth line added was tea
again, and it merged into the first tea line rather than making a fourth entry.

### Watch out

**`Collectors.toMap` instead of `groupingBy`.** `toMap` throws on a duplicate key; `groupingBy` groups.
A basket with the same SKU twice is a duplicate key.

**A stream used twice.** A stream is consumed by its terminal operation; a second terminal operation on
the same stream throws `IllegalStateException: stream has already been operated upon or closed`. Build a
new one from the list each time; that is what every method here does.

**`reduce(Money::plus)` without an identity** returns `Optional<Money>`, not `Money`. Give it
`Money.zero(...)` and you get a `Money` back, zero for an empty basket.

### Challenge

Write `Map<String, Money> totalByCategory(Quote quote, CatalogService catalog)` with one `groupingBy`
whose downstream is a `reducing`. Then write it as `groupingBy` plus `mapping` plus `reducing` and decide
which reads better.

### In the real world

The product listing of Demo 9 computes facet counts, which is `groupingBy` with `counting()`, first in
Java and then pushed down into SQL. Knowing the collector form is what lets you recognise the SQL form.

---

## Lab 4 — Optional, exceptions and the rules of a clean API (20 min)

### Problem

Open `catalog/CatalogService.java`. `get` currently ends with `.orElse(null)`. A caller that forgets the
check gets a `NullPointerException` three stack frames later with no SKU in the message. Open
`pricing/PriceList.java`: `priceOrThrow` throws `UnsupportedOperationException`, which is why six price
tests and all the pricing tests are still red. The question both files ask is the same: when a lookup
finds nothing, what does the method say?

### Concept

**There are two kinds of "not found", and they deserve two different signatures.**

- *Absence is normal.* A search box types a SKU; it may not exist. Return `Optional<Product>` and make
  the caller decide. `CatalogService.find` does this.
- *Absence is a bug upstream.* Checkout asks for a SKU that is already in the basket; if the catalogue
  does not have it, something is badly wrong, and no caller has a sensible branch for it. Throw a named
  exception that carries the data: `ProductNotFoundException(sku)`. `CatalogService.get` does this.

**What neither does is return `null`.** `null` is the absence that does not announce itself.

**`Optional` is a return type, not a field, parameter or collection element.** `orElseThrow(supplier)` is
the bridge from the first kind to the second: the repository answers with an `Optional`, and `get` turns
an empty one into the exception.

**Unchecked exceptions for domain failures.** `ProductNotFoundException extends RuntimeException`: the
caller cannot fix it by catching, so the compiler should not make them write `throws`. Checked exceptions
are for things the caller can act on, like a file that may not exist yet.

### Steps

1. **`TODO(lab-4.1)` — read, do not type.** In `CatalogRepository.findBySku`, the repository wraps a map
   lookup: `Optional.ofNullable(bySku.get(sku))`. That one line is the boundary where a possibly-null map
   value becomes an honest `Optional`. `CatalogService.find` passes it straight through. Delete the comment
   once you have read it.

2. **`TODO(lab-4.2)` — `get` never returns null.** In `CatalogService.java` replace the `orElse(null)` line:

   ```java
   Product product = repository.findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
   ```

   Open `ProductNotFoundException.java`: it keeps the `sku` in a field with an accessor, so a handler in
   Demo 6 can put it into the error response rather than parsing the message.

3. **`TODO(lab-4.3)` — `PriceList.priceOrThrow`.** Replace the throw:

   ```java
   return find(sku, context).map(Price::value).orElseThrow(() -> new NoPriceException(sku, context));
   ```

   `map` on an `Optional` transforms the value if present and stays empty otherwise, so the chain reads:
   find the price, take its value, or explain which SKU and context had none.

### Verify

Delete `@Disabled("enable in Lab 4")` from `CatalogServiceTest.java` and the remaining `@Disabled` lines
from `PricingServiceTest.java`. Run everything:

```bash
./mvnw -q test
```

`MoneyTest`, `DiscountsTest`, `PriceListTest`, `CatalogServiceTest`, `BasketReportTest` and
`PricingServiceTest` are all green: 30 tests. `QuoteBatchTest` is still skipped (Lab 7).
`getThrowsANamedExceptionForAnUnknownSku` checks the message and the `sku` field;
`failsLoudlyWhenAProductHasNoPrice` checks that a basket with an unknown SKU fails before any total is
computed.

### Watch out

**`Optional.get()`.** It throws `NoSuchElementException` with no message when empty, which is `null` with
extra steps. Use `orElseThrow(supplier)`, `map`, `orElse` or `ifPresent`.

**`orElse(expensiveCall())` vs `orElseGet(() -> expensiveCall())`.** `orElse` evaluates its argument
always; `orElseGet` only when needed. For a constant, `orElse`; for a computation, `orElseGet`.

**`isPresent()` followed by `get()`.** That is a null check with more typing. If you find yourself writing
it, the `Optional` wanted to be a `map` or an `ifPresent`.

### Challenge

`CatalogService.inCategory` returns an empty list for an unknown category. Should it? Write down the case
for `List`, for `Optional<List>` and for an exception, then check what `BasketReport.linesByCategory`
would do with each.

### In the real world

Demo 6 maps `ProductNotFoundException` to an HTTP 404 with a `ProblemDetail` body, once, for every
service. That mapping is only possible because the exception is named and carries the SKU; a `null` or a
generic `RuntimeException("not found")` gives the handler nothing to work with.

---

## Lab 5 — Maven: lifecycle, dependencies, the wrapper (20 min)

### Problem

You have been typing `./mvnw -q test` without knowing what it does, and the first run took a minute while
this one takes seconds. Open `pom.xml`. It is 110 lines and you will be asked, on day one of any Java job,
to add a dependency, bump a version, or explain why a test runs in `verify` but not in `compile`.

### Concept

**Maven is a lifecycle, not a script.** `validate → compile → test → package → verify → install → deploy`.
Asking for a phase runs every phase before it. `./mvnw test` compiles main and test sources then runs the
tests; `./mvnw package` does that and builds the jar; `./mvnw verify` does that and runs integration checks.
You never call `compile` then `test`; you call the furthest phase you want.

**A dependency is coordinates plus a scope.** `groupId:artifactId:version`, and `scope` says which
classpaths it joins: `compile` (default, everywhere), `runtime` (not visible to your code, present when it
runs; the PostgreSQL driver), `test` (tests only; JUnit, AssertJ).

**A BOM pins a family of versions at once.** Importing `junit-bom` in `dependencyManagement` means every
JUnit artifact below gets its version from the BOM and you write none. Spring Boot's parent POM, from
Demo 4, is a very large BOM.

**Plugins do the work of each phase.** The compiler plugin honours `maven.compiler.release`; surefire runs
the tests; the jar plugin writes the manifest; exec runs a main class. Each is pinned, because an unpinned
plugin is a build that changes under you.

**The wrapper pins Maven itself.** `./mvnw` downloads the Maven version named in
`.mvn/wrapper/maven-wrapper.properties` the first time, then uses it. Everyone on the team, and the CI
server, builds with the same Maven without installing anything.

### Steps

1. **Read `pom.xml` top to bottom**, with this map:
   `<properties>` (the Java level and every version in one place), `<dependencyManagement>` (the BOM),
   `<dependencies>` (three test-scoped, one runtime-scoped), `<build><plugins>` (four, each pinned).

2. **Walk the lifecycle.** Run each and watch what appears under `target/`:

   ```bash
   ./mvnw -q clean
   ./mvnw -q compile        # target/classes
   ./mvnw -q test           # target/test-classes, target/surefire-reports
   ./mvnw -q package        # target/shopscope-monolith-0.1.0.jar
   ```

3. **See the dependency tree.** Find JUnit's transitive dependencies and the scope on each line:

   ```bash
   ./mvnw dependency:tree
   ```

4. **Run the jar.** The jar plugin wrote `Main-Class: com.shopscope.monolith.QuoteDemo` into the manifest:

   ```bash
   java -jar target/shopscope-monolith-0.1.0.jar
   ```

5. **Run through Maven instead**, which also works before `package` because exec uses `target/classes`:

   ```bash
   ./mvnw -q exec:java
   ```

6. **Change the Java level and change it back.** Set `maven.compiler.release` to `11`, run
   `./mvnw -q compile`, read the error (records are a Java 16 feature). Set it back to `17`.

### Verify

Steps 4 and 5 both print the same quote:

```
  Assam breakfast tea 250g     x2    INR 698.00
  Oat biscuits 400g            x3    INR 387.00
  Electric kettle 1.7 L        x1   INR 2299.00
  Subtotal                           INR 3384.00
  Discount                             INR 198.80  [TEA10, BISCUITS-3-FOR-2]
  Tax                                  INR 573.34
  Total                              INR 3758.54
  Summary[units=6, subtotal=INR 3384.00]
```

Tea at 10% off is 69.80; three biscuits is one free at 129.00; the kettle is at its India price. Check the
arithmetic once by hand; it is the arithmetic you wrote in Labs 1 and 2.

### Watch out

**`java -jar` says "no main manifest attribute".** The jar plugin's `<mainClass>` is what writes it; without
that configuration a jar is just a zip of classes.

**The PostgreSQL driver "is not found" from `java -jar`.** It is `runtime` scope and the jar does not bundle
dependencies. That is fine today (Lab 8 runs through Maven) and it is what Spring Boot's executable jar
solves in Demo 4.

**Bumping a version in one `<dependency>` when a BOM manages it.** The BOM wins silently. Change the
property the BOM reads instead.

### Challenge

Run `./mvnw -q test -Dsurefire.printSummary=true -Dtest=MoneyTest` and then
`./mvnw help:effective-pom | grep -A3 surefire`. The effective POM is what Maven actually runs: your file
merged with the defaults. Find where the surefire version you pinned appears.

### In the real world

Every CI pipeline in Track 6 is `./mvnw -B verify` with caching of `~/.m2`. The wrapper is what makes the
pipeline and your laptop build the same thing.

---

## Lab 6 — JUnit and AssertJ: the first tests (20 min)

### Problem

`MoneyTest` has one `plusAddsExactly` with one example. Rounding bugs live at the edges: `0.10 + 0.20`,
`999.99 + 0.01`, a half-cent. One example each is three copies of the same method; and `percent` has no
test at all, which means Lab 1's rounding mode has never been checked.

### Concept

**A parameterized test is one method, many rows.** `@ParameterizedTest` replaces `@Test`; `@CsvSource`
supplies the rows; the method takes the columns as parameters. JUnit converts `"18"` to an `int` for you.
The `name` attribute makes the report readable: `{0} + {1} = {2}`.

**AssertJ reads like the sentence you would say.** `assertThat(actual).isEqualTo(expected)`,
`assertThatThrownBy(() -> ...).isInstanceOf(...).hasMessageContaining(...)`,
`assertThat(list).extracting(Product::sku).containsExactly(...)`. The chain is discoverable with
autocomplete after `assertThat(x).`, which is the practical reason to prefer it over `assertEquals`.

**Records make assertions cheap.** `isEqualTo(Money.of("15.50", "INR"))` works because `Money` has a
value-based `equals`. No getters to compare one by one.

### Steps

Both steps are in **`src/test/java/com/shopscope/monolith/shared/MoneyTest.java`**.

1. **`TODO(lab-6.1)` — parameterize `plusAddsExactly`.** Add the imports:

   ```java
   import org.junit.jupiter.params.ParameterizedTest;
   import org.junit.jupiter.params.provider.CsvSource;
   ```

   Replace the single-example test with:

   ```java
   @ParameterizedTest(name = "{0} + {1} = {2}")
   @CsvSource({
           "10.00, 5.50, 15.50",
           "0.10, 0.20, 0.30",
           "999.99, 0.01, 1000.00"
   })
   void plusAddsExactly(String left, String right, String expected) {
       assertThat(Money.of(left, "INR").plus(Money.of(right, "INR"))).isEqualTo(Money.of(expected, "INR"));
   }
   ```

2. **`TODO(lab-6.2)` — test the rounding.**

   ```java
   @ParameterizedTest(name = "{1}% of {0} is {2}")
   @CsvSource({
           "200.00, 18, 36.00",
           "349.00, 10, 34.90",
           "0.05, 50, 0.02"
   })
   void percentRoundsHalfEven(String amount, int percent, String expected) {
       assertThat(Money.of(amount, "INR").percent(percent)).isEqualTo(Money.of(expected, "INR"));
   }
   ```

   The last row is the point: 50% of 0.05 is 0.025, and `HALF_EVEN` rounds to the even neighbour, 0.02.
   `HALF_UP` would give 0.03. Change the mode in `Money.percent` for a moment and watch this row fail.

   Delete both `TODO` comments.

### Verify

```bash
./mvnw -q test -Dtest=MoneyTest
```

Twelve green: the six rows count as six tests. Open
`target/surefire-reports/com.shopscope.monolith.shared.MoneyTest.txt` and read the names; they are the
`name` templates filled in.

### Watch out

**`@Test` and `@ParameterizedTest` on the same method.** JUnit runs it twice, once with no arguments, and
that run fails. One annotation per method.

**A `CsvSource` row with a comma inside a value.** Quote the value with single quotes:
`"'1,000.00', 1, 1000.00"`. Easier: avoid thousands separators in test data.

**AssertJ's `isEqualTo` on `BigDecimal`.** It uses `equals`, so scale matters. For a bare `BigDecimal` use
`isEqualByComparingTo`; for `Money`, `isEqualTo` is right because the record fixed the scale.

### Challenge

Write a `@ParameterizedTest` for `PriceScope.precedence` with a `@MethodSource` that returns the four
scopes and their ranks. `@MethodSource` points at a static method returning a `Stream<Arguments>`.

### In the real world

Demo 11 adds Mockito and test-data builders; Track 3 goes to property-based and mutation testing. The
parameterized test is the one you will write most: every validation rule, every rounding case, every
status-code mapping is a table.

---

## Lab 7 — Executors and futures: quoting a thousand baskets at once, and the one line that becomes virtual threads on Java 21+ (10 min)

### Problem

A nightly job re-prices every open basket. `concurrent/QuoteBatch.quoteAll` currently does it one basket
at a time in a stream. A thousand baskets at a few milliseconds each is fine; a thousand baskets that each
call a pricing service over the network is not.

### Concept

**The shape of a batch is: tasks, an executor, futures.** Wrap each unit of work in a `Callable<Quote>`,
hand the list to an `ExecutorService`, get a `Future<Quote>` per task, and `get()` each future in order.
The executor decides how many run at once; your code does not.

**`invokeAll` waits for all of them and returns the futures in the same order as the tasks.** That is what
keeps `quotes.get(0)` matching `baskets.get(0)`.

**Shut the executor down in `finally`.** A fixed pool's threads are not daemon threads; forget this and
the JVM does not exit.

**This only works because `PricingService` and `Basket` are immutable.** A thousand threads read the same
price list and the same rules at once, and nothing needs a lock, because nothing is written.

> **On Java 21 and later**, replace `Executors.newFixedThreadPool(POOL_SIZE)` with
> `Executors.newVirtualThreadPerTaskExecutor()`: one virtual thread per basket, cheap enough that the
> pool size stops being a decision, and `ExecutorService` becomes `AutoCloseable` so the `finally` turns
> into try-with-resources. The rest of the method does not change, which is the point of learning the
> shape on 17.

### Steps

1. **`TODO(lab-7.1)`** in `src/main/java/com/shopscope/monolith/concurrent/QuoteBatch.java`. Replace the
   sequential `return` with:

   ```java
   ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
   try {
       List<Callable<Quote>> tasks = baskets.stream()
               .<Callable<Quote>>map(basket -> () -> pricing.quote(basket, context))
               .toList();
       List<Future<Quote>> futures = executor.invokeAll(tasks);
       return futures.stream().map(QuoteBatch::join).toList();
   } catch (InterruptedException e) {
       Thread.currentThread().interrupt();
       throw new IllegalStateException("Interrupted while quoting", e);
   } finally {
       executor.shutdown();
   }
   ```

   `join` is already in the file: it unwraps `Future.get()` and rethrows the cause of an
   `ExecutionException` so a failed quote fails the batch with the real exception. The
   `.<Callable<Quote>>map(...)` type witness tells the compiler the lambda is a `Callable`, not a
   `Supplier`.

### Verify

Delete `@Disabled("enable in Lab 7")` from `QuoteBatchTest.java` and run
`./mvnw -q test -Dtest=QuoteBatchTest`. One green test: a thousand quotes, the first for one biscuit
packet, the last for a thousand, in order.

### Watch out

**`future.get()` swallowing the real error.** It throws `ExecutionException` wrapping whatever the task
threw. Unwrap `getCause()` or you will debug the wrapper.

**Catching `InterruptedException` and doing nothing.** Re-set the interrupt flag with
`Thread.currentThread().interrupt()` so the caller still knows.

**Parallel streams as the shortcut.** `baskets.parallelStream()` works for CPU-bound work on the common
pool; for anything that waits on a network it starves every other parallel stream in the JVM. The
explicit executor is the version you can reason about.

### Challenge

Time both versions on 10,000 baskets with `System.nanoTime()`. Then add `Thread.sleep(5)` inside
`PricingService.quote` to fake a network call and time them again. The second number is why the lab exists.

### In the real world

Demo 24's notification service sends e-mail asynchronously with exactly this shape behind Spring's
`@Async` and a task executor; Demo 21's resilience patterns put timeouts and bulkheads on the executor.
Virtual threads are one property in Spring Boot (`spring.threads.virtual.enabled`) once you run on 21+.

---

## Lab 8 — An SQL refresher on the ShopScope schema, and plain JDBC once (30 min)

### Problem

`SeedData.prices()` is a Java list. Real prices live in a database, and from Demo 7 Spring Data JPA will
read and write them for you. Before a library hides the database, you should see the SQL it generates and
the JDBC it wraps, once, with your own hands.

### Concept

**A relational schema is tables, keys and constraints.** `product` has a primary key (`sku`); `price`
has a foreign key to it, a `check` that keeps amounts non-negative and scope types in the allowed set, and
an index on `(sku, currency)` because that is how every lookup starts. Open
`src/main/resources/sql/schema.sql` and read it; the comments at the top are the commands you run.

**Price selection is a query.** The question Lab 2 answered in Java, "which price applies to this
shopper", is a `where` clause and an `order by` in SQL. Ask it both ways today; Demo 7 compares them.

**JDBC is four objects.** `DriverManager` gives a `Connection`; a `PreparedStatement` holds SQL with `?`
placeholders that you bind (never concatenate); `executeQuery` returns a `ResultSet` you walk with
`next()`; and every one of them must be closed, which try-with-resources does in reverse order even when
an exception is thrown.

### Steps

1. **Start PostgreSQL and load the schema**, from the starter folder, with the two commands at the top of
   `schema.sql`:

   ```bash
   docker run --rm -d --name shopscope-pg -e POSTGRES_PASSWORD=shopscope -e POSTGRES_USER=shopscope -e POSTGRES_DB=shopscope -p 5432:5432 postgres:17
   docker exec -i shopscope-pg psql -U shopscope -d shopscope < src/main/resources/sql/schema.sql
   ```

2. **Ask the price-selection question in SQL.** Open a shell:

   ```bash
   docker exec -it shopscope-pg psql -U shopscope -d shopscope
   ```

   and run:

   ```sql
   select sku, amount, scope_type, scope_value from price where sku = 'SKU-KET-001' and currency = 'INR';
   ```

   Two rows. Now rank them the way `PriceScope.precedence` does and pick the winner for a shopper in India:

   ```sql
   select amount, scope_type, scope_value
   from price
   where sku = 'SKU-KET-001' and currency = 'INR'
     and (scope_type = 'BASE' or (scope_type = 'COUNTRY' and scope_value = 'IN'))
   order by case scope_type when 'CUSTOMER_GROUP' then 3 when 'CHANNEL' then 2 when 'COUNTRY' then 1 else 0 end desc
   limit 1;
   ```

   `2299.00`, the India price. Try `join product using (sku)` to add the product name, then `\q`.

3. **`TODO(lab-8.2)` — the loader**, in `src/main/java/com/shopscope/monolith/jdbc/JdbcPriceLoader.java`.
   Replace the comment and the bare `return prices;` with:

   ```java
   List<Price> prices = new ArrayList<>();
   // try-with-resources closes the result set, the statement and the connection, in that order, even on error.
   try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
        PreparedStatement statement = connection.prepareStatement(SELECT)) {
       statement.setString(1, currency);
       try (ResultSet rows = statement.executeQuery()) {
           while (rows.next()) {
               prices.add(new Price(
                       rows.getString("sku"),
                       Money.of(rows.getBigDecimal("amount"), java.util.Currency.getInstance(rows.getString("currency"))),
                       scope(rows.getString("scope_type"), rows.getString("scope_value"))));
           }
       }
   }
   return prices;
   ```

   `scope(...)` is already there: it turns the two string columns back into a `PriceScope`. That mapping,
   row to record, is the job an ORM automates.

4. **Run it.** There is no test for this lab (it needs Docker) and the exec plugin is wired to `QuoteDemo`.
   Add a temporary `main` to `JdbcPriceLoader`:

   ```java
   public static void main(String[] args) throws SQLException {
       System.out.println(load("jdbc:postgresql://localhost:5432/shopscope", "shopscope", "shopscope", "INR").size() + " INR prices");
   }
   ```

   then run it through Maven so the runtime-scoped driver is on the classpath:

   ```bash
   ./mvnw -q compile exec:java -Dexec.mainClass=com.shopscope.monolith.jdbc.JdbcPriceLoader
   ```

   Remove the `main` afterwards; the finished code does not keep it.

### Verify

Step 2 prints `2299.00 | COUNTRY | IN`. Step 4 prints `7 INR prices`, one per INR row in the insert
statement. Stop the container when you are done: `docker stop shopscope-pg`.

### Watch out

**String concatenation into SQL.** `"where sku = '" + sku + "'"` is how SQL injection happens. The `?`
and `setString` are not optional.

**Forgetting to close.** A `Connection` that is never closed is a leaked database connection; after a few
hundred, the database refuses new ones. Try-with-resources is the only form you should write.

**`rows.getBigDecimal("amount")` on a column declared `numeric(12,2)`.** It comes back with scale 2;
`Money`'s constructor keeps it. Had the column been `float`, you would already have lost cents.

### Challenge

Write the SQL for `PriceList.find` with the customer group and channel included, as one query with
parameters for all four context values. Then count the branches it replaced in Lab 2.

### In the real world

Demo 7 writes `priceList.find(...)` as a Spring Data query method and shows the SQL Hibernate generates
for it. If the generated SQL surprises you, this lab is where you learned to read it.

---

## Lab 9 — A first document in MongoDB (10 min)

### Problem

A customer has an address book. In SQL that is a second table, a foreign key and a join on every read.
In Demo 13 the customer service moves to MongoDB, where the addresses live inside the customer. See the
shape once now, so the choice in Demo 13 is a comparison and not a leap.

### Concept

**A document is a nested record stored whole.** The customer, its addresses, and any field one customer has
and another does not, in one JSON-like object under one `_id`. Reads return the whole thing; there is no
join because there is nothing to join.

**No schema is enforced by default.** Two documents in the same collection can have different fields. That
is freedom when the data genuinely varies, and a trap when it does not; the study note says when each
applies.

### Steps

1. **Start MongoDB and open its shell**, with the commands at the top of `docs/lab-9-first-document.js`:

   ```bash
   docker run --rm -d --name shopscope-mongo -p 27017:27017 mongo:8
   docker exec -it shopscope-mongo mongosh shopscope
   ```

2. **Paste the script one statement at a time.** Insert Priya with two addresses, read her back, query
   inside the array for `"addresses.city": "Bengaluru"`, update one address in place with the positional
   `$`, and insert Arjun with a field Priya does not have.

### Verify

`findOne` returns the whole customer with both addresses nested. The update changes only the office line.
`countDocuments()` is `2`. `exit`, then `docker stop shopscope-mongo`.

### Watch out

**Querying `addresses.city` without quotes.** The dotted path is a string key; `{ addresses.city: ... }`
is a syntax error.

**Expecting the update to touch both addresses.** The positional `$` updates the first array element that
matched the query, which is the office address because the query named its label.

### Challenge

Model the same customer in SQL: two tables, the foreign key, and the query that returns a customer with
all addresses. Count the rows and the joins. Then write down one question each store answers better.

### In the real world

Demo 13 builds `customer-service` on exactly this document with Spring Data MongoDB, and the embedded
address book is why. The order service stays on SQL, because an order is a ledger and a ledger wants
transactions.

---

## Lab 10 — HTTP essentials with curl (15 min)

### Problem

Demo 1 gives the monolith a hand-written HTTP layer, and from Demo 5 everything the storefront does is an
HTTP request to the `/api` contract. Status codes, methods and headers are the vocabulary of every later
demo. Learn it against a public API where nothing is yours yet.

### Concept

**The method says what you intend; the status code is the answer.** `GET` reads and is safe; `PUT` and
`DELETE` can be repeated without changing the outcome (idempotent); `POST` creates and is not. `2xx` means
done, `4xx` means you got it wrong, `5xx` means the server did. `404` is an answer, not an error page.

**Two headers carry the contract.** `Content-Type` says what you are sending; `Accept` says what you want
back. JSON on both sides, for this whole track.

**The URL carries the paging.** `?limit=3&skip=3` is state in the request, which is why it can be
bookmarked and cached.

### Steps

1. **Open `http/lab-10-http-essentials.http`** in IntelliJ or VS Code (REST Client) and run the six
   requests in order, reading the status line and the headers of each response before the body.

2. **Run the same requests with curl**, so you have seen the raw form:

   ```bash
   curl -i https://dummyjson.com/products/1
   curl -i https://dummyjson.com/products/99999
   curl -s "https://dummyjson.com/products?limit=3&skip=3&select=title,price"
   curl -i -X POST https://dummyjson.com/products/add -H "Content-Type: application/json" -d '{"title":"ShopScope stoneware mug","price":499}'
   curl -i -X PATCH https://dummyjson.com/products/1 -H "Content-Type: application/json" -d '{"price":549}'
   curl -i -X DELETE https://dummyjson.com/products/1
   ```

   `-i` shows the response headers, `-X` sets the method, `-H` adds a header, `-d` sends a body.

3. **Send the DELETE twice** and compare the two answers. Then send the POST twice and compare the ids.

### Verify

Request 1 is `200` with `Content-Type: application/json`. Request 2 is `404`. Request 4 is `201` with the
created product echoed back and a new `id`. The second DELETE answers the same as the first (idempotent);
the second POST makes a second product (not idempotent).

### Watch out

**A body with no `Content-Type`.** Many servers answer `415` or silently treat it as a form. Always send
the header with a JSON body.

**Reading only the body.** The status line and headers are where the contract lives; `curl -i` or the
REST Client's response pane show them. Train the habit now.

### Challenge

Write, in one line each, what the monolith's `/api/checkout` should answer for: a successful order, a
declined card, an unknown SKU, and a request body that is not JSON. Keep the note; Demo 1 Lab 7 shows what
the hand-rolled version actually does, and Demo 6 fixes it.

### In the real world

Every `.http` file in this track grows from this one. By Demo 16 the collection covers the whole `/api`
contract through the gateway, and Track 3 turns it into contract tests.

---

## Wrap-up — what you can now do

- Model a value with a record, validate and normalise it in a compact constructor, and keep it immutable.
- Describe a closed set of cases as a sealed interface and branch over it with `instanceof` patterns.
- Answer questions over a collection with one stream pipeline and the right collector.
- Decide between `Optional` and a named unchecked exception, and never return `null`.
- Read a POM, run the lifecycle, explain scopes and a BOM, and run a main class three ways.
- Write a parameterized test with readable names and fluent assertions.
- Run a batch of work on an executor and collect the futures in order, on any Java from 17 up.
- Query a relational table, map rows by hand with JDBC, insert and query a document, and read an HTTP
  response from the status line down.

Everything you typed today is in `../01-shopscope-as-a-plain-java-monolith/starter`, which is where the
next demo begins.

## Next demo

[Demo 1 — ShopScope as a plain-Java monolith](../01-shopscope-as-a-plain-java-monolith/): the rest of the
backend arrives around today's code, in one process, wired with `new`. It works, and the demo is about
finding the nine reasons it will not keep working.

## Interview questions

1. **When would you choose a record over a class?** When the type is defined by its data: value objects,
   DTOs, keys. Records give you equality by value, immutability and a compact constructor for validation.
   Choose a class when identity matters or state must change.

2. **What does `sealed` give you that `final` or an ordinary interface does not?** A closed set of
   subtypes the compiler knows. Code that handles every permitted case is provably complete; on Java 21+
   a `switch` over it needs no `default` and fails to compile when a case is added.

3. **What is pattern matching for `instanceof`?** Test and bind in one expression:
   `if (x instanceof Foo f)` makes `f` available as a `Foo` without a cast, and `&&` guards can use it.

4. **`groupingBy` versus `toMap`?** `groupingBy` collects all elements per key into a list (or a
   downstream collector); `toMap` maps one element per key and throws on duplicates unless you give it a
   merge function.

5. **Why does `reduce` need an identity?** Without one the result is `Optional`, because an empty stream
   has no value to return. With `Money.zero(currency)` the result is always a `Money`.

6. **When should a method return `Optional` and when should it throw?** `Optional` when absence is a
   normal outcome the caller will branch on. Throw a named unchecked exception when absence means a bug
   or an invariant broken that the caller cannot sensibly handle.

7. **Checked or unchecked for a domain exception?** Unchecked. Callers cannot fix "product not found"
   by catching it locally; forcing `throws` through every layer adds noise and nothing else.

8. **What is a Maven BOM?** A POM imported in `dependencyManagement` that pins versions for a family of
   artifacts, so dependencies below it carry no version and always agree with each other.

9. **Difference between `runtime` and `test` scope?** `runtime` is absent at compile time and present
   when the application runs (a JDBC driver). `test` is present only for compiling and running tests.

10. **Why `./mvnw` rather than `mvn`?** The wrapper pins the Maven version in the repository, so every
    machine and the CI server build with the same Maven without installing it.

11. **What does `invokeAll` guarantee?** It blocks until every task completes and returns the futures in
    the order of the tasks, so results can be matched to inputs by index.

12. **Why bind parameters in JDBC instead of concatenating?** Bound parameters are sent separately from
    the SQL text, so user input can never change the query's structure. Concatenation is SQL injection.

13. **When is a document store the better fit than a relational table?** When an aggregate is read and
    written whole and its shape varies between instances: a customer with an address book. When the data
    is a ledger with cross-row invariants, stay relational.

## Troubleshooting

**`java -version` shows 11 or lower.** The code needs 17. Install a current JDK (Temurin, Corretto or
your package manager's `openjdk@17` or newer) and make sure `JAVA_HOME` points at it; the wrapper uses
`JAVA_HOME` first.

**`./mvnw: Permission denied`.** `chmod +x mvnw` once. On Windows use `mvnw.cmd`.

**"Every test was skipped, is something wrong?"** No. The starter disables every test class until its
lab; `./mvnw verify` is green on purpose. Each lab's Verify step tells you which `@Disabled` to delete.

**`Tests run: 0` for a class you enabled.** Check that you deleted the class-level `@Disabled` and not
just an import, and that the test class name ends in `Test` (surefire's default pattern).

**Docker is not running (Labs 8 and 9).** Start Docker Desktop, then rerun the `docker run` command. If
you cannot run Docker at all, read the two labs; nothing later in Demo 0 or Demo 1 depends on them.

**`port 5432 already in use`.** A local PostgreSQL is running. Either stop it, or start the container on
another port (`-p 5433:5432`) and use `localhost:5433` in the JDBC URL.

**The tests pass in the IDE but `./mvnw test` fails, or the reverse.** The IDE may be using a different
JDK. Set the project JDK to the same one `java -version` shows in your terminal.
