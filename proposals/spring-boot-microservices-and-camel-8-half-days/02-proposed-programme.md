# Microservices with Spring Boot and Apache Camel: proposed programme

**Format:** 8 half-day sessions (about 4 hours each), hands-on.
**Prerequisite:** good knowledge of Java and the Spring Framework.
**Project:** ShopScope Commerce, an online store built across the programme. It starts as a plain-Java
monolith (seven modules in one process, no framework) with a written register of its problems, becomes a
Spring Boot application, is split into services with a registry, central configuration, a gateway and
resilience, and finally gets Camel routes between the services and the outside world.

This document is the programme content, module by module, in the order of the requested outline. Every
requested topic appears in exactly one module. The half-day schedule with timings is in
*03-eight-half-day-session-plan.md*.

Each module lists: what the participant can do afterwards, the requested topics it covers, what gets built,
the labs with minutes, and what the module adds beyond the request.

## How the training is hands-on

**More than half of every half-day is spent at the keyboard.** The programme has 56 labs totalling about
17 hours of typing across the 8 half-days; the rest is short concept blocks (never more than 20 minutes
before the next lab), breaks and wrap-ups. Nothing is shown on a slide that participants do not then build.

**One system, built by the participants.** ShopScope Commerce is not a sample downloaded on day one. The
pricing engine is written as plain Java in the first hour and wired into Spring by hand. The catalogue
service is generated with Spring Initializr in lab 1.8. By the end of half-day 6 the participants' own code
is a running system of five services behind a gateway, and in half-days 7 and 8 the Camel routes connect
those same services. Everything demonstrated runs on code the participants typed.

**Every lab has the same shape**, so participants always know where they are:

| Step | What happens |
|---|---|
| Problem | A concrete failure or need in the running system (a race that oversells, a call that hangs, a feed that will not parse). |
| Concept | The idea that solves it, in a few minutes, with the one diagram or table that matters. |
| Steps | Numbered, typed by the participant. The starter carries `TODO(lab-x.y)` markers in the same order. |
| Verify | A request in the `.http` collection or Postman, a test that goes green, or a log line that must appear. |
| Watch out | The two or three mistakes everyone makes, and how to recognise them. |
| Challenge | An extension for those who finish early, with the answer in the finished code. |
| In the real world | How the same thing looks in a production code base. |

**Starters keep the group together.** Every session starts from a ready-made starter project: the previous
session finished, plus the markers for this session's labs. A participant who runs over on one lab, or misses
a half-day, continues from the finished state and loses nothing. The trainer never waits for the slowest
machine, and nobody copies code from a screen.

**Verification is part of the lab, not a demo.** Every lab ends with something the participant runs: a
request that returns the right status code, a test that passes, a circuit breaker seen opening in Actuator, a
Camel route whose log shows the split messages. A lab is finished when the verify step passes on the
participant's machine, not when the trainer moves on.

**Breaking things is a lab too.** Several labs start by making the system fail on purpose: two browser tabs
updating one cart, latency injected into the inventory service, a feed with a malformed record. Participants
see the failure before they fix it, so the fix is understood rather than memorised.

**What participants have running at the end of each half-day** is listed in *03-eight-half-day-session-plan.md*.

---

## Module 1 · Spring Framework and Spring Boot (half-day 1)

**Afterwards, participants can** explain how the container wires an application, choose the right injection
style, convert a plain Spring project to Spring Boot, read the auto-configuration report, and build with Maven.

**Requested topics covered:** Spring introduction · dependency injection · `@Component` · auto-wiring ·
`@Autowired` · auto-wiring collections · `@Configuration` · why Spring Boot · set-up · opinionated defaults ·
Spring Initializr · customising Boot configuration · Maven build · `@SpringBootApplication` ·
`@EnableAutoConfiguration` · `@ComponentScan` · starters · the web starter · the data starter.

