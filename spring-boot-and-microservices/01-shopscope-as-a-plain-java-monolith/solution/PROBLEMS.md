# The problem register

What is wrong with the monolith, found in Demo 1, and the demo that fixes each one. Re-read this at the
end of every part; the goal of the track is an empty table.

| # | Problem | Where you saw it | Fixed in |
|---|---|---|---|
| 1 | **Modules call each other directly.** `OrderService` calls `NotificationService`; an e-mail failure is a checkout failure. The dependency graph has arrows that should not exist. | Lab 1, Lab 6 | Demo 3 (events), Demo 13 (separate processes) |
| 2 | **Wiring by hand, and a hidden dependency.** The composition root is a page of `new` in a fixed order; `OrderService` creates its own `RazorpayTestGateway`, so no test can replace it. | Lab 2, Lab 3 | Demo 2 (the container, constructor injection) |
| 3 | **Nothing is testable in isolation.** Every test wires the real object graph by hand, and the gateway's rules leak into order tests. | Lab 3 | Demo 2, Demo 11 |
| 4 | **Configuration by hand.** A properties file parsed where it is read, tax rates hard-coded in `TaxRules`, a second environment means a second file and a system property. | Lab 4 | Demo 3 (`Environment`, profiles), Demo 4 (`@ConfigurationProperties`), Demo 14 (config server) |
| 5 | **Cross-cutting code pasted everywhere.** Timing and audit logging copied into every service method. | Lab 5 | Demo 3 (one aspect), Demo 22 (observability) |
| 6 | **The shared in-memory store, and the overselling bug.** Stock is decremented before payment and never restored when payment fails. No transaction, because there is no database. | Lab 6, `ProblemRegisterTest` | Demo 7 (PostgreSQL), Demo 10 (transactions, locking), Demo 13 (reservations) |
| 7 | **One process.** Everything scales together, fails together and deploys together. | Lab 1, Lab 8 | Demo 12 and Demo 13 (extraction), Demo 16 (the gateway) |
| 8 | **The hand-rolled HTTP layer.** String routing, a JSON parser that misreads anything nested, status codes chosen by hand, a 500 with the exception in the body. | Lab 7 | Demo 5 (Spring MVC), Demo 6 (`ProblemDetail`) |
| 9 | **Start-up and shutdown by hand.** The order of `main` is load-bearing; a shutdown hook has to remember every resource. | Lab 8 | Demo 2 (lifecycle), Demo 4 (Boot) |
