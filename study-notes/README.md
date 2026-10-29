# ShopScope Commerce · Study notes

**The theory track of the Java course.** The demo guides in
[`../spring-boot-and-microservices/`](../spring-boot-and-microservices/) are hands-on: a starter,
labs with minutes, a test that goes green. These notes are the other half: the concepts behind
each demo, written to be read before the session, revisited after it, and used on their own as a
reference. Every note names the demos that use it, and every demo guide opens with a
**Pre-reading** section that names its notes.

| | |
|---|---|
| 📚 **You are here**, `study-notes/` | Module-wise notes. Read, understand, self-check |
| 🛠 [`../spring-boot-and-microservices/`](../spring-boot-and-microservices/) | The demos. Build ShopScope, one step at a time |
| 📋 [`../COURSE-OUTLINE.md`](../COURSE-OUTLINE.md) | The whole track: every demo and lab |

**References are from official documentation only**: the Java documentation and JEPs on
openjdk.org, dev.java, maven.apache.org, junit.org, assertj.github.io, docs.spring.io, and the
database vendors' own manuals. No blogs, no videos. Everything cited is something its
maintainers keep correct as the software moves.

Code in the notes compiles on **Java 17**; features from Java 21 and later appear only in
clearly marked asides.

---

## The modules

### Part 0 — Before Spring

| # | Module | Time | Status |
|---|---|---|---|
| 01 | **[Modern Java for Spring](./01-modern-java-for-spring/)** | 3–4h | ✅ complete |
| 02 | **[Build and test: Maven, JUnit and AssertJ](./02-build-and-test-maven-junit-assertj/)** | 2h | ✅ complete |
| 03 | **[Data and the network, before frameworks](./03-data-and-the-network-before-frameworks/)** | 2h | ✅ complete |
| 04 | **[The monolith and its problems](./04-the-monolith-and-its-problems/)** | 2–3h | ✅ complete |

Records, sealed types, pattern matching, streams, `Optional`, exceptions and executors, on the
ShopScope pricing domain. Then what `./mvnw verify` does and how a test is written. Then SQL,
JDBC, a document and an HTTP request with nothing in between. Then the plain-Java monolith read
as a reviewer would read it, and the problem register that the rest of the track empties.

### Part 1 — Spring takes over the monolith

| # | Module | Time | Status |
|---|---|---|---|
| 05 | Inversion of control and the Spring container | | planned |
| 06 | Configuration, events and AOP | | planned |
| 07 | Spring Boot and auto-configuration | | planned |

### Part 2 — A modular monolith: REST and data

| # | Module | Time | Status |
|---|---|---|---|
| 08 | REST with Spring MVC | | planned |
| 09 | Spring Data JPA | | planned |
| 10 | Transactions and concurrency | | planned |
| 11 | Testing Spring applications | | planned |

### Part 3 — From monolith to microservices

| # | Module | Time | Status |
|---|---|---|---|
| 12 | Microservices and synchronous communication | | planned |
| 13 | Spring Cloud: config, discovery, gateway | | planned |

### Parts 4 to 6 — Security, resilience, integrations

| # | Module | Time | Status |
|---|---|---|---|
| 14 | Spring Security and JWT | | planned |
| 15 | Resilience and observability | | planned |
| 16 | Integrations: payments, e-mail, batch | | planned |

---

## Mapping to the ShopScope demos

The demo track builds one system, ShopScope Commerce, from a plain-Java monolith to a set of
services. Each demo guide opens with a **Pre-reading** section naming the modules below; this is
the same map in both directions.

**Demo → study notes**

| Demo | Read |
|---|---|
| [Demo 00 · The Java you will write ShopScope in](../spring-boot-and-microservices/00-the-java-you-will-write-shopscope-in/) | [01](./01-modern-java-for-spring/) in full · [02](./02-build-and-test-maven-junit-assertj/) in full · [03](./03-data-and-the-network-before-frameworks/) in full |
| [Demo 01 · ShopScope as a plain-Java monolith](../spring-boot-and-microservices/01-shopscope-as-a-plain-java-monolith/) | [04](./04-the-monolith-and-its-problems/) in full · [01](./01-modern-java-for-spring/) §3 sealed types and §7 `Optional`, as a refresher |

**Study notes → demos**

| Module | Used in |
|---|---|
| [01 Modern Java for Spring](./01-modern-java-for-spring/) | Demo 00 (all labs), Demo 01, and every demo after |
| [02 Build and test](./02-build-and-test-maven-junit-assertj/) | Demo 00 (Labs 5 and 6, every Verify step), Demo 01 (Labs 3 and 6), and every demo after |
| [03 Data and the network, before frameworks](./03-data-and-the-network-before-frameworks/) | Demo 00 (Labs 8 to 10), Demo 01 (Lab 7), Demos 05, 07 and 13 |
| [04 The monolith and its problems](./04-the-monolith-and-its-problems/) | Demo 01 (every lab); re-read at the end of Parts 1, 2 and 3 |

---

## How to use these notes

1. **Before a demo**, read the modules its guide names, at the depth it names ("in full", or a
   section). Thirty minutes of reading makes a two-hour session feel slow in the good way.
2. **During a demo**, keep the note open for the lab you are on; the guide's Concept blocks are
   summaries of these sections.
3. **After a demo**, do the Self-check questions without looking. If one is hard, that is the
   section to re-read.
4. **Later**, use the Contents list of each module as the index it is meant to be.
