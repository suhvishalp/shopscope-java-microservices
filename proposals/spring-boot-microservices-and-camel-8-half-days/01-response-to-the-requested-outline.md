# Microservices with Spring Boot: our programme against the requested outline

**Requested:** "Microservices with Spring Boot and Apache Camel", 8 half-days, for developers with good Java and Spring knowledge.
**Proposed:** the ShopScope Commerce programme: a 5-day core plus one-day add-ons, chosen to match the request
(see the TOC workbook, *Build your programme* sheet).

This note does three things: shows where every requested topic lands in our programme, explains what we add
and why, and lists the questions we need answered to size the final programme.

## 1. In short

- **Every requested Spring Boot and Spring Cloud topic is covered**, most of them at greater depth, in a
  programme where the topics build on each other instead of being visited one by one.
- **One system, built end to end.** Participants start from ShopScope Commerce as a plain-Java monolith,
  write down what is wrong with it, and migrate it step by step into a system with a storefront, seven
  services, a gateway and real payment and e-mail providers. Every session starts from the previous
  session's finished code, so a topic is never learned on a throwaway example, and every pattern arrives as
  the answer to a problem the participants have already met.
- **Current stack.** Spring Boot 4 (Spring Framework 7, Spring Security 7, Hibernate 7) on a Java 17 baseline
  that runs unchanged on 21 and 25, with the
  clients and patterns Spring recommends today, not the ones it has put into maintenance mode.
- **What the request leaves out, we put back:** security, testing, transactions under concurrent load, one
  error contract, observability, and containers. A microservice system without these is a demo, not a system.
- **Apache Camel is the one requested topic our standard programme does not include.** We propose it as a
  dedicated add-on day built on the same system, once the questions in section 5 are answered.

## 2. Where each requested topic lands

