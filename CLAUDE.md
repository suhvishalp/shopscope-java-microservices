# Authoring conventions · ShopScope Commerce (Java microservices track)

The source of truth for scope and order is `COURSE-OUTLINE.md`. Update it first when the plan changes, then the demos, then the TOC.

## The chain
- Every demo folder has `README.md` (the guide), `WALKTHROUGH.md` (the finished code explained) and `starter/`.
- **The starter of demo N+1 is demo N, finished.** When a demo changes, carry the change forward through every later starter.
- Demo titles are steps in the journey (what happens to ShopScope), with the topics underneath; folder names follow the title. Code that is not the lesson may be pre-built in a starter; the labs stay on the session's concepts.
- Track 1 starts from a plain-Java monolith (Demo 1, package `com.shopscope.monolith.<module>`) that later demos migrate to the services below.
- Demo numbers are global; tracks are folders (`spring-boot-and-microservices/`, `nextjs-storefront/`, `testing/`, `docker/`, `docker-compose/`, `github-actions/`, later `event-driven/`, `kubernetes/`, `spring-ai/`, `capstone/`).
- The storefront and testing tracks branch from the finished backend (Demo 25). A storefront starter is `site/` plus `backend/compose.yaml`.
- The last demo of each track ships `solution/`.

## Guides
- Same format as the React track (`~/personal/react-shopscope-demos`): each lab is *Problem → Concept → Steps → Verify → Watch out → Challenge → In the real world*, with minutes in the heading.
- Starters carry `TODO(lab-x.y)` markers in guide order.
- Each guide ends with interview questions on its topics.
- `study-notes/` holds the concept notes. Each note lists the demos that use it, and each guide links its notes.

## Code
- **Java 17 baseline**: every POM sets `maven.compiler.release` to 17; no Java 21+ syntax or APIs in starters (no pattern-matching `switch`, record patterns, `_`, virtual threads, `SequencedMap`). Mention 21+ features in prose as asides. Spring Boot 4.x (supports 17), the matching Spring Cloud train, Maven wrapper, one parent POM per starter.
- No Lombok: records and plain Java.
- Packages are `com.shopscope.<service>.<feature>`, organised by feature. Inside a feature keep the `bff-java` roles: `api`, `application`, `domain`, `infrastructure`. JPA and HTTP types never leave `infrastructure`.
- The service boundaries and the public `/api/*` contract mirror `bff-java` (see the mapping table in the outline). No commercetools platform; every service owns its database: PostgreSQL (catalog, pricing, inventory, identity), MySQL (order, payment), MongoDB (customer, notification).
- Providers: Razorpay for payments, Resend for e-mail (Mailpit as the offline adapter), WireMock for the rest.
- Shared cross-cutting code lives in `libs/shopscope-starter` (our own auto-configuration), introduced in Demo 4.
- Errors are `ProblemDetail` everywhere. Every service has `.http` request files under `http/`.
- **Ports:**

  | Port | Service |
  |---|---|
  | 8080 | gateway |
  | 8081 | catalog |
  | 8082 | pricing |
  | 8083 | order |
  | 8084 | inventory |
  | 8085 | customer |
  | 8086 | payment |
  | 8087 | notification |
  | 9000 | identity |
  | 8888 | config |
  | 8761 | discovery |
  | 5432 | PostgreSQL |
  | 3306 | MySQL |
  | 27017 | MongoDB |
  | 6379 | Redis |
  | 8180 | Keycloak |
  | 8025 | Mailpit |
  | 8089 | WireMock |
  | 8090 | assistant (Spring AI track) |
  | 9090 | Prometheus |
  | 3000 | storefront (Next.js) |
  | 3030 | Grafana |

## Reuse sources
- `~/commercetools/ct-b2c-dev-course/` (`github.com/commercetools/commercetools-sdk-developer-java-training`): `bff-java` for service boundaries, the four roles and the `/api` contract; `storefront/site` for the Next.js structure. Reuse the design, not the code or the brand.
- `~/personal/playwright-demos`: Playwright patterns for the testing track.
- `github.com/reva-java-jan-29-org/reva-java-0202-batch`: weeks 4–6, the microservices step-by-step guide.
- `~/uptimecrew/ai-native-workshops/`: `5-1-workshop-02-Sep` (Docker), `5.2-docker-compose-workshop`, `gh-workshop-17-aug` (GitHub Actions).
- These use Java 21, Boot 3.5 and Cloud 2025.0. Port them; don't copy them as they are.

## TOC workbook
- `toc/tools/spring.py` builds the client workbook from `toc/tools/spring_data.py`.
- Run it with `~/.venvs/docs/bin/python`, the only Python with openpyxl.
- The current workbook is v1 and predates the outline. Rebuild it once the outline is agreed.
