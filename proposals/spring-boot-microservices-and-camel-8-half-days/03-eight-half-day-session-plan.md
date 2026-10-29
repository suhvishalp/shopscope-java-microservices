# Microservices with Spring Boot and Apache Camel: the 8 half-day plan

Eight half-days of four hours. Each half-day has two hands-on sessions, a short break, and a wrap-up that
ends with the system running on every participant's machine. Times below assume a 09:00 start; the whole
plan shifts with the start time.

| Time | Block |
|---|---|
| 09:00 – 09:10 | Opening: where the system is, what today adds |
| 09:10 – 10:40 | Session 1 (90 min, concept block then labs) |
| 10:40 – 10:55 | Break |
| 10:55 – 12:25 | Session 2 (90 min) |
| 12:25 – 12:45 | Wrap-up: verify the system end to end, questions, what to read before next time |
| 12:45 – 13:00 | Buffer, or extra lab time for those who want it |

Module numbers refer to *02-proposed-programme.md*; the labs and their minutes are listed there.

---

## Half-day 1 · Spring Core and Spring Boot

**Outcome:** the ShopScope monolith runs, its problem register is written, and its hand-written wiring has
become a Spring Boot application with its own starter in a multi-module Maven build.

| Session | Topics | Labs (minutes) |
|---|---|---|
| S1 · The monolith, then Spring Core: dependency injection and the container | ShopScope as a plain-Java monolith, run and broken; the problem register; the wiring problem; dependency injection by hand; `ApplicationContext`; `@Configuration` and `@Bean`; `@Component` and scanning; `@Autowired` on constructors vs fields; `@Primary`, `@Qualifier`; auto-wiring a `List` and a `Map`; scopes | 1.1 (15) · 1.2 (20) · 1.3 (25) · 1.4 (10) |
| S2 · Spring Boot | Why Boot; opinionated defaults; the parent and starters; `@SpringBootApplication` = `@EnableAutoConfiguration` + `@ComponentScan` + `@Configuration`; the conditions report; customising configuration with `application.yml`, profiles and `@ConfigurationProperties`; Spring Initializr; Maven build; the web and data starters | 1.5 (20) · 1.6 (25) · 1.7 (15) · 1.8 (15) |

**Requested topics covered:** all of requested Day 1.
**Running at the end:** the ShopScope monolith as a Spring Boot application, built by `./mvnw verify`, with `PROBLEMS.md` filled in.

## Half-day 2 · REST APIs and JPA

**Outcome:** the catalogue service has a REST API for stores and products, one error contract, and a database
with migrations.

| Session | Topics | Labs (minutes) |
|---|---|---|
| S1 · REST APIs with Spring Boot | Spring MVC under REST; `DispatcherServlet`; `@RestController`, `@RequestBody`, `@ResponseBody`; GET, POST, PUT, DELETE; status codes and idempotency; `ResponseEntity`; validation; `ProblemDetail`; Postman and `.http` files | 2.1 (15) · 2.2 (10) · 2.3 (25) · 2.4 (20) · 2.5 (10) |
| S2 · Spring Data JPA | JPA, Hibernate and Spring Data; `@Entity`, `@Id`, `@GeneratedValue`, `@Column`, `@Enumerated`, `@Embedded`; Flyway; `JpaRepository`, derived queries, `@Query`, paging; the REST-with-JPA project | 2.6 (20) · 2.7 (15) · 2.8 (25) · a first look at relationships (demonstrated) |

**Requested topics covered:** Spring MVC project, common annotations, REST, `@RestController`, `@RequestBody`,
`@ResponseBody`, the four methods, Postman, REST with JPA, common JPA annotations, the REST + JPA project.
**Running at the end:** `catalog-service` on its database, with the Postman collection green.

## Half-day 3 · Relationships, Actuator, transactions and REST clients

**Outcome:** the order service exists with carts, orders and transactions, and calls the catalogue and pricing
services over HTTP. Actuator is on every service.

