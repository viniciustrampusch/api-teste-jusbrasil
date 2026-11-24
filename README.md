# Engineering baseline

Reusable AI-first engineering environment and a small Kotlin and Spring Boot application. The application reports liveness and readiness. The `dev` profile connects to the local PostgreSQL container. It does not use a message broker or a distributed-tracing stack.

Decisions and version rationale: [docs/adr/0001-baseline-decisions.md](docs/adr/0001-baseline-decisions.md).

## Principles

The engineer owns the decisions. AI drafts and checks. Assumptions are explicit. Changes stay small.

- DRY: remove real duplication. Do not hide a one-line repetition behind a new abstraction.
- KISS: choose the simplest correct design.
- YAGNI: do not build for a requirement that has not been asked.
- DDD and Clean Architecture are used when the domain needs them, not as a default folder tree.
- SOLID is applied when it removes a real problem.
- Security and observability match the exposure and the operational need.

## Layout

```text
.cursor/
  rules/          standing constraints
  skills/         procedures the agent follows
  agents/         independent reviewers
  commands/       slash commands
docs/adr/         decision records
src/main/         application
src/test/         unit tests
src/integrationTest/  boundary tests
```

One Gradle module. The behavior lives in `interview.jusbrasil`. Add another module only when a part has its own dependencies or release cycle.

## Stack

| Piece | Version |
| --- | --- |
| Java | 25, via the Gradle toolchain |
| Kotlin | 2.4.21 |
| Spring Boot | 4.2.0 |
| Gradle | 9.9.2 wrapper, Kotlin DSL |
| JUnit Jupiter | 6.1.3, managed by Spring Boot |
| MockK | 1.14.12 |
| WireMock | 3.13.2 |
| Spotless + ktlint | 8.12.0 |
| Detekt | 2.0.0-alpha.6 |
| JaCoCo | 0.8.16 |

Gradle Kotlin DSL is used because the project is Kotlin and these plugins support it. Detekt 1.23.8 cannot analyze Kotlin 2.4, so the build uses the Detekt 2 alpha that can. Its own configurations stay on Kotlin 2.4.10, which is the compiler that alpha was built with. JUnit Jupiter 6 is what Spring Boot 4.2 ships. The test API is still `org.junit.jupiter`. WireMock is the standalone jar because Spring Boot 4.2's Jetty alignment is incompatible with WireMock's embedded Jetty 11 and Jetty 12.0 modules. Testcontainers is not used.

The first run downloads Gradle and a JDK 25. The system `java` command can stay on an older release. Running the jar directly requires Java 25.

## Commands

Run these from the repository root.

| Goal | Command |
| --- | --- |
| Compile and build the jar | `./gradlew bootJar` |
| Format sources | `./gradlew spotlessApply` |
| Fast check: format, Detekt, unit tests | `./gradlew fastCheck` |
| Full check: format, Detekt, unit tests, integration tests, coverage report | `./gradlew check` |
| Unit tests | `./gradlew test` |
| Integration tests | `./gradlew integrationTest` |
| All tests | `./gradlew test integrationTest` |
| Development profile | `./gradlew bootRun --args='--spring.profiles.active=dev'` |
| Rebuild on change during development | `./gradlew bootRun --continuous --args='--spring.profiles.active=dev'` |
| Production jar | `./gradlew bootJar` |
| Production profile | `"$JAVA_25" -jar build/libs/engineering-baseline-0.1.0.jar --spring.profiles.active=prod` |

`JAVA_25` is a Java 25 executable. After the toolchain has downloaded one, it is under `~/.gradle/jdks`. `./gradlew bootRun --args='--spring.profiles.active=prod'` also uses the toolchain and does not require `JAVA_25` on the path.

Coverage HTML is written to `build/reports/jacoco/test/html` when unit tests run. There is no coverage percentage gate.

There are no shell wrappers. The Gradle tasks are the commands. There is no Git hook. Before a commit, run `./gradlew fastCheck` while iterating and `./gradlew check` before delivery. Do not commit unless you intend to.

## Profiles

Shared settings are in `src/main/resources/application.yaml`.

| Profile | Environment name | Health details | Application log level |
| --- | --- | --- | --- |
| default | `local` | hidden | `INFO` |
| `dev` | `dev` | shown | `DEBUG` for this application |
| `prod` | `prod` | hidden | `INFO` |

Optional environment variables, with no secrets:

```bash
export SPRING_PROFILES_ACTIVE=dev
export SERVER_PORT=8080
```

Names are listed in `.env.example`. Do not commit a `.env` file.

## Health

| Endpoint | Meaning |
| --- | --- |
| `GET /actuator/health` | Overall status |
| `GET /actuator/health/liveness` | Process can keep running |
| `GET /actuator/health/readiness` | Process can accept traffic |

```bash
curl -sS http://localhost:8080/actuator/health
curl -sS http://localhost:8080/actuator/health/liveness
curl -sS http://localhost:8080/actuator/health/readiness
```

