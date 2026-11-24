---
name: code-review
description: Reviews an implementation against the original requirements and produces an evidence-based report plus a remediation plan. Use when the user asks for a code review, PR review, or delivery review. Does not modify code.
---

# Code review

Review the implementation against the original requirements and acceptance criteria. Do not modify files. Do not commit, push, merge, approve, or change pull-request state. Implementation of fixes belongs to the `review-remediation` skill and starts only when the user asks for it.

Do not invent defects. Do not report a check as done without the command output or the code that supports it. Separate confirmed defects, potential risks, and recommendations.

## Procedure

1. Read the original requirement and acceptance criteria. If they are missing, say so and review only what can be checked from the code and tests.
2. Inspect the current diff or the files in scope. Note assumptions the code relies on.
3. Run the checks that the change requires and keep the real output:
   - `./gradlew test` for unit tests
   - `./gradlew integrationTest` when a boundary or integration test changed
   - `./gradlew spotlessCheck detekt` for formatting and static analysis
   - `./gradlew check` before a delivery review
4. For a non-trivial change, delegate in parallel to the `code-reviewer`, `security-reviewer`, and `test-engineer` subagents. Give them the requirement, the diff, and the command output. For a tiny change, review it directly and say that delegation was not useful.
5. Merge findings. Drop duplicates and anything without evidence.
6. Check for an open pull request. Do not create one.
7. Write the report using [report-template.md](report-template.md), then write the remediation plan below.

## Review dimensions

Check only the dimensions the change can affect. Say when a dimension does not apply.

- **Requirements:** implemented, partial, missing acceptance criteria, explicit assumptions, unused behavior.
- **Correctness:** bugs, wrong business rules, invalid transitions, boundaries, null handling, errors, races, data consistency, idempotency when relevant.
- **Maintainability:** unnecessary abstraction, overengineering, duplication, coupling, cohesion, long methods, unclear names, dead code, DRY, KISS, and YAGNI.
- **Performance:** inefficient queries, N+1, unbounded collections, missing pagination, blocking calls, wasteful allocation, resource leaks, needless network calls.
- **Security:** authentication, authorization, access control, injection, input validation, sensitive data, secrets in code or logs, insecure configuration, vulnerable dependencies, relevant OWASP Top 10 risks.
- **Architecture:** domain boundaries, dependency direction, where business rules live, invariants, infrastructure leakage, patterns used without a need, accidental complexity.
- **Tests:** unit coverage of behavior, integration coverage, missing failures and edge cases, weak or brittle tests, false confidence, WireMock at HTTP boundaries.
- **Observability:** useful logs, levels, correlation or trace identifiers when justified, metrics when justified, error visibility, sensitive data in logs, noisy logs.
- **Reliability:** timeouts, retries, circuit breakers when justified, idempotency, transaction boundaries, consistency, external-dependency failure, resource cleanup.
- **Delivery:** build, lint, static analysis, unit tests, integration tests, application startup, documentation, and configuration needed by the change.

## Severity

- **P0** Critical. The change cannot be accepted.
- **P1** High. Fix before delivery.
- **P2** Medium. Fix in this change when practical.
- **P3** Low. Small improvement.
- **P4** Informative. No change required.

## Pull requests

1. If this directory is not a git repository, or `gh` cannot see a PR for the current branch, say that no comments were published.
2. If a PR is open and `gh` can comment, publish confirmed P0-P2 findings on the relevant lines. Include severity, impact, and the suggested correction.
3. Skip repeated or purely stylistic comments.
4. Keep the full report in the conversation even when PR comments were published.
5. List which comments were published and which were not, with the reason.
6. Do not merge, approve, or edit the PR state.

## Remediation plan

Include this plan in the review. Do not execute it.

- Findings that require a fix, ordered by severity and impact.
- Mandatory fixes separate from optional improvements.
- Files and components affected.
- Correction approach.
- Tests that should prove each correction.

Optional recommendations are not implemented unless the user approves them.