**What gets built.** The session opens with ShopScope as a plain-Java monolith: participants run it, break
it and write its problem register. Its hand-written wiring becomes a Spring application, then a Spring
Boot application with a starter module of its own.

| Lab | Minutes |
|---|---|
| 1.1 The monolith: run it, find the hidden dependency and the overselling bug, write the problem register | 15 |
| 1.2 The container: `@Configuration`, `@Bean`, `@Component` and scanning | 20 |
| 1.3 Injection: constructor vs field `@Autowired`, `@Primary` and `@Qualifier`, a `List` and a `Map` of beans (the discount-rule chain) | 25 |
| 1.4 Scopes and the stateful-singleton bug | 10 |
| 1.5 Add Boot: the parent, starters, `@SpringBootApplication` taken apart into `@EnableAutoConfiguration` and `@ComponentScan` | 20 |
| 1.6 Auto-configuration: delete what Boot now provides, read the conditions report, override a bean | 25 |
| 1.7 Externalised configuration: `application.yml`, profiles, `@ConfigurationProperties` | 15 |
| 1.8 Spring Initializr and the Boot parent: the web and data starters, and a multi-module Maven build | 15 |

**Beyond the request.** The bean lifecycle is traced end to end, and participants see auto-configuration as a
diff on their own code rather than as a list of annotations. Oracle is discussed where the data starter is
introduced; see *Databases* at the end.

## Module 2 · REST APIs, JPA and Actuator (half-days 2 and 3)

**Afterwards, participants can** design and build a REST API with consistent status codes and one error
contract, persist it with Spring Data JPA, keep transactions correct under concurrent requests, call other
services from Java, and expose and customise Actuator endpoints.

**Requested topics covered:** Actuator introduction, endpoint configuration and customisation · JPA and Spring
Data · Spring MVC project · common annotations · REST introduction · `@RestController` · `@RequestBody` ·
`@ResponseBody` · GET, POST, PUT and DELETE · testing with Postman · REST with JPA · common JPA annotations ·
a project with REST and JPA · REST clients · `RestTemplate` · content-type and accept headers · web services overview.

**What gets built.** The monolith's hand-rolled HTTP layer becomes a REST API for stores and products, the
catalogue moves into a database with versioned migrations, and Actuator arrives. The order module gets
carts, checkout and transactions (closing the overselling bug from half-day 1), and the modules start
calling each other over HTTP in preparation for the split.

| Lab | Minutes |
|---|---|
| 2.1 Spring MVC under REST: `DispatcherServlet`, handler mapping, message converters, `@RequestBody` and `@ResponseBody` | 15 |
| 2.2 Resource design: URIs, methods, idempotency, status codes | 10 |
| 2.3 Product CRUD with `@RestController`, `ResponseEntity`, `Location`, `204` and `404` | 25 |
| 2.4 Validation, and one error contract with `ProblemDetail` | 20 |
| 2.5 Testing with Postman and `.http` files: a request collection that grows with the course | 10 |
| 2.6 JPA, Hibernate and Spring Data: the first `@Entity`, `@Id`, `@Column`, `@Enumerated`, `@Embedded` | 20 |
| 2.7 Flyway migrations and seed data | 15 |
| 2.8 `JpaRepository`, derived queries, `@Query`, paging and projections | 25 |
| 2.9 Relationships: `@ManyToOne`, `@OneToMany`, `@ManyToMany`, the owning side; the N+1 problem caught and fixed | 25 |
| 2.10 Actuator: enabling, exposing and securing endpoints, health groups, `info`, a custom endpoint | 25 |
| 2.11 The order service: cart and order aggregates, `@Transactional`, rollback rules, propagation | 25 |
| 2.12 Two tabs, one cart: optimistic locking with `@Version` | 20 |
| 2.13 REST clients: `RestClient`, content-type and accept headers, timeouts, error mapping; `RestTemplate` for reading existing code | 25 |
| 2.14 Declarative HTTP interfaces with `@HttpExchange` | 15 |