| Session | Topics | Labs (minutes) |
|---|---|---|
| S1 · Relationships and Actuator | `@ManyToOne`, `@OneToMany`, `@ManyToMany`, the owning side; the N+1 problem caught and fixed; Actuator: endpoints, exposure, configuration, health groups, `info`, a custom endpoint | 2.9 (25) · 2.10 (25) · concept 20 · buffer 20 |
| S2 · Transactions and REST clients | The order service; `@Transactional`, rollback rules, propagation; optimistic locking on a real race; `RestClient` with content-type and accept headers, timeouts and error mapping; `RestTemplate` for existing code; `@HttpExchange` interfaces; web services overview | 2.11 (25) · 2.12 (20) · 2.13 (25) · 2.14 (15) |

**Requested topics covered:** Actuator (introduction, configuration, customisation), REST clients,
`RestTemplate`, headers, microservices with Spring Boot (first contact), web services overview.
**Running at the end:** three services (`pricing`, `catalog`, `order`) calling each other, each with Actuator.

## Half-day 4 · Microservices architecture and service discovery

**Outcome:** the services register with a discovery server and call each other through load-balanced and
Feign clients; a checkout spans four services and the group has seen it fail.

| Session | Topics | Labs (minutes) |
|---|---|---|
| S1 · Architecture | SOA vs microservices; how microservices implement web-service concepts; protocols and standards; topology, granularity, database scope, deployment; advantages, disadvantages and primary benefits; ShopScope's map; checkout across four services; correlation ids | 3.1 (25) · 3.2 (25) · 3.3 (10) |
| S2 · Spring Cloud discovery | Spring Cloud; the Eureka server (`@EnableEurekaServer`) and clients (`@EnableDiscoveryClient`); load-balanced `RestClient` across two instances; load-balancing rules; Feign proxy and service discovery with Feign; health and self-preservation; fault tolerance introduced | 3.4 (20) · 3.5 (20) · 3.6 (20) · 3.7 (15) |

**Requested topics covered:** all of requested Day 3.
**Running at the end:** a discovery server, two catalogue instances, every call load-balanced.

## Half-day 5 · Resilience and central configuration

**Outcome:** the system survives a slow or failing inventory service, and every service reads its
configuration from one Git repository.

| Session | Topics | Labs (minutes) |
|---|---|---|
| S1 · Fault tolerance with Resilience4j | Failures injected on purpose; timeouts first; retries with backoff and idempotency keys; the circuit breaker pattern and fallbacks; bulkheads and rate limiters; graceful degradation in checkout | 4.1 (10) · 4.2 (15) · 4.3 (20) · 4.4 (25) · 4.5 (15) |
| S2 · Spring Cloud Config | The config server backed by Git; one file per service per profile; the client side and profiles; encrypted secrets; refresh at runtime | 4.6 (25) · 4.7 (15) · 4.8 (20) · buffer 20 |

**Requested topics covered:** circuit breaker pattern, Resilience4j, Spring Cloud Config, profiles, Git integration.
**Running at the end:** the circuit breaker seen opening and closing in Actuator; configuration served from Git.

## Half-day 6 · The API gateway, and the system end to end

**Outcome:** one gateway is the single entry point, with declarative routes, a logging filter and rate
limiting; the whole system runs end to end before Camel is added.

| Session | Topics | Labs (minutes) |
|---|---|---|
| S1 · Spring Cloud Gateway | Why a gateway; routes; predicates on path, method, header and query; `lb://` URIs; filters; a custom global logging filter; path rewriting; request ids | 4.9 (25) · 4.10 (25) · concept 15 · buffer 10 |
| S2 · The edge, and the system review | CORS at the edge; rate limiting with Redis; a guided review of the whole system: every call from the gateway to the database, traced with the request id; where Camel will plug in tomorrow | 4.11 (15) · system walk-through and verification (45) · retrospective and Q&A (30) |

