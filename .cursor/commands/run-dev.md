---
name: run-dev
description: Run the application with the dev profile
---

# Run in development

From the repository root, run:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The Gradle toolchain uses Java 25 even when the system `java` command is older. DevTools is on the development classpath only. The dev profile opens a connection to the local PostgreSQL container, so that container must already be running and `POSTGRES_PASSWORD` must be available.

After startup, request:

```bash
curl -sS http://localhost:8080/actuator/health
curl -sS http://localhost:8080/actuator/health/liveness
curl -sS http://localhost:8080/actuator/health/readiness
```

The dev profile includes health details. Do not use this profile as a production setting. Report the process result and the HTTP responses. Stop the process when the check is finished unless the user wants it left running.
