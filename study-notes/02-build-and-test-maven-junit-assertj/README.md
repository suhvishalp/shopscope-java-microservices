# Module 02 — Build and test: Maven, JUnit and AssertJ

**Study notes** · ~2 hours

> **Goal.** Understand what happens when you type `./mvnw verify`: the lifecycle, where
> dependencies come from and which classpath they land on, what the plugins in ShopScope's POM
> do, and how JUnit and AssertJ turn a lab into a red test that you make green.

**Prerequisites:** [Module 01](../01-modern-java-for-spring/) for the code the tests exercise.
A terminal and JDK 17 or newer.

---

## Contents

1. [What Maven is for](#1-what-maven-is-for)
2. [The lifecycle](#2-the-lifecycle)
3. [Coordinates and dependencies](#3-coordinates-and-dependencies)
4. [Scopes: which classpath](#4-scopes-which-classpath)
5. [`dependencyManagement` and BOMs](#5-dependencymanagement-and-boms)
6. [The plugins in ShopScope's POM](#6-the-plugins-in-shopscopes-pom)
7. [The wrapper: why `./mvnw`](#7-the-wrapper-why-mvnw)
8. [Reading the dependency tree](#8-reading-the-dependency-tree)
9. [JUnit: the shape of a test](#9-junit-the-shape-of-a-test)
10. [AssertJ: assertions that read](#10-assertj-assertions-that-read)
11. [What makes a good unit test](#11-what-makes-a-good-unit-test)
12. [The red-green loop in the guides](#12-the-red-green-loop-in-the-guides)
13. [Self-check](#13-self-check)
14. [References](#14-references)

---

## 1. What Maven is for

Maven builds the project: compiles the sources, runs the tests, packages a jar. It also
resolves dependencies: you name a library and a version, Maven downloads it and everything it
needs from Maven Central into `~/.m2/repository`, once, and puts it on the right classpath.

Everything is declared in one file, `pom.xml` (the Project Object Model). Maven is
convention over configuration: sources in `src/main/java`, tests in `src/test/java`, resources
in `src/main/resources`, output in `target/`. Follow the layout and the POM stays short.

---

## 2. The lifecycle

A Maven build is a sequence of **phases**. Asking for a phase runs every phase before it:

| Phase | What happens | Plugin |
|---|---|---|
| `validate` | the POM is checked | |
| `compile` | `src/main/java` to `target/classes` | compiler |
| `test` | `src/test/java` compiled and run | compiler, surefire |
| `package` | `target/shopscope-monolith-0.1.0.jar` | jar |
| `verify` | integration checks (none yet; Demo 11 adds some) | |
| `install` | the jar copied into `~/.m2` for other local projects | |

So `./mvnw test` compiles then tests; `./mvnw package` compiles, tests and packages;
`./mvnw verify` is what the guides use, because it is the phase CI will use. `./mvnw clean`
deletes `target/` and is a separate lifecycle; `./mvnw clean verify` is "from scratch".

Add `-q` for quiet output and `-B` (batch) in scripts. `-DskipTests` packages without running
tests, which is for emergencies, not habits.

---

## 3. Coordinates and dependencies

Every artifact has three coordinates: `groupId` (who), `artifactId` (what), `version`.
ShopScope's own are at the top of the POM:

```xml
<groupId>com.shopscope</groupId>
<artifactId>shopscope-monolith</artifactId>
<version>0.1.0</version>
```

A dependency is the same three, inside `<dependencies>`:

```xml
<dependency>
  <groupId>org.assertj</groupId>
  <artifactId>assertj-core</artifactId>
  <version>${assertj.version}</version>
  <scope>test</scope>
</dependency>
```

`${assertj.version}` is a property defined once in `<properties>`, so every version lives in
one place and a bump is one line. From Demo 4, Spring Boot's parent POM manages most versions
for us; until then we do it by hand, which is the point of seeing it once.

A dependency's own dependencies (its **transitive** dependencies) come along automatically.
That is why declaring `junit-jupiter` is enough to get the engine, the API and the params
module.

---

## 4. Scopes: which classpath

Scope says when a dependency is present:

| Scope | Compile `src/main`? | Run `src/main`? | Compile and run tests? | In the jar's dependencies? |
|---|---|---|---|---|
| `compile` (default) | yes | yes | yes | yes |
| `runtime` | no | yes | yes | yes |
| `test` | no | no | yes | no |
| `provided` | yes | no (the container supplies it) | yes | no |

In ShopScope's POM the PostgreSQL driver is `runtime`: nothing in `src/main/java` imports a
PostgreSQL class (`JdbcPriceLoader` uses only `java.sql`), but at run time `DriverManager`
needs the driver on the classpath. JUnit and AssertJ are `test`: they are never shipped.

If a test fails to compile with "cannot find symbol" for a class that is clearly in a library,
check the scope first.

---

## 5. `dependencyManagement` and BOMs

`<dependencyManagement>` declares versions without adding dependencies. A dependency listed
there with no scope is not on any classpath; it only says "if anyone depends on this, use this
version". That is how a **BOM** (bill of materials) works: a POM that lists consistent versions
for a whole family of artifacts.

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.junit</groupId>
      <artifactId>junit-bom</artifactId>
      <version>${junit.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

After this import, `junit-jupiter` and `junit-platform-launcher` are declared below **without a
version**, and both resolve to 6.1.3. Mixing JUnit artifacts at different versions is a classic
"tests silently do not run" bug; the BOM makes it impossible.

Spring Boot's parent POM (Demo 4) is a very large `dependencyManagement` block: hundreds of
libraries at versions tested together. That is most of what "Spring Boot manages versions"
means.

---

## 6. The plugins in ShopScope's POM

Phases do nothing by themselves; **plugins** bound to phases do the work. The POM pins four:

**`maven-compiler-plugin`.** Compiles with `javac`. `<maven.compiler.release>17</maven.compiler.release>`
in `<properties>` passes `--release 17`, which checks the code against the Java 17 API even
when you compile with JDK 25. That is what makes "runs on 17, 21 and 25" a guarantee rather
than a hope. `-Xlint:all` turns on every compiler warning; read them.

**`maven-surefire-plugin`.** Runs the tests in the `test` phase and writes reports to
`target/surefire-reports/`. ShopScope's configuration points `java.util.logging` at a quieter
config for tests, so the monolith's log lines do not drown the results.

**`maven-jar-plugin`.** Builds the jar in `package`. The `<manifest><mainClass>` entry makes
`java -jar target/shopscope-monolith-0.1.0.jar` work without naming the class.

**`exec-maven-plugin`.** `./mvnw -q exec:java` runs the main class on the project's classpath,
which is handy for a program with a runtime dependency (the JDBC driver) that `java -jar` would
not find. `-Dexec.mainClass=...` overrides the class, `-Dexec.args=--demo` passes arguments.

A plugin's `<version>` is pinned on purpose. An unpinned plugin means two developers can build
the same commit with different tools.

---

## 7. The wrapper: why `./mvnw`

`mvnw` (and `mvnw.cmd` on Windows) is a small script committed with the project. On first run it
downloads the Maven version named in `.mvn/wrapper/maven-wrapper.properties` into `~/.m2` and
uses it from then on. Everyone building the project, including CI, gets the same Maven without
installing anything. Always use `./mvnw`, never a global `mvn`, so "works on my machine" has one
less cause.

The wrapper does not choose the JDK. `./mvnw -v` prints which Java it found; set `JAVA_HOME` if
it is not the one you expect.

---

## 8. Reading the dependency tree

```
./mvnw dependency:tree
```

prints every dependency, transitive ones indented under what pulled them in, with scope. Use it
to answer "why is this library here?" and "which version won?". When two paths bring different
versions of the same artifact, Maven picks the **nearest** one in the tree, which is sometimes
not the newest; a BOM or an explicit version in `dependencyManagement` is the fix.

---

## 9. JUnit: the shape of a test

JUnit Jupiter (JUnit 6 on this track; the annotations are the same as JUnit 5) runs methods
marked `@Test` in classes under `src/test/java`:

```java
class CatalogServiceTest {

    private CatalogService catalog;

    @BeforeEach
    void seed() {
        CatalogRepository repository = new CatalogRepository();
        SeedData.products(repository);
        catalog = new CatalogService(repository);
    }

    @Test
    void getThrowsANamedExceptionForAnUnknownSku() {
        assertThatThrownBy(() -> catalog.get("SKU-NOPE"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("No product with SKU SKU-NOPE");
    }
}
```

- `@BeforeEach` runs before every test, so each test gets a fresh catalogue. Tests must not
  depend on each other or on their order.
- Test classes and methods can be package-private; no `public` needed.
- `@Disabled("reason")` skips a test or a whole class and shows the reason in the report.

**Parameterized tests** run one method over a table of inputs. `MoneyTest` does this for
rounding:

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

Each row is a test with its own name in the report. `@CsvSource` converts the strings to the
parameter types (`int percent`). Other sources exist (`@ValueSource`, `@MethodSource`,
`@EnumSource`); `@CsvSource` covers most tables.

---

## 10. AssertJ: assertions that read

AssertJ gives one entry point, `assertThat(actual)`, and a fluent chain whose methods depend on
the type of `actual`. The editor completes them, and the failure messages say what was expected
and what was found.

```java
assertThat(quote.lines()).extracting(PricedLine::unitPrice)
        .containsExactly(Money.of("349.00", "INR"), Money.of("2299.00", "INR"));

assertThat(catalog.find(SeedData.TEA))
        .hasValueSatisfying(product -> assertThat(product.category()).isEqualTo("tea"));

assertThat(catalog.find("SKU-NOPE")).isEmpty();

assertThat(notifications.sent()).singleElement()
        .satisfies(email -> assertThat(email.to()).isEqualTo("priya@example.com"));

assertThatThrownBy(() -> prices.priceOrThrow(KETTLE, PriceContext.of("USD", "US")))
        .isInstanceOf(NoPriceException.class)
        .hasMessageContaining("USD/US");
```

The ones you will use most:

| On | Methods |
|---|---|
| any object | `isEqualTo`, `isNotNull`, `isInstanceOf`, `hasToString`, `extracting("field")` |
| records | `isEqualTo` works out of the box because records implement `equals` |
| collections | `hasSize`, `containsExactly`, `containsOnly`, `extracting(fn)`, `singleElement`, `isEmpty` |
| maps | `containsOnlyKeys`, `containsOnly(entry(k, v))` |
| `Optional` | `isPresent`, `isEmpty`, `hasValue`, `hasValueSatisfying`, `map(fn)` |
| exceptions | `assertThatThrownBy(() -> ...)` then `isInstanceOf`, `hasMessage`, `hasMessageContaining` |

`as("stock after a declined payment")` before the assertion names it in the failure message;
`ProblemRegisterTest` uses it so the failure reads like the bug.

---

## 11. What makes a good unit test

- **One behaviour per test, named as a sentence.** `declinedPaymentPlacesNoOrder`, not `test2`.
  The method name is the specification; the body is the evidence.
- **Arrange, act, assert.** Build the inputs, call one thing, check the result. If the
  "act" is three calls, you are testing a workflow, which is fine, but say so in the name.
- **No shared mutable state between tests.** `@BeforeEach` builds fresh objects.
- **Deterministic.** `OrderServiceTest` injects `Clock.fixed(...)` so `placedAt` is known. A
  test that depends on the wall clock, a random id or thread timing is a test you will delete
  later.
- **Assert on values, not on how they were made.** The test asks what the quote total is, not
  which method `PricingService` called first.
- **Fast.** The whole Demo 0 suite runs in well under a second. A slow unit test gets skipped by
  people, then by CI.

---

## 12. The red-green loop in the guides

Every starter ships with its tests already written and `@Disabled("enable in Lab N")`, so
`./mvnw verify` is green on a fresh checkout. Each lab ends the same way: remove the
`@Disabled`, run the test, watch it fail (red), do the lab, run it again (green). The test is
the lab's "Verify" step and its definition of done.

This has two effects. You never wonder whether you are finished, because the test says so. And
you read a test before you write the code it tests, which is the habit that makes test-driven
work feel natural by Demo 11.

The one exception is `ProblemRegisterTest` in Demo 1: a test that states the behaviour the
monolith *should* have, fails against the monolith, and stays `@Disabled` with the number of
the demo that will make it pass. The register is executable.

---

## 13. Self-check

1. Which phases run when you type `./mvnw package`, in order?
2. Why is the PostgreSQL driver `runtime` scope and AssertJ `test` scope? What goes wrong if you
   swap them?
3. What does importing `junit-bom` in `dependencyManagement` let you leave out of the two JUnit
   dependencies, and why does that matter?
4. What does `<maven.compiler.release>17</maven.compiler.release>` guarantee that
   `<source>17</source><target>17</target>` does not?
5. Why does the repository contain `mvnw` and `.mvn/wrapper/` rather than asking you to install
   Maven?
6. Write a `@ParameterizedTest` with a `@CsvSource` of three rows for `Money.times`.
7. What is wrong with a test whose `@BeforeEach` is empty and whose fields are initialised
   once as statics?
8. Why does every starter's test suite start green even though the labs are not done?

---

## 14. References

- [Introduction to the build lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html),
  [dependency mechanism](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)
  (scopes, `dependencyManagement`, BOM import), [POM reference](https://maven.apache.org/pom.html).
- [Maven Wrapper](https://maven.apache.org/wrapper/), [Compiler plugin](https://maven.apache.org/plugins/maven-compiler-plugin/),
  [Surefire plugin](https://maven.apache.org/surefire/maven-surefire-plugin/), [Jar plugin](https://maven.apache.org/plugins/maven-jar-plugin/).
- [JUnit user guide](https://junit.org/junit5/docs/current/user-guide/): writing tests, parameterized tests.
- [AssertJ core documentation](https://assertj.github.io/doc/).

**Used in:** Demo 00 (Labs 5 and 6, and the Verify step of every lab), Demo 01 (Labs 3 and 6),
and every demo after.
