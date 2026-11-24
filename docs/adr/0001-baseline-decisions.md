# ADR 0001: Engineering baseline

Status: Accepted

Date: 2026-11-24

## Context

The workspace was empty. The goal is a reusable, AI-oriented engineering environment and a small Kotlin and Spring Boot application that can evolve. The application has one current behavior: report whether the process is alive and ready. There is no persistence and no outbound product integration.

Java 25 is the required runtime. The machine used to create the baseline had Java 21 and Java 17 installed, not Java 25. Spring Initializr returned an internal error while resolving the Spring Boot 4.2.0 BOM, so the build was assembled from the published artifacts and the Spring Boot 4.2 reference material instead of a generated project.

## Decisions

### Single Gradle module

The application is one process with one package of behavior. Extra modules would add build and dependency overhead without an independent lifecycle. Package by feature when a second behavior appears. Split a Gradle module when a part needs its own dependencies or release cadence.

### Gradle Kotlin DSL

The application code is Kotlin, and the Spring Boot, Kotlin, Spotless, and Detekt plugins used here support the Kotlin DSL. One language for the build and the code is easier to maintain than introducing Groovy for a single build file. This is not a standing rule. A Groovy build is appropriate when the people maintaining it are more effective there or a plugin makes the Kotlin DSL awkward. Record that choice in a new ADR.

### Versions

Checked against current official or publisher metadata on 2026-11-24. Newer is not automatically better.

| Piece | Version | Reason |
| --- | --- | --- |
| Java | 25 | Requested LTS. Spring Boot 4.2 supports 17 through 27. Gradle 9.1+ runs on and compiles 25. |
| Spring Boot | 4.2.0 | Latest stable release. 4.2.1 was still a snapshot. |
| Kotlin | 2.4.21 | Version Spring Boot 4.2.0 manages and tests. The Kotlin plugin version drives the alignment. Kotlin 2.4 already applies parameter-property annotation targets, so `-Xannotation-default-target=param-property` is not set. |
| Gradle | 9.9.2 | Current stable wrapper. Runs on the installed JDK 21 and compiles with a JDK 25 toolchain. |
| JDK provisioning | Foojay resolver 0.10.0 | Stable resolver. 1.0.0 was a release candidate. Downloads a JDK 25 only for this build. |
| JUnit Jupiter | 6.1.3, from the Spring Boot BOM | `spring-boot-starter-test` 4.2.0 depends on Jupiter 6.1.3. The API package remains `org.junit.jupiter`. Pinning JUnit 5 would fight the BOM. |
| MockK | 1.14.12 | Latest stable. Used at the Spring `Environment` boundary in unit tests. |
| WireMock | 3.13.2 standalone | Latest stable 3.x. 4.0 was still beta. The plain artifact expects Jetty 11. `wiremock-jetty12` expects Jetty 12.0.30, and Spring Boot 4.2 aligns Jetty to 12.1, which removes `Environment.ensure` and crashes the server. The standalone jar shades its HTTP server, so the integration tests do not depend on the application's Jetty version. |
| Spotless | 8.12.0 | Current stable plugin, with ktlint. |
| Detekt | 2.0.0-alpha.6 | Detekt 1.23.8 is built with Kotlin 2.0.21 and cannot read Kotlin 2.4 metadata. No stable 2.0 exists. Alpha 6 is built with Kotlin 2.4.10 and refuses to run on 2.4.21. The `detekt` configurations pin `org.jetbrains.kotlin` to 2.4.10 so Spring Boot's alignment does not replace that compiler. Application code stays on 2.4.21. |
| JaCoCo | 0.8.16 | Current stable. Reports are generated. A coverage percentage is not a gate until there is enough business code to make the number meaningful. |
| Jackson Kotlin | `tools.jackson.module:jackson-module-kotlin` | Spring Boot 4 uses Jackson 3.1.8. The version comes from the imported Jackson BOM. |

Spring Boot's dependency-management plugin is applied without its own version so it stays on 1.1.7, the version the Spring Boot 4.2.0 plugin brings.

### Tests

Unit tests live in `src/test/kotlin` and run with `./gradlew test`. Integration tests live in `src/integrationTest/kotlin` and run with `./gradlew integrationTest`. `check` runs both.

The health endpoint is tested through Spring Boot's MockMvc support, which loads the application context and the web stack without a socket. WireMock is not used for that test because nothing is calling another service.

`WireMockHttpBoundaryIntegrationTest` starts WireMock and calls it with `java.net.http.HttpClient`. The product does not have an outbound client yet. The test exists so the integration suite proves that the chosen WireMock release runs on this JDK and shows the request-verification pattern for the next outbound HTTP boundary. It is not a reason to add a fake external product API.

Testcontainers is not a dependency.

### Quality gate

`check` depends on Spotless, the plain Detekt task, type-resolved Detekt for main, unit tests, and integration tests, the test suites, and the JaCoCo report that is finalized by the unit-test task. `fastCheck` is the inner loop: Spotless, type-resolved Detekt for main and unit tests, and unit tests. Detekt's line length is 140 so it matches ktlint's default. `FunctionNaming` ignores functions annotated with `@Test`, so backtick test names stay allowed. The override is `config/detekt/detekt.yml`. Everything else uses Detekt's default rules. The application entry point suppresses `SpreadOperator` because `runApplication(*args)` is the Spring Boot Kotlin entry point and the copied array is the process arguments.

No Git hook is installed. A hook that runs the full suite on every commit is slower than the problem it prevents. The git workflow rule still requires `fastCheck` during iteration and `check` before delivery. The agent does not commit unless asked.

No shell scripts wrap Gradle. The tasks are already one command.

### Health

Spring Boot Actuator exposes three HTTP endpoints:

- `GET /actuator/health` is the overall status.
- `GET /actuator/health/liveness` reports that the process can keep running.
- `GET /actuator/health/readiness` reports that the process can accept traffic.

Probes are enabled explicitly so both groups exist outside Kubernetes. No external dependency is checked. A small `ApplicationMetadata` indicator adds the application name and environment. Details are `always` in the `dev` profile and `never` in the default and `prod` profiles.

### Environments

`application.yaml` holds the shared configuration. `dev` and `prod` override the environment name, log level, and health detail visibility. Secrets are not stored in the repository. `.env.example` lists optional non-secret variables. Spring Boot reads `SPRING_PROFILES_ACTIVE` and `SERVER_PORT` from the environment without a dotenv library.

### Observability and security

Console logging is enough. The `dev` profile sets `interview.jusbrasil` to `DEBUG`. There is no JSON logger, metric registry, or tracer. The only HTTP route is the public health endpoint, and production responses omit details. Authentication will be added with the first non-public capability.

### Cursor layout

Rules in `.cursor/rules` hold standing constraints. Skills in `.cursor/skills` hold procedures. Subagents in `.cursor/agents` hold independent review roles. Slash commands in `.cursor/commands` are short prompts that run a Gradle task or point at a skill. These are the current Cursor locations. Nothing is duplicated into a second custom format.

## Consequences

The first build needs network access so the wrapper can download Gradle 9.9.2 and a JDK 25. `java -jar` fails when the default `java` is older than 25. Use the toolchain JDK or `./gradlew bootRun`.

Detekt is an alpha. Spotless with ktlint is the stable formatter. Revisit Detekt when 2.0 is stable.

The WireMock test does not protect product behavior. Remove it only when a real outbound client test replaces the pattern. Do not point it at a live service.

The Kotlin package is `interview.jusbrasil`. A hyphen is not a legal package identifier, so the project slug `interview-jusbrasil` is the Gradle group and the package uses a dot.