**Beyond the request.** One error contract shared by every service, migrations instead of `ddl-auto`, the
N+1 problem fixed rather than mentioned, and locking demonstrated on a real race. The clients taught are the
ones Spring recommends today; `RestTemplate` is covered so the team can read and migrate existing code.

## Module 3 · Microservices architecture and Spring Cloud discovery (half-day 4)

**Afterwards, participants can** argue for and against splitting a system, draw service boundaries and
database scope, run services that find each other through a registry, and call them through load-balanced
and declarative clients.

**Requested topics covered:** SOA and microservices architecture · how microservices implement web-service
concepts · protocols and standards · overall topology · service granularity · database scope · service
deployment · advantages and disadvantages · primary benefits · Spring Cloud · Eureka server and client ·
`@EnableEurekaServer` · `@EnableDiscoveryClient` · Feign proxy · service discovery with Feign ·
fault tolerance (introduced) · configurable load-balancing rules.

**What gets built.** The modular monolith is split: the catalogue is extracted first, then the rest. A
discovery server, two instances of the catalogue service behind client-side load balancing, and an OpenFeign client next to the HTTP interface from Module 2.
A checkout that spans the order, catalogue, pricing and inventory services shows what happens when one step fails.

| Lab | Minutes |
|---|---|
| 3.1 Architecture, not code: monolith vs modular monolith vs microservices, bounded contexts, database per service, topology, granularity and deployment of ShopScope | 25 |
| 3.2 Checkout across four services: the order of the calls, what happens when step three fails, a manual compensation | 25 |
| 3.3 Correlation ids across calls | 10 |
| 3.4 The discovery server with `@EnableEurekaServer`; clients register with `@EnableDiscoveryClient` | 20 |
| 3.5 Client-side load balancing: a `@LoadBalanced` `RestClient` across two instances; load-balancing rules | 20 |
| 3.6 OpenFeign: the same call as a Feign proxy, compared with the HTTP interface | 20 |
| 3.7 Health, heartbeats and self-preservation: what "down" means to a registry | 15 |

**Beyond the request.** The architecture session is applied to the participants' own running system, and
the failure of a multi-service call is seen before the resilience patterns of Module 4 are taught.

## Module 4 · Resilience, central configuration and the API gateway (half-days 5 and 6)

**Afterwards, participants can** make a call chain survive a slow or failing service, keep configuration
for every service in one Git repository with profiles and refresh it at runtime, and put one gateway in front
of the system with routes, predicates and filters.

**Requested topics covered:** circuit breaker pattern · Resilience4j · Spring Cloud Config · profiles ·
Config with a Git repository · API gateway · Spring Cloud Gateway · filters · a logging filter · declarative
configuration of predicates, methods and URLs.

**What gets built.** Failures are injected into the inventory service and contained with timeouts, retries
and circuit breakers. A config server serves every service from Git. A gateway becomes the single entry
point with a logging filter and rate limiting.

| Lab | Minutes |
|---|---|
| 4.1 Break it on purpose: latency and failures injected into the inventory service | 10 |
| 4.2 Timeouts everywhere, and why they come first | 15 |
| 4.3 Retries with backoff and jitter; idempotency keys so a retried POST is safe | 20 |
| 4.4 Circuit breaker and fallbacks with Resilience4j through Spring Cloud CircuitBreaker | 25 |
| 4.5 Bulkheads and rate limiters; graceful degradation in checkout | 15 |
| 4.6 The config server backed by a Git repository: one file per service per profile | 25 |
| 4.7 The client side: `spring.config.import`, profiles, fail-fast and retry | 15 |
| 4.8 Encrypted secrets, and refresh at runtime with `@RefreshScope` | 20 |
| 4.9 Spring Cloud Gateway: routes, predicates on path, method and header, `lb://` URIs | 25 |
| 4.10 Filters: path rewriting, request ids, a custom global logging filter | 25 |
| 4.11 CORS once at the edge, and rate limiting with Redis | 15 |