With the `dev` profile, the body includes component details. `applicationMetadata` carries the application name and environment. The default and `prod` profiles omit component details. They still include the overall status and the `liveness` and `readiness` group names, which is the Actuator response when probes are enabled and details are hidden. No external system is probed.

## Short links

Links and clicks live in the process. Restarting it drops them. No extra install is required.

```bash
./gradlew bootRun
```

```bash
curl -sS -X POST http://localhost:8080/api/shorten \
  -H 'content-type: application/json' \
  -d '{"url":"https://example.com/docs"}'

curl -sS -D - -o /dev/null http://localhost:8080/CODE

curl -sS http://localhost:8080/api/urls

curl -sS http://localhost:8080/api/stats/CODE
```

`POST /api/shorten` accepts `{ "url", "expiresAt"? }` and returns `201` with `shortCode`, `shortUrl`, `originalUrl`, `createdAt`, and `expiresAt`. The URL must use `http` or `https`. A short code has 7 alphanumeric characters. `expiresAt`, when set, must be in the future. `GET /{code}` answers `301` to the original URL, `404` when the code is missing, and `410` when the link is expired. A successful redirect records one click. Repeating the request records another click. The `301` response sends `Cache-Control: no-store` so the browser does not reuse the first redirect.

`GET /api/urls` returns the links newest first. Each item includes `shortCode`, `shortUrl`, `originalUrl`, `createdAt`, `expiresAt`, and `clickCount`. `clickCount` counts every recorded redirect.

`GET /api/stats/{code}` returns `200` when the code exists, including an expired link and a link with no clicks. `totalClicks` is that same full count. `clicksByDay` is the last 30 UTC dates, ending today, oldest first, with `0` on days that have no clicks. `topReferrers` uses every click, keeps at most five rows, and orders them by `clicks` descending, then by `referrer` ascending. A missing referrer is `""`. An unknown code returns `404` with `{"message":"Short code not found"}`.

`./gradlew check` runs formatting, static analysis, unit tests, and integration tests.

## Git

Branch from `main` as `type/short-description`. Commit with:

```text
type(scope): description
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`, `ci`, `perf`, `security`.

Keep commits small. Inspect the diff. Do not skip validation. Use Git Flow only when the project asks for it.

## Cursor skills

Skills live in `.cursor/skills/<name>/SKILL.md`. Invoke one by name in Agent chat, or use the slash command that points at it.

| Skill | When | How |
| --- | --- | --- |
| `feature-delivery` | Implement a feature | Ask to deliver the feature, or name the skill |
| `technical-design` | Compare approaches before coding | Ask for a technical design |
| `code-review` | Review without editing | `/code-review` |
| `review-remediation` | Fix confirmed review findings | `/review-remediation` |
| `interview-mode` | About 60 minutes, working demo | Ask to start interview mode |

`code-review` does not change files. It writes a report and a fix plan. `review-remediation` changes code only for mandatory findings unless an optional item was approved. Neither skill commits or merges.

## Cursor subagents

Subagents live in `.cursor/agents`. Ask for one by name. The agent can also delegate when the description matches.

| Subagent | Access | Ask |
| --- | --- | --- |
| `code-reviewer` | Read-only | "Use the code-reviewer subagent to review the current diff." |
| `test-engineer` | Can add tests when asked | "Use the test-engineer subagent to find missing tests." |
| `security-reviewer` | Read-only | "Use the security-reviewer subagent to review this change." |

They report evidence. They do not invent findings. The security and code reviewers do not edit the tree.

## Local PostgreSQL

The `dev` profile connects to this container. `POSTGRES_PASSWORD` must be set in the environment or in an untracked `.env` file. `./gradlew bootRun` reads that file when the variable is not already set. The database name and user are `teste_ia`. The host port is `5432`.

```text
jdbc:postgresql://localhost:5432/teste_ia
```

The default and `prod` profiles do not open a database connection. There is no schema and no repository yet.

```bash
docker compose up -d --build
docker compose down
```

`docker compose down` keeps the named volume. Data remains until the volume is removed.

## Slash commands

Type `/` in Agent chat.

| Command | Action |
| --- | --- |
| `/build` | `./gradlew bootJar` |
| `/quality` | `./gradlew check` |
| `/test-unit` | `./gradlew test` |
| `/test-integration` | `./gradlew integrationTest` |
| `/test-all` | `./gradlew test integrationTest` |
| `/run-dev` | Boot with the `dev` profile and call health |
| `/run-prod` | Build the jar and run the `prod` profile on Java 25 |
| `/up` | `docker compose up -d --build` |
| `/down` | `docker compose down` |
| `/code-review` | Run the code-review skill |
| `/review-remediation` | Run the remediation skill |

## What is intentionally absent

No schema, no repository, no security framework, no distributed tracing, no multi-module build, no Testcontainers, no Git hook, and no shell script that only renames a Gradle task. The PostgreSQL driver and the `dev` datasource are present. Add one of the absent pieces when a requirement needs it.
