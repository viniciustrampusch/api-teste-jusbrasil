---
name: quality
description: Run the full quality gate
---

# Quality gate

From the repository root, run `./gradlew check`.

This task compiles, runs Spotless, Detekt, unit tests, integration tests, and the JaCoCo unit-test report.

Report the command, the exit status, and any failed task with the relevant output. Do not skip tasks or weaken tests to obtain a green result.

Use `./gradlew fastCheck` only when the user asked for the fast inner loop. Say which tasks that command omits.
