# Module 03 — Data and the network, before frameworks

**Study notes** · ~2 hours

> **Goal.** See the three things every later framework hides: a relational table and the SQL
> that asks it a question, a document in a document database, and an HTTP request on the wire.
> When Spring Data JPA, Spring Data MongoDB and Spring MVC arrive, you will know what they are
> doing for you, and what they are not.

**Prerequisites:** [Module 01](../01-modern-java-for-spring/) §8 for exceptions and
try-with-resources. Docker Desktop for the database labs; `curl` for the HTTP lab.

---

## Contents

1. [Relational basics on the ShopScope schema](#1-relational-basics-on-the-shopscope-schema)
2. [The price-selection question, in SQL](#2-the-price-selection-question-in-sql)
3. [Plain JDBC, once](#3-plain-jdbc-once)
4. [What an ORM hides](#4-what-an-orm-hides)
5. [A document database](#5-a-document-database)
6. [HTTP essentials](#6-http-essentials)
7. [JSON on the wire](#7-json-on-the-wire)
8. [`curl` and `.http` files](#8-curl-and-http-files)
9. [Ports used on this track](#9-ports-used-on-this-track)
10. [Self-check](#10-self-check)
11. [References](#11-references)

---

## 1. Relational basics on the ShopScope schema

`src/main/resources/sql/schema.sql` is the smallest useful piece of ShopScope's database: two
tables.

```sql
create table if not exists product (
    sku         varchar(32)  primary key,
    name        varchar(200) not null,
    category    varchar(64)  not null,
    description text         not null default ''
);

create table if not exists price (
    id          bigint generated always as identity primary key,
    sku         varchar(32)    not null references product (sku),
    amount      numeric(12, 2) not null check (amount >= 0),
    currency    char(3)        not null,
    scope_type  varchar(16)    not null check (scope_type in ('BASE', 'COUNTRY', 'CHANNEL', 'CUSTOMER_GROUP')),
    scope_value varchar(64)
);

create index if not exists price_sku_currency on price (sku, currency);
```

The vocabulary:

- A **table** is rows of the same shape; a **column** has a type and constraints.
- A **primary key** identifies a row. `product` uses the natural key, the SKU. `price` uses a
  **surrogate** key, an `identity` column the database numbers for you, because a price has no
  natural identity of its own.
- A **foreign key** (`references product (sku)`) says every price points at a product that
  exists. The database refuses a price for `SKU-NOPE` and refuses to delete a product that still
  has prices. This is the relational model's promise: the data cannot be inconsistent.
- **`check`** constraints keep bad values out: no negative amounts, only four scope types.
  Compare `Price`'s compact constructor; the database enforces the same rules, for every client,
  including the ones not written in Java.
- **`numeric(12, 2)`** is exact decimal money, like `BigDecimal`. Never `float` or `double`.
- An **index** on `(sku, currency)` makes the lookup "all prices for this SKU in this currency"
  fast. Without it the database reads the whole table. Indexes cost space and slow down writes,
  so you add the ones your queries need, not all of them.

Reading rows is a `select` with a `where` clause:

```sql
select sku, amount, scope_type, scope_value
from price
where sku = 'SKU-KET-001' and currency = 'INR';
```

---

## 2. The price-selection question, in SQL

`PriceList.find` picks the most specific price for a shopper. The same question in SQL, for a
wholesale customer in India on the app:

```sql
select amount, scope_type, scope_value
from price
where sku = 'SKU-KET-001'
  and currency = 'INR'
  and (
        scope_type = 'BASE'
     or (scope_type = 'COUNTRY'        and scope_value = 'IN')
     or (scope_type = 'CHANNEL'        and scope_value = 'app')
     or (scope_type = 'CUSTOMER_GROUP' and scope_value = 'wholesale')
  )
order by case scope_type
           when 'CUSTOMER_GROUP' then 3
           when 'CHANNEL'        then 2
           when 'COUNTRY'        then 1
           else 0
         end desc
limit 1;
```

Read it against the Java: the `where` is `appliesTo`, the `order by case` is `precedence()`,
`limit 1` is `max`. The logic is the same; only the language differs. In Demo 7 this becomes a
JPQL query, and in Demo 9 the ranking moves into SQL for good, because the database is better at
it than a stream over every price.

---

## 3. Plain JDBC, once

JDBC is the Java API every database driver implements. `JdbcPriceLoader` is the whole of it in
one method:

```java
private static final String SELECT = """
        select sku, amount, currency, scope_type, scope_value
        from price
        where currency = ?
        order by sku
        """;

public static List<Price> load(String jdbcUrl, String user, String password, String currency) throws SQLException {
    List<Price> prices = new ArrayList<>();
    try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
         PreparedStatement statement = connection.prepareStatement(SELECT)) {
        statement.setString(1, currency);
        try (ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                prices.add(new Price(
                        rows.getString("sku"),
                        Money.of(rows.getBigDecimal("amount"), Currency.getInstance(rows.getString("currency"))),
                        scope(rows.getString("scope_type"), rows.getString("scope_value"))));
            }
        }
    }
    return prices;
}
```

Five steps, and each one is something you will later never write again:

1. **`DriverManager.getConnection(url, user, password)`** opens a network connection. The URL
   `jdbc:postgresql://localhost:5432/shopscope` names the driver, host, port and database. The
   driver jar must be on the classpath (`runtime` scope in the POM), and `DriverManager` finds it
   by itself.
2. **`prepareStatement(SELECT)`** sends the SQL once and gets back a statement with a `?`
   placeholder. **`setString(1, currency)`** binds a value to it. Never build SQL by
   concatenating strings with user input; a bound parameter cannot change the query's structure,
   which is the whole defence against SQL injection.
3. **`executeQuery()`** runs it and returns a `ResultSet`, a cursor over the rows.
4. **`rows.next()`** advances; the getters read the current row by column name. The mapping
   from columns to a `Price` is written by hand: this is the "repository layer" at its rawest.
5. **try-with-resources** closes the result set, then the statement, then the connection,
   in reverse order of opening, even when an exception is thrown. A connection that is not
   closed is a leak; a pool of them is an outage.

`SQLException` is checked. At this boundary that is reasonable: the caller may well want to
retry or report a connection failure.

---

## 4. What an ORM hides

Spring Data JPA (Demo 7) replaces everything in section 3 with an interface:

```java
interface PriceRepository extends JpaRepository<PriceEntity, Long> {
    List<PriceEntity> findBySkuAndCurrency(String sku, String currency);
}
```

Connection pooling, statement preparation, parameter binding, row mapping, closing: all
generated. That is a lot of leverage, and it is why nobody writes JDBC by hand for ordinary
work. It is also why people get surprised: when the generated SQL is slow (the N+1 problem of
Demo 8) or the transaction boundary is not where they thought (Demo 10), the fix requires
knowing what is happening underneath. Having written it once, you will read Hibernate's SQL log
and recognise it.

---

## 5. A document database

A relational customer with two addresses is two tables and a foreign key. A **document**
database stores the customer as one JSON-like document with the addresses inside it. The Lab 9
script, in `docs/lab-9-first-document.js`, inserts one into MongoDB:

```javascript
db.customers.insertOne({
  _id: "c-1",
  email: "priya@example.com",
  name: "Priya",
  customerGroup: "retail",
  addresses: [
    { label: "home",   line1: "12 MG Road",       city: "Bengaluru", country: "IN", default: true },
    { label: "office", line1: "4 Residency Road", city: "Bengaluru", country: "IN", default: false }
  ]
});
```

Reading it back is one call, and the addresses come with it: `db.customers.findOne({ _id: "c-1" })`.
There is no join, because there is no second table. Querying inside the array works:
`db.customers.find({ "addresses.city": "Bengaluru" })`. Updating one address in place works:
`updateOne` with `"addresses.$.line1"`.

There is also no schema. The script's second document has a `gstNumber` field the first one
does not, and MongoDB accepts it without comment.

**When that helps.** The data is naturally a tree that is read and written as a unit, and its
shape varies by document: a customer profile with an address book, an order with its lines, a
product with attributes that differ per category, an event log. The document is the aggregate,
one read gets all of it, and adding a field is just adding a field.

**When it hurts.** The data is shared across many aggregates and must be consistent everywhere:
a price referenced by thousands of orders, stock that must never go negative, a ledger. Joins,
foreign keys, multi-row transactions and `check` constraints are what you want there, and a
relational database gives you all of them. "No schema" also means the schema lives in your code
and your discipline, which is fine until a second team writes to the collection.

That is why ShopScope ends up with three databases (Demo 13): PostgreSQL for the catalogue,
MySQL for orders and the payment ledger, MongoDB for customers and notifications. Each one is
the right tool for its module's data, and the `infrastructure` boundary keeps the choice local.

---

## 6. HTTP essentials

A request has a **method**, a **path** (with optional query parameters), **headers** and
sometimes a **body**. A response has a **status code**, headers and usually a body.

### Methods

| Method | Meaning | Safe (no change)? | Idempotent (repeat = same result)? |
|---|---|---|---|
| `GET` | read a resource | yes | yes |
| `POST` | create, or do something | no | no |
| `PUT` | replace a resource | no | yes |
| `PATCH` | change part of a resource | no | not necessarily |
| `DELETE` | remove a resource | no | yes |

**Idempotency** is the property that matters most in a distributed system: a client that times
out can safely retry an idempotent request. `DELETE /api/products/1` twice removes it once.
`POST /api/checkout` twice places two orders, which is why Demo 10 gives checkout an idempotency
key and Demo 21 only retries where it is safe.

### Status codes

| Class | Meaning | On this track |
|---|---|---|
| `2xx` | success | `200 OK`, `201 Created` (with a `Location` header), `204 No Content` |
| `3xx` | go elsewhere | `304 Not Modified` with ETags (Demo 6) |
| `4xx` | the client's request is wrong | `400`, `401`, `403`, `404 Not Found`, `409 Conflict`, `422 Unprocessable Content`, `429` |
| `5xx` | the server failed | `500`, `503` |

The status code is the answer; the body explains it. A `404` with a body that says "no product
with SKU X" is right; a `200` with `{"error": "not found"}` is a bug that every client has to
special-case.

### Headers

`Content-Type` says what the body *is* (`application/json`); the sender sets it. `Accept` says
what the client *wants back*; the server honours it if it can. The monolith's `ShopHttpServer`
sets `Content-Type` on every response and ignores `Accept` entirely, which is one of the things
Spring MVC's content negotiation fixes in Demo 5.

---

## 7. JSON on the wire

JSON is text: objects in `{}`, arrays in `[]`, strings in double quotes, numbers, `true`,
`false`, `null`. It has no comments, no trailing commas, no dates (they are strings by
convention, ISO 8601) and no distinction between integer and decimal.

Turning Java objects into JSON and back is called **serialisation** and **deserialisation**,
and it is harder than it looks. The monolith's `Json` class is a deliberately minimal encoder
and a "parser" that only reads one flat object with a regular expression. Send it
`{"quantity": "one"}` and the quantity becomes a `NumberFormatException`; send it a nested
object and it silently reads nothing. Jackson, which Spring MVC uses from Demo 5, handles
nesting, escapes, unicode, dates, records and validation. Seeing the hand-rolled version once
is enough to never want to write another.

---

## 8. `curl` and `.http` files

`curl` is the HTTP client in every terminal. The flags you need:

```bash
curl -i https://dummyjson.com/products/1                 # -i: show the status line and headers
curl -i https://dummyjson.com/products/99999             # a 404, and what its body says
curl -X POST https://dummyjson.com/products/add \        # -X: the method
     -H "Content-Type: application/json" \               # -H: a header
     -d '{"title": "ShopScope stoneware mug", "price": 499}'   # -d: the body
curl -s localhost:8080/api/products | python3 -m json.tool      # -s: silent, pipe to a pretty-printer
```

An `.http` file is the same requests in a text file that IntelliJ and the VS Code REST Client
can run with one click, with `###` between requests:

```http
### A resource that does not exist: the status code is the answer, not the body.
GET https://dummyjson.com/products/99999

### POST: send JSON, get 201 and the created resource back.
POST https://dummyjson.com/products/add
Content-Type: application/json

{"title": "ShopScope stoneware mug", "price": 499}
```

Lab 10 of Demo 0 uses `http/lab-10-http-essentials.http` against a public API; from Demo 1 on,
every starter has an `http/` folder with the requests for its own API, and the collection grows
with the course.

---

## 9. Ports used on this track

From the project conventions:

| Port | What |
|---|---|
| 5432 | PostgreSQL |
| 3306 | MySQL |
| 27017 | MongoDB |
| 6379 | Redis |
| 8080 | the monolith (Demo 1), later the API gateway |
| 8081 to 8087 | the services, once extracted |

The database labs start each engine with one `docker run`; the commands are at the top of
`schema.sql` and `lab-9-first-document.js`.

---

## 10. Self-check

1. What does the foreign key on `price.sku` prevent, and what happens if you delete a product
   that has prices?
2. Why is `amount` a `numeric(12, 2)` and not a `double`?
3. Write the SQL that lists every INR price for `SKU-TEA-001` with the most specific scope
   first.
4. In `JdbcPriceLoader`, why is the currency bound with `setString` rather than concatenated
   into the query?
5. In what order does try-with-resources close the connection, statement and result set, and
   why does the order matter?
6. Give one kind of data that fits a document database better than tables, and one that does
   not.
7. Which of `GET`, `POST`, `PUT`, `DELETE` can a client safely retry after a timeout, and why?
8. What is the difference between `Content-Type` and `Accept`?
9. What does `curl -i` show that `curl` alone does not?

---

## 11. References

- JDBC: [`java.sql` package](https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/java/sql/package-summary.html),
  the [JDBC basics tutorial](https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html),
  try-with-resources: [JLS §14.20.3](https://docs.oracle.com/javase/specs/jls/se17/html/jls-14.html#jls-14.20.3).
- PostgreSQL: [data types](https://www.postgresql.org/docs/current/datatype.html),
  [constraints](https://www.postgresql.org/docs/current/ddl-constraints.html),
  [indexes](https://www.postgresql.org/docs/current/indexes.html).
- MongoDB: [documents](https://www.mongodb.com/docs/manual/core/document/),
  [CRUD operations](https://www.mongodb.com/docs/manual/crud/),
  [data modelling](https://www.mongodb.com/docs/manual/data-modeling/).
- HTTP: [RFC 9110, HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110) (methods, status codes,
  idempotency), [JSON, RFC 8259](https://www.rfc-editor.org/rfc/rfc8259).
- [`curl` manual](https://curl.se/docs/manpage.html).

**Used in:** Demo 00 (Labs 8, 9 and 10), Demo 01 (Lab 7, the hand-rolled HTTP layer), Demo 05
(REST), Demo 07 (JPA), Demo 13 (MySQL and MongoDB).