**Beyond the request.** Timeouts and retries before circuit breakers, because that is the order they fail
in; secrets kept out of Git; rate limiting at the edge.

## Module 5 · Enterprise integration with Apache Camel (half-days 7 and 8)

**Afterwards, participants can** build Camel routes inside Spring Boot services, move messages between
services over ActiveMQ and Kafka, transform JSON and XML into Java beans, call REST APIs from routes, and apply
the enterprise integration patterns to real flows.

**Requested topics covered:** getting started with Camel · the first route · Spring beans for transformation ·
processors · a file route · ActiveMQ as a container · sender and receiver routes between two services ·
Camel terminology and architecture · JSON and XML unmarshalling · transformation after unmarshalling · Kafka
with Camel · HTTP calls from a route · choice and Simple · reusable endpoints · complex deciders · pipeline
and multicast · content-based routing · splitter · aggregation · routing slip · dynamic routing · best
practices: dynamic configuration, stand-alone and logging.

**What gets built.** Camel routes on ShopScope: the supplier feed as a file route into the catalogue; order
events between the order and notification services over ActiveMQ, then Kafka; HTTP calls to the catalogue
from a route; and the enterprise integration patterns applied to the feed and the checkout.

| Lab | Minutes |
|---|---|
| 5.1 Camel in a Spring Boot service: the first route, `RouteBuilder`, endpoints, the Camel context; terminology and architecture | 20 |
| 5.2 Spring beans and processors for transformation in a route | 20 |
| 5.3 A file route for the supplier feed; unmarshalling JSON and XML into Java beans, and transforming after | 30 |
| 5.4 ActiveMQ in a container; a sender route in the order service, a receiver route in the notification service | 30 |
| 5.5 The same flow over Kafka with Camel | 20 |
| 5.6 Calling the catalogue's REST API from a route | 15 |
| 5.7 Choice and the Simple language; reusable endpoints; complex deciders | 25 |
| 5.8 Pipeline and multicast | 15 |
| 5.9 Content-based routing and the splitter on the supplier feed | 25 |
| 5.10 Aggregation: reassembling the split feed, completion rules | 20 |
| 5.11 Routing slip and dynamic routing on the checkout flow | 25 |
| 5.12 Best practices: dynamic configuration, stand-alone Camel vs inside Spring Boot, logging and error handling; testing a route | 25 |

**Beyond the request.** Every route runs on the system built in Modules 1 to 4, so routing decisions use real
data and real services, and the choice between Camel and the plain Spring alternatives (Spring Batch, Spring
Kafka, HTTP interfaces) is made with both in view.

---

## Databases

The standard labs run PostgreSQL for the catalogue and MySQL for the order service, both in containers, with
the same JPA code. If Oracle is the production database, the catalogue labs run on Oracle Database Free in a
container instead; the JPA code does not change, and the differences (sequences, dialect, Flyway scripts) are
shown once. The order service's MySQL stays, so the team sees two engines behind the same repository boundary.

## Environment

JDK 17 or newer (the code compiles for 17 and runs unchanged on 21 and 25), IntelliJ IDEA or VS Code, Maven (the wrapper is included), Docker Desktop for the databases, ActiveMQ
and Kafka, and Postman or the IDE's HTTP client. Every half-day starts from a ready-made starter project, so a
participant who missed a lab continues from the finished state.

## What participants take away

Every session's starter and finished code, a step-by-step guide per session with the labs, study notes for
every concept, the growing Postman and `.http` collection, and interview-style questions per topic.

## Optional extensions

Not requested, available as further half-days or full days on the same system: Spring Security with JWT and
Keycloak; testing with Testcontainers and contract tests; observability with Micrometer, Prometheus, Grafana
and OpenTelemetry; Docker and Docker Compose; Kafka in depth (reliable consumers, outbox, sagas).