**Requested topics covered:** API gateway, Spring Cloud Gateway, filters, logging filter, declarative predicates, methods and URLs.
**Running at the end:** the complete Spring Cloud system, five services behind one gateway.

## Half-day 7 · Apache Camel: routes, files and messaging

**Outcome:** Camel routes run inside the services: the supplier feed flows from a file into the catalogue,
and order events flow between two services over ActiveMQ and then Kafka.

| Session | Topics | Labs (minutes) |
|---|---|---|
| S1 · Getting started with Camel | Camel and enterprise integration; terminology and architecture (context, routes, endpoints, exchanges, components); the first route; Spring beans for transformation; processors; a file route; unmarshalling JSON and XML to Java beans; transformation after unmarshalling | 5.1 (20) · 5.2 (20) · 5.3 (30) |
| S2 · Messaging with Camel | ActiveMQ as a Docker container; a sender route in the order service; a receiver route in the notification service; the same flow over Kafka; calling the catalogue's REST API from a route | 5.4 (30) · 5.5 (20) · 5.6 (15) |

**Requested topics covered:** the first thirteen items of requested Day 5, through "making HTTP REST API calls from a Camel route".
**Running at the end:** order events moving between services over ActiveMQ and Kafka, the feed importing from a folder.

## Half-day 8 · Enterprise integration patterns with Camel

**Outcome:** the enterprise integration patterns applied to the feed and the checkout, with the best
practices for running Camel in production.

| Session | Topics | Labs (minutes) |
|---|---|---|
| S1 · Routing and transformation patterns | Choice and the Simple language; reusable endpoints; complex deciders; pipeline and multicast; content-based routing; the splitter on the supplier feed | 5.7 (25) · 5.8 (15) · 5.9 (25) |
| S2 · Aggregation, dynamic routing and best practices | Aggregation with completion rules; routing slip and dynamic routing on the checkout; best practices: dynamic configuration, stand-alone vs inside Spring Boot, logging and error handling; testing a route; closing retrospective | 5.10 (20) · 5.11 (25) · 5.12 (25) · close (15) |

**Requested topics covered:** the remaining items of requested Day 5, through "Camel best practices".
**Running at the end:** the complete system with Camel routes for files, messaging, HTTP and every pattern in the request.

---

## Coverage check

| Requested day | Half-days |
|---|---|
| Day 1 · Spring Framework and Spring Boot | 1 |
| Day 2 · Actuator, JPA, REST, clients, microservices overview | 2, 3 |
| Day 3 · Architecture, Spring Cloud, Eureka, Feign, load balancing | 4 |
| Day 4 · Circuit breaker, Config, API gateway | 5, 6 |
| Day 5 · Apache Camel and the integration patterns | 7, 8 |

Every topic in the request is covered. Half-day 3 adds transactions and locking, half-day 5 adds timeouts
and retries before the circuit breaker, and half-day 6 adds rate limiting and a full-system review; these are
the additions that make the system real rather than a set of examples.

## Lab time

| Half-day | Lab minutes | Share of the 205 working minutes |
|---|---|---|
| 1 | 145 | 71 % |
| 2 | 125 | 61 % |
| 3 | 135 | 66 % |
| 4 | 135 | 66 % |
| 5 | 145 | 71 % |
| 6 | 110 | 54 % (the rest is the guided system review, also at the keyboard) |
| 7 | 135 | 66 % |
| 8 | 135 | 66 % |

Working minutes exclude the opening, the break and the buffer.

## If the group is faster or slower

- **Faster:** every lab has a challenge, and the finished code of the next session is available for reading.
- **Slower:** the buffers in half-days 3, 5 and 6 absorb overruns, and every session starts from a finished
  starter, so an unfinished lab never blocks the next one.
- **Ninth half-day, optional:** Spring Security with JWT at the gateway, or testing with Testcontainers, on the same system.