| Requested (their day) | Where it is in our programme | What changes |
|---|---|---|
| Spring introduction, dependency injection, `@Component`, auto-wiring, collections, `@Configuration` (Day 1) | Core Day 1, sessions 2–3 (after session 1's plain-Java monolith and its problem register) | Every way to define and inject a bean, scopes, the full bean lifecycle and post-processors, taught on the monolith whose wiring the container takes over. Constructor injection as the default; `@Autowired` on fields explained as the legacy pattern. |
| Why Spring Boot, set-up, opinionated defaults, Initializr, custom configuration, Maven, `@SpringBootApplication`, `@EnableAutoConfiguration`, `@ComponentScan`, starters (Day 1) | Core Day 2, session 1 | The same project is converted from plain Spring to Boot, so auto-configuration is a visible diff. Participants read the conditions report and then **write their own starter**, which is the proof they understood it. |
| Data starter with Oracle (Day 1) | Core Day 2 (PostgreSQL), Day 4 (MySQL and MongoDB) | Three database engines instead of one, behind the same repository boundary. Oracle can replace one of the SQL engines (see question 3). |
| Actuator: endpoints, configuration, customisation (Day 2) | Core Day 2 session 1; Resilience add-on | Health groups, liveness and readiness, custom endpoints, then Micrometer metrics, Prometheus, Grafana and distributed tracing. |
| JPA and Spring Data, common JPA annotations, REST with JPA (Day 2) | Core Day 2, session 4; Day 3, sessions 1 and 3 | Migrations with Flyway, localised data, relationships and the N+1 problem fixed three ways, then transactions, propagation, isolation and optimistic and pessimistic locking demonstrated on a real race condition. |
| Spring MVC, REST, `@RestController`, `@RequestBody`, `@ResponseBody`, GET/POST/PUT/DELETE, Postman (Day 2) | Core Day 2, sessions 2–3 | Resource design, status codes and idempotency, validation, **one error contract (`ProblemDetail`) across every service**, paging and filtering, API versioning, OpenAPI with Swagger UI. `.http` files and a Postman collection that grow with the course. |
| REST clients, `RestTemplate`, content-type and accept headers (Day 2) | Core Day 4, session 1 | `RestClient` and declarative HTTP interfaces (`@HttpExchange`), which are Spring's current recommendation; `RestTemplate` is in maintenance mode and is shown only for reading existing code. Timeouts, connection pools and error mapping are part of the lab, because a client without a timeout is the first outage. |
| Web services overview, SOA vs microservices, topology, granularity, database scope, deployment, trade-offs (Day 3) | Core Day 4, session 1 | The same architecture discussion, then applied: participants split their own modular monolith, extracting the catalogue first, with a database per service, and feel the trade-offs when a checkout spans four services and step three fails. |
| Eureka server and client, Feign, service discovery, load-balancing rules (Day 3) | Core Day 4, session 3; Resilience add-on, session 1 | Eureka and client-side load balancing in the core; OpenFeign compared with HTTP interfaces, registry health and self-preservation, and why Kubernetes makes Eureka optional, in the add-on. |
| Fault tolerance, circuit breaker, Resilience4j (Days 3–4) | Resilience add-on, session 2 | Failures injected on purpose, then timeouts, retries with backoff and idempotency keys, circuit breakers, bulkheads and rate limiters, and what the customer sees when checkout degrades gracefully. |
| Spring Cloud Config, profiles, Git repository (Day 4) | Core Day 4, session 3 | Plus encrypted secrets, what never goes into Git, and refresh at runtime with `@RefreshScope`. |
| Spring Cloud Gateway, filters, a logging filter, predicates (Day 4) | Core Day 4, session 4 | Routes, predicates, custom global filters, CORS once at the edge, rate limiting with Redis, and on Day 5 JWT validation at the edge with token relay to the services. |
| Apache Camel and enterprise integration patterns, ActiveMQ, Kafka with Camel (Day 5) | **Not in the standard programme.** Proposed as an add-on day (section 4). | Kafka itself, reliable consumers, the outbox and sagas are a separate add-on day. File and feed integration is covered with Spring Batch in the Integrations add-on. |

## 3. What we add, and why it matters for the stated goal

The request describes topics. Our programme describes a system, and the topics are the steps that build it.
That difference is what makes the learning stick, and it is also why the following are in the programme even
though they are not in the request.

- **Security (Core Day 5).** A microservice system has an edge and services calling each other. Without JWT
  validation at the gateway, token relay, ownership rules and service-to-service credentials, every service is
  open. We teach Spring Security from the filter chain to Keycloak with OAuth2 and OpenID Connect.
- **Testing (Core Day 3, and two add-on days).** Unit tests, web and data slices, Testcontainers against real
  databases, and in the add-ons consumer-driven contracts between services, Playwright on the storefront and
  load tests. A team cannot refactor a service it cannot test.
- **Transactions and concurrency (Core Day 3).** The request mentions JPA but not what happens when two
  requests update the same cart. We demonstrate the race, then fix it with optimistic and pessimistic locking.
- **One error contract and live documentation (Core Day 2).** Every service answers errors the same way and
  publishes OpenAPI. This is what lets a storefront team and a back-end team work in parallel.
- **Observability (Resilience add-on).** Metrics, one trace followed across five services, and logs that
  carry the trace id. Without this, Day 4's circuit breakers cannot be seen opening.
- **Real integrations (Integrations add-on).** A payment provider with signed webhooks, an e-mail provider
  behind a port with an offline adapter, a partner API behind WireMock, and batch feeds. These are the parts
  of an enterprise system that fail most often, and the resilience patterns are applied to them.
- **A storefront, containers and a pipeline (add-ons).** The system can be run end to end, containerised,
  composed, built in CI and deployed, which is where "microservices" stops being a slide.
- **Three database engines.** PostgreSQL, MySQL and MongoDB each arrive where their strengths are the lesson,
  behind the same repository boundary, so participants see what changes between engines and what does not.
- **A format that holds its timing.** Every session is a short concept block followed by labs participants
  type themselves, with minutes, a starter project and a step-by-step guide. A team that runs over on one
  lab still starts the next session on time, because the next starter is the previous session finished.

## 4. The topics in the request that we would add for this client

**Apache Camel and enterprise integration patterns.** Camel is a sound choice for an organisation with an
existing integration estate, and the requested Day 5 is a coherent Camel curriculum. It is not in our standard
programme because, in a Spring Boot microservice system built from scratch, the same needs are met by Spring
Batch (files and feeds), Spring Kafka (messaging) and HTTP interfaces (REST calls), all of which we teach.
If Camel is a requirement, we propose **an add-on day, "Enterprise integration with Apache Camel"**, built on
ShopScope rather than on isolated examples:

1. The first route, Spring beans and processors; the supplier feed as a file route with JSON and XML unmarshalling.
2. ActiveMQ in a container; a sender route in the order service and a receiver route in the notification service.
3. Content-based routing, choice and Simple; splitter and aggregator on the feed; HTTP calls to the catalogue from a route.
4. Multicast, routing slip and dynamic routing for the checkout; Kafka through Camel; dynamic configuration, logging and best practices.

**Oracle.** Our labs run PostgreSQL, MySQL and MongoDB in containers. If Oracle is the client's production
database, the SQL labs can run on Oracle Database Free in a container with the same JPA code, or Oracle can
replace MySQL as the second engine. The trade-off is a larger container image and slower start-up on
participant laptops.

**ActiveMQ.** Not in our standard programme, which uses Kafka. It would be covered in the Camel day, or the
Kafka add-on can be delivered on ActiveMQ Artemis with JMS if that is the client's broker.

## 5. Questions before we finalise the programme

**Duration and format**
1. The request says 8 half-days but lists five days of content. Is Day 5 (Camel) inside the 8 half-days or in
   addition to them? How long is a half-day, and are sessions remote or in a room?
2. Is the preference for half-days fixed? Our material is designed as full days with four sessions; it maps
   to half-days cleanly as two sessions each, but labs that span a break lose momentum.

**Apache Camel**
3. Is Camel a hard requirement because of an existing Camel code base, or an area the team is evaluating?
   If existing: which Camel version, which components, and which patterns (splitter, aggregator, routing
   slip, dynamic router) are actually in production?
4. Which broker do you run, ActiveMQ Classic, Artemis, or Kafka? Both ActiveMQ and Kafka appear in the request.

**Platform and versions**
5. Is Oracle mandatory for the labs (and is a development instance available to participants), or will
   PostgreSQL and MySQL in containers do, with Oracle covered as a configuration difference?
6. Which Java and Spring Boot versions run in production today? Our material is Spring Boot 4 on a Java 17
   baseline (it runs unchanged on 21 and 25).
   If the estate is on Boot 2 or 3, the capstone day includes an upgrade lab with OpenRewrite.
7. Where do the services run: virtual machines, Docker, Kubernetes, or a cloud platform? This decides how
   much weight Eureka and Config Server carry against platform services.

**Scope**
8. Security, testing and containers are absent from the request. Is that deliberate (covered elsewhere) or
   an omission? We recommend at least the security day for any microservice programme.
9. The request lists `RestTemplate`. Should the course teach what the team's code uses today, what Spring
   recommends now (`RestClient`, HTTP interfaces), or both?

**Participants and environment**
10. How many participants, and what is their Spring experience in practice? "Good knowledge of Spring"
    changes whether Day 1 is a refresher or the foundation.
11. Can participants run Docker Desktop on their machines, and do they have internet access for the sandbox
    providers? If not, every lab has an offline path (WireMock, Mailpit, GitHub Codespaces).
12. Is an assessment or a capstone review wanted at the end?

## 6. Two ways to fit the request

**Option A, 4 days (8 half-days), closest to the request.** Core Day 1, Core Day 2, Core Day 4, and the
Resilience add-on. Covers every requested Spring Boot and Spring Cloud topic. Leaves out MySQL, MongoDB,
search, transactions, testing, security and Camel.

**Option B, 7 days, recommended.** The 5-day core, the Resilience add-on, and the Camel add-on day. Covers
every requested topic including Camel, and adds security, testing, transactions and three database engines.

Either option is a selection on the *Build your programme* sheet of the TOC workbook; the day-by-day
schedule updates with it.
