# ShopScope Commerce · Java microservices track

One enterprise e-commerce system, migrated step by step from a plain-Java monolith to a containerised, secured,
observable set of microservices with a Next.js storefront and a CI/CD pipeline:
- **Storefront:** a Next.js 16 app, built from scratch in its own track.
- **Backend:** Spring Boot services behind a gateway, one per module of the monolith you start with, each with
  its own database: PostgreSQL, MySQL or MongoDB, chosen per service.
- **Integrations:** Razorpay (payments), Resend (e-mail), a shipping partner and supplier feeds.

**Java 17 baseline.** Every starter compiles with `--release 17` and runs unchanged on 17, 21 and 25. Spring
Boot 4 and Spring Framework 7 support Java 17.

## What is here now (review build)

| Demo | Folder | Status |
|---|---|---|
| Demo 0 · The Java you will write ShopScope in | [`spring-boot-and-microservices/00-the-java-you-will-write-shopscope-in/`](./spring-boot-and-microservices/00-the-java-you-will-write-shopscope-in/) | guide, walkthrough, starter |
| Demo 1 · ShopScope as a plain-Java monolith | [`spring-boot-and-microservices/01-shopscope-as-a-plain-java-monolith/`](./spring-boot-and-microservices/01-shopscope-as-a-plain-java-monolith/) | guide, walkthrough, starter, and `solution/` (which becomes Demo 2's starter) |
| Study notes 01–04 | [`study-notes/`](./study-notes/) | modern Java; Maven, JUnit and AssertJ; SQL, JDBC, documents and HTTP; the monolith and its problems |

```bash
cd spring-boot-and-microservices/01-shopscope-as-a-plain-java-monolith/starter
./mvnw -q verify                              # build and test (JDK 17+)
./mvnw -q exec:java -Dexec.args=--demo        # one scripted checkout
./mvnw -q exec:java                           # the HTTP server on :8080; see http/monolith.http
```

## The plan

The full plan, every demo and lab, the target architecture and the repository layout are in
**[COURSE-OUTLINE.md](./COURSE-OUTLINE.md)** (draft v2.2). Demo titles are steps in the journey, with the topics
underneath.

| Track | Folder | Demos |
|---|---|---|
| Spring Boot and microservices: from a plain-Java monolith to a secured, observable system | [`spring-boot-and-microservices/`](./spring-boot-and-microservices/) | 00–25 |
| Next.js storefront | `nextjs-storefront/` | 26–32 |
| Testing | `testing/` | 33–36 |
| Docker | `docker/` | 37–38 |
| Docker Compose | `docker-compose/` | 39–40 |
| GitHub Actions | `github-actions/` | 41–42 |
| Event-driven (Kafka) | `event-driven/` | 43–46 (planned) |
| Kubernetes | `kubernetes/` | 47–50 (planned) |
| Spring AI | `spring-ai/` | 51–54 (planned) |
| Capstone | `capstone/` | 55 (planned) |
| Apache Camel (client add-on) | `camel/` | 56–59 (on request) |

**The starter of demo N+1 is demo N, finished**, across tracks too. The storefront and testing tracks start
from the finished backend of Demo 25; Docker starts from the finished storefront and tests.

The client-facing TOC workbook lives in [`toc/`](./toc/); client proposals in [`proposals/`](./proposals/).
