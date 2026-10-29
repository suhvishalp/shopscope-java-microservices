# Module 01 — Modern Java for Spring

**Study notes** · ~3–4 hours

> **Goal.** Be fluent in the Java that Spring Boot 4 code is written in: records as value
> objects, sealed interfaces, pattern matching with `instanceof`, streams and collectors,
> `Optional` used properly, exceptions that carry meaning, and the executor API. Every example
> is taken from the ShopScope pricing domain you build in Demo 0, so the code you read here is
> the code you will type there. Everything compiles on Java 17; the few things Java 21 adds are
> marked as asides.

**Prerequisites:** Java syntax, classes and interfaces, generics, collections. If `List<T>`
and `interface` are new to you, read a Java introduction first.

---

## Contents

1. [Records: a value object in one line](#1-records-a-value-object-in-one-line)
2. [Immutability: return a new one](#2-immutability-return-a-new-one)
3. [Sealed interfaces: all the cases, known to the compiler](#3-sealed-interfaces-all-the-cases-known-to-the-compiler)
4. [Pattern matching with `instanceof`](#4-pattern-matching-with-instanceof)
5. [Lambdas and method references](#5-lambdas-and-method-references)
6. [Streams and collectors](#6-streams-and-collectors)
7. [`Optional`, done right](#7-optional-done-right)
8. [Exceptions for a clean API](#8-exceptions-for-a-clean-api)
9. [Text blocks and `var`](#9-text-blocks-and-var)
10. [Concurrency essentials: executors and futures](#10-concurrency-essentials-executors-and-futures)
11. [Common mistakes](#11-common-mistakes)
12. [Self-check](#12-self-check)
13. [References](#13-references)

---

## 1. Records: a value object in one line

A **value object** is a thing identified by its value, not by an identity. Two amounts of
`INR 10.00` are the same amount; there is no "this INR 10.00" versus "that INR 10.00". Money,
a price, a line item, a price context: all values.

Before Java 16 a value object was fifty lines: private final fields, a constructor, getters,
`equals`, `hashCode`, `toString`. A **record** is the same thing in one line:

```java
public record Price(String sku, Money value, PriceScope scope) {}
```

The compiler generates a constructor taking the three components, an accessor per component
(`price.sku()`, not `getSku()`), and `equals`, `hashCode` and `toString` based on all
components. The fields are `private final`. A record cannot extend a class, and nothing can
extend a record; it is a transparent carrier for its components, and the language enforces it.

### Compact constructors

You usually want validation. A **compact constructor** has no parameter list and runs before
the fields are assigned; it can check and even reassign the parameters:

```java
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
    }
}
```

Three things to notice. The null checks mean a `Money` cannot exist without a currency.
The reassignment of `amount` **normalises** the value: `new BigDecimal("10")` and
`new BigDecimal("10.00")` are not equal as `BigDecimal`s (`equals` compares scale), so without
normalisation `Money.of("10", "INR")` and `Money.of("10", "INR")` could differ. After the
compact constructor both carry `10.00`. And the last line is written `amount = ...`, not
`this.amount = ...`: you are adjusting the parameter, and the generated assignment runs after.

### Equality

Records compare **by components**. That is why `assertThat(Money.of("10", "INR")).isEqualTo(Money.of("10.00", "INR"))`
passes in `MoneyTest`: both normalise to `10.00`, and `equals` compares the two `BigDecimal`s
and the two `Currency`s. It also means a record is a safe key in a `Map` or member of a `Set`,
and that AssertJ's `isEqualTo` works on records without any custom comparator.

### Static factories

Records can have static methods, which is where readable construction lives:

```java
public static Money of(String amount, String currencyCode) {
    return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
}

public static Money zero(Currency currency) {
    return new Money(BigDecimal.ZERO, currency);
}
```

`Money.of("349.00", "INR")` says what it is. Take the `String` overload seriously: writing
`new BigDecimal(349.00)` with a `double` gives you `349` with floating-point noise behind it.
Money is never a `double`.

### When not to use a record

A record is for data that is defined by its components. If the thing has an identity that
outlives its data (a customer whose e-mail changes but who stays the same customer) or needs
mutable state, it is a class. In Demo 7, JPA entities are classes for exactly that reason, and
DTOs stay records.

---

## 2. Immutability: return a new one

A record's fields are final, so there is no `setAmount`. Every "change" is a method that
returns a new value:

```java
public Money plus(Money other) {
    requireSameCurrency(other);
    return new Money(amount.add(other.amount), currency);
}

public Money times(int quantity) {
    return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
}
```

Why it matters: a `Money` can be passed to any method, stored in any collection, read from any
thread, and nobody can change it under you. In `QuoteBatch` a thousand threads quote baskets
that share the same `PriceList` and `Money` values, and there is no lock anywhere, because there
is nothing to protect.

The same rule applies to collections inside a record. `Basket` holds a `List<LineItem>`; its
compact constructor does `lines = List.copyOf(lines)`, which takes a defensive copy and makes it
unmodifiable. Adding a line is a method that builds a new list and a new basket:

```java
public Basket add(LineItem line) {
    List<LineItem> merged = new ArrayList<>(lines);
    // ... merge or append ...
    return new Basket(customerId, merged);
}
```

The caller writes `basket = basket.add(line)`. If you forget the assignment, nothing happens,
which is the one habit to build: with immutable values, the result is always the return value.

---

## 3. Sealed interfaces: all the cases, known to the compiler

An ordinary interface can be implemented by anyone, anywhere. Sometimes that is the point
(`TaxRule` will have implementations we have not written yet). Sometimes it is wrong: a price
scope is one of exactly four things, and a fifth kind should be a deliberate change, not
something a stray class can add.

A **sealed interface** lists its permitted implementations:

```java
public sealed interface PriceScope
        permits PriceScope.Base, PriceScope.ForCountry, PriceScope.ForChannel, PriceScope.ForCustomerGroup {

    record Base() implements PriceScope {}
    record ForCountry(String country) implements PriceScope {}
    record ForChannel(String channel) implements PriceScope {}
    record ForCustomerGroup(String customerGroup) implements PriceScope {}
}
```

The implementations are nested records: each case is a small value with the data that case
needs (`Base` needs none). Permitted classes must be `final`, `sealed` or `non-sealed`; records
are implicitly final, so they just work.

What `permits` buys you is **exhaustiveness**. The compiler knows every case, so code that
handles "all the scopes" can be checked. On Java 17 that check is something you enforce by
convention in an `instanceof` chain (section 4); on Java 21 it is enforced by the compiler in a
`switch`. Either way, when a fifth scope is added, you can find every place that must change by
searching for the type, and in the `switch` form the compiler finds them for you.

`DiscountRule` is the second sealed hierarchy in ShopScope: `PercentageOff`, `AmountOff`,
`BuyXGetYFree` and `BasketOver`. `PaymentResult` in Demo 1 is the third: `Approved` or
`Declined`, nothing else, ever.

A sealed interface can still have default methods that work for every case, which is how
`PriceScope.appliesTo` and `precedence()` are written.

---

## 4. Pattern matching with `instanceof`

The old way to branch on a type was a test, then a cast:

```java
if (rule instanceof PercentageOff) {
    PercentageOff r = (PercentageOff) rule;
    ...
}
```

Since Java 16 the test binds a variable in one step:

```java
if (rule instanceof PercentageOff r && r.sku().equals(line.sku())) {
    return line.lineTotal().percent(r.percent());
}
```

`r` is in scope wherever the compiler can prove the test passed, which includes the right-hand
side of the `&&`. That `&&` is a **guard**: the branch applies to percentage rules *for this
SKU*. `Discounts.lineDiscount` is one `instanceof` chain over the sealed `DiscountRule`:

```java
public static Money lineDiscount(DiscountRule rule, PricedLine line) {
    Money zero = Money.zero(line.unitPrice().currency());
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
    return zero;
}
```

The variable name `r` is reused because each binding is scoped to its own `if`. The final
`return zero` is the "none of the above" case: a rule for another SKU, or a basket-level rule.

`PriceScope.appliesTo` uses the same shape on `this`, which is legal inside a default method:

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

> **On Java 21 and later.** The same logic is a `switch` with type patterns, record patterns
> and `when` guards, and the compiler rejects it if a sealed case is missing:
>
> ```java
> return switch (rule) {
>     case PercentageOff(var code, var sku, var percent) when sku.equals(line.sku()) ->
>             line.lineTotal().percent(percent);
>     case AmountOff(var code, var sku, var amount) when sku.equals(line.sku()) ->
>             amount.times(line.quantity()).min(line.lineTotal());
>     case BuyXGetYFree(var code, var sku, var buy, var free) when sku.equals(line.sku()) ->
>             line.unitPrice().times((line.quantity() / (buy + free)) * free);
>     default -> zero;
> };
> ```
>
> Java 22 adds `_` for components you do not use. None of this is required on this track;
> the `instanceof` form runs everywhere.

---

## 5. Lambdas and method references

A lambda is a function value: `line -> line.quantity()` is a function from a `LineItem` to an
`int`. Java gives it a type from context, via a **functional interface** (an interface with one
abstract method): `Function<LineItem, Integer>`, `Predicate<Price>`, `Supplier<Quote>`,
`Callable<Quote>`, `Comparator<Money>`.

A **method reference** is a lambda that only calls one method, written as the method's name:

| Lambda | Method reference | Kind |
|---|---|---|
| `line -> line.quantity()` | `LineItem::quantity` | instance method of the argument |
| `(a, b) -> a.plus(b)` | `Money::plus` | instance method, first argument is the receiver |
| `p -> Discounts.basketDiscount(p, subtotal)` | (no shorter form, it captures `subtotal`) | |
| `s -> new ProductNotFoundException(s)` | `ProductNotFoundException::new` | constructor |

Prefer the reference when it exists; prefer the lambda when it reads better or captures
a local. A lambda can read local variables that are effectively final (never reassigned), which
is why `subtotal` can be used inside `rule -> Discounts.basketDiscount(rule, subtotal)`.

---

## 6. Streams and collectors

A stream is a pipeline: a **source**, zero or more **intermediate** operations (lazy, return a
stream), and one **terminal** operation (runs the pipeline, returns a result). Nothing happens
until the terminal operation.

```java
Money subtotal = lines.stream()              // source
        .map(PricedLine::lineTotal)          // intermediate
        .reduce(zero, Money::plus);          // terminal
```

`reduce(identity, accumulator)` folds a stream into one value. With `Money::plus` and a zero
in the right currency it is a sum. Read it as "start from zero, add each line total".

### The collectors you will use every week

`BasketReport` is a tour of them. Each method is one pipeline, no loop, no mutable accumulator.

**`groupingBy` with a downstream collector.** Group lines by SKU, and for each group sum the
quantities:

```java
public static Map<String, Integer> quantityBySku(Basket basket) {
    return basket.lines().stream()
            .collect(Collectors.groupingBy(LineItem::sku, Collectors.summingInt(LineItem::quantity)));
}
```

Without the downstream collector, `groupingBy(LineItem::sku)` gives `Map<String, List<LineItem>>`.
The second argument says what to do with each group's elements.

**`groupingBy` with a computed key.** The key can come from anywhere, including another object:

```java
Map<String, List<LineItem>> byCategory = basket.lines().stream()
        .collect(Collectors.groupingBy(line -> catalog.get(line.sku()).category()));
```

**`max` with a comparator** returns an `Optional`, because the stream may be empty:

```java
Optional<PricedLine> top = quote.lines().stream().max(Comparator.comparing(PricedLine::lineTotal));
```

`Comparator.comparing(keyExtractor)` builds a comparator from a function; it needs the key to be
`Comparable`, which `Money` is.

**`partitioningBy`** is `groupingBy` with a boolean key, and it always has both keys:

```java
Map<Boolean, List<PricedLine>> split = quote.lines().stream()
        .collect(Collectors.partitioningBy(line -> line.lineTotal().isGreaterThan(threshold)));
```

**`teeing`** runs two collectors over one pass and merges their results. Units and subtotal
from one traversal:

```java
return quote.lines().stream().collect(Collectors.teeing(
        Collectors.summingInt(PricedLine::quantity),
        Collectors.mapping(PricedLine::lineTotal, Collectors.reducing(zero, Money::plus)),
        (units, subtotal) -> new Summary(units, subtotal)));
```

`Collectors.mapping` adapts a collector to a different element type; `reducing` is `reduce`
as a collector.

**`toList()`** (Java 16) returns an unmodifiable list; `collect(Collectors.toList())` returns a
list whose mutability is unspecified. Use `toList()`.

**`flatMap`** flattens a stream of streams. In `PricingService`, "the discount of every rule on
every line" is rules × lines:

```java
Money lineDiscounts = rules.stream()
        .flatMap(rule -> lines.stream().map(line -> Discounts.lineDiscount(rule, line)))
        .reduce(zero, Money::plus);
```

### Rules that keep streams readable

- One pipeline per question. If a pipeline needs a comment per line, it is two pipelines.
- Intermediate operations must not have side effects. A `map` that also writes to a list is a
  loop in disguise, and a wrong one once the stream is parallel.
- Streams are single-use. Keep the `List`, build a new stream each time.
- A `for` loop is not a failure. `Basket.indexOf` is a loop because it returns early with an
  index; a stream would be longer and slower to read.

---

## 7. `Optional`, done right

`Optional<T>` is a return type that says "there may be nothing here, and that is normal". It
exists so that callers cannot forget the empty case, because the compiler makes them unwrap.

`CatalogService` shows the two sides of the same question:

```java
public Optional<Product> find(String sku) {
    return repository.findBySku(sku);
}

public Product get(String sku) {
    return repository.findBySku(sku).orElseThrow(() -> new ProductNotFoundException(sku));
}
```

`find` is for callers for whom absence is an outcome: a search box, an "is this SKU valid"
check. `get` is for callers for whom absence is a bug: checkout, which only ever asks for SKUs
that are in a basket. Give the caller the version that matches their situation, and never
return `null` from either.

### The API that matters

| Method | Use |
|---|---|
| `orElseThrow(supplier)` | the right default: absence is an error here |
| `map(f)` | transform the value if present; `find(sku).map(Product::name)` |
| `filter(p)` | keep the value only if it passes |
| `ifPresent(consumer)` | do something, no result |
| `orElse(value)` | a default value (evaluated eagerly) |
| `orElseGet(supplier)` | a default value computed only when needed |
| `isPresent()` / `get()` | the test-then-get pair; almost always a `map` or `orElseThrow` in disguise |

`PriceList.find` returns `Optional<Price>` from a stream's `max`; `priceOrThrow` is the `get`
version:

```java
public Money priceOrThrow(String sku, PriceContext context) {
    return find(sku, context).map(Price::value).orElseThrow(() -> new NoPriceException(sku, context));
}
```

### Where `Optional` does not belong

- **Not a field.** A record component of type `Optional` makes equality and serialisation
  awkward; store the nullable value and expose an `Optional` from an accessor if you must.
  `PriceContext` stores `channel` and `customerGroup` as nullable `String`s for this reason.
- **Not a parameter.** `find(Optional<String> sku)` forces every caller to wrap. Overload
  instead.
- **Not for collections.** An empty list already says "nothing"; `Optional<List<T>>` says it
  twice.
- **Not `Optional.get()` without a check.** It throws `NoSuchElementException`, which tells the
  reader nothing. Use `orElseThrow` with your own exception.

---

## 8. Exceptions for a clean API

ShopScope's exceptions are unchecked (`extends RuntimeException`), named for the situation, and
carry the data a handler needs:

```java
public class ProductNotFoundException extends RuntimeException {

    private final String sku;

    public ProductNotFoundException(String sku) {
        super("No product with SKU " + sku);
        this.sku = sku;
    }

    public String sku() {
        return sku;
    }
}
```

Three decisions are in there.

**Unchecked.** A checked exception forces every caller in the chain to declare or catch it.
That is right for "the file was not there" at the boundary where someone can do something about
it; it is wrong for a domain condition that the caller usually cannot repair. Spring itself
throws unchecked exceptions everywhere, and from Demo 6 on, one `@RestControllerAdvice` turns
them into HTTP responses. Unchecked exceptions are what make that possible.

**Named for the situation, not the mechanism.** `ProductNotFoundException`, `NoPriceException`,
`OutOfStockException`, `PaymentDeclinedException`, `CurrencyMismatchException`. A log line with
the class name already says what happened.

**Carries data.** The SKU is a field, not just a substring of the message. In Demo 6 the handler
puts it into the `ProblemDetail` body; a test asserts on it with `extracting("sku")`.

When to throw and when to return `Optional` is the `find`/`get` question from section 7: throw
when the caller cannot proceed without the thing; return `Optional` when absence is an answer.
Never both, and never `null`.

One more rule: validate at construction. `LineItem` refuses a zero quantity, `Price` refuses a
negative amount, `Money` refuses a null currency, each in its compact constructor. An object that
cannot be built wrong needs no defensive checks later.

---

## 9. Text blocks and `var`

A **text block** is a multi-line string literal. SQL is the typical use:

```java
private static final String SELECT = """
        select sku, amount, currency, scope_type, scope_value
        from price
        where currency = ?
        order by sku
        """;
```

Indentation is stripped relative to the closing delimiter, and there is no `\n` noise.
`JdbcPriceLoader` uses this one.

`var` declares a local with an inferred type. Use it when the type is obvious from the right
side (`var fields = new LinkedHashMap<String, String>()`), avoid it when it hides something the
reader needs (`var result = service.process(x)`). `var` is only for locals; fields, parameters
and return types keep explicit types.

---

## 10. Concurrency essentials: executors and futures

Quoting one basket is fast; quoting a thousand, one after another, is a thousand times slower
than it needs to be. The Java answer is an `ExecutorService`: a pool of threads that runs tasks.

```java
public static List<Quote> quoteAll(PricingService pricing, List<Basket> baskets, PriceContext context) {
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
}
```

The pieces:

- A **`Callable<Quote>`** is a task that returns a value and may throw. The lambda
  `() -> pricing.quote(basket, context)` captures the basket.
- **`invokeAll`** submits every task and waits for all of them. It returns the futures in the
  same order as the tasks, which is why `quotes.get(999)` is the quote for basket 999.
- A **`Future<Quote>`** is a handle to a result that may not exist yet. `get()` blocks until
  it does, and wraps any exception from the task in an `ExecutionException`; `QuoteBatch.join`
  unwraps the cause so the caller sees the real failure.
- **`shutdown()` in `finally`.** A pool's threads are non-daemon; forget this and the JVM does
  not exit. Always in a `finally`, so an exception does not leak the pool.
- **`InterruptedException`** means someone asked this thread to stop. Restore the flag with
  `Thread.currentThread().interrupt()` so the request is not lost, then fail.

This works because `PricingService`, `PriceList` and `Money` are immutable (section 2). Nothing
in a quote writes shared state, so a thousand threads need no synchronisation.

> **On Java 21 and later.** Replace one line:
> `Executors.newVirtualThreadPerTaskExecutor()`. A virtual thread is a thread managed by the
> JVM rather than the operating system, cheap enough to create one per task and discard it, so
> the pool size stops being a decision. `ExecutorService` is also `AutoCloseable` from 21, so the
> executor can go in a try-with-resources and the `finally` disappears. The surrounding code,
> callables, `invokeAll`, futures, does not change, which is why learning it on a fixed pool is
> not wasted.

---

## 11. Common mistakes

**`new BigDecimal(0.1)`.** The `double` constructor captures floating-point noise. Use the
`String` constructor or `BigDecimal.valueOf(long)`.

**Forgetting that records compare scale.** `new BigDecimal("10")` and `new BigDecimal("10.00")`
are not equal. Normalise in the compact constructor, as `Money` does.

**Mutating a parameter instead of returning a new value.** `basket.add(line);` on its own does
nothing. The result is the return value.

**`Optional.get()` and `orElse(null)`.** Both reintroduce the problem `Optional` solves.

**Reusing a stream.** A stream is consumed by its terminal operation. Keep the collection.

**A side effect inside `map`.** It runs once per element, maybe in parallel, maybe not at all for
elements that are filtered later. Collect, then act.

**A `catch (Exception e)` that logs and continues.** `ShopHttpServer.checkout` in Demo 1 does
this on purpose, and the result is a 500 with a `NumberFormatException` in the body. Catch what
you can handle; let the rest propagate to one place that knows how to answer.

**An executor without `shutdown`.** The program never exits and you spend an hour finding out
why.

---

## 12. Self-check

1. Why does `Money`'s compact constructor reassign `amount`, and what would break if it did not?
2. What does a record generate for you, and what can you add to it?
3. What does `permits` on a sealed interface give you that an ordinary interface does not?
4. In `rule instanceof PercentageOff r && r.sku().equals(line.sku())`, where is `r` in scope and why?
5. Rewrite `basket.lines().stream().map(l -> l.quantity()).reduce(0, (a, b) -> a + b)` with
   method references and a collector.
6. What is the difference between `groupingBy(LineItem::sku)` and
   `groupingBy(LineItem::sku, summingInt(LineItem::quantity))`?
7. `CatalogService` has both `find` and `get`. Which should a checkout call, and why?
8. Why are ShopScope's domain exceptions unchecked, and what do they carry besides a message?
9. Why does `QuoteBatch` call `executor.shutdown()` in a `finally` block?
10. What is the only line that changes in `QuoteBatch` on Java 21, and what does not change?

---

## 13. References

- Records: [JEP 395](https://openjdk.org/jeps/395), and the Java Language Specification §8.10.
- Sealed classes: [JEP 409](https://openjdk.org/jeps/409).
- Pattern matching for `instanceof`: [JEP 394](https://openjdk.org/jeps/394). Pattern matching
  for `switch` (Java 21): [JEP 441](https://openjdk.org/jeps/441). Record patterns: [JEP 440](https://openjdk.org/jeps/440).
- Text blocks: [JEP 378](https://openjdk.org/jeps/378). Local variable type inference: [JEP 286](https://openjdk.org/jeps/286).
- Streams: [`java.util.stream` package summary](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/package-summary.html)
  and [`Collectors`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/stream/Collectors.html).
- [`Optional`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Optional.html).
- [`BigDecimal`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html),
  [`Currency`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Currency.html).
- [`ExecutorService`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ExecutorService.html),
  [`Future`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/Future.html).
  Virtual threads (Java 21): [JEP 444](https://openjdk.org/jeps/444).
- The Java tutorials on [dev.java](https://dev.java/learn/): records, sealed classes, lambda
  expressions, the stream API.

**Used in:** Demo 00 (all labs), Demo 01 (sealed `PaymentResult`, `Optional` in the catalogue, the
executor in `QuoteBatch`), and every demo after.
