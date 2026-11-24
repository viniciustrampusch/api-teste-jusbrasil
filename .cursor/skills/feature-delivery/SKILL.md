---
name: feature-delivery
description: Implements a feature from requirements through tests, validation, and a completion report. Use when the user asks to build, implement, or deliver a feature, user story, or behavior in this repository.
---

# Feature delivery

Deliver the requested behavior in small steps. Prefer a simple design. Apply DRY, KISS, and YAGNI. Do not build speculative requirements. State important decisions and assumptions. Do not make destructive changes, commits, or pushes without explicit approval.

## Steps

1. Restate the requirement in concrete behavior. Separate facts from assumptions.
2. List acceptance criteria. Ask only the questions whose answers would change the design or the acceptance criteria.
3. Name the business rules, inputs, outputs, and failure cases.
4. Choose the smallest structure that fits. Reuse existing types and modules. Record a decision only when there is a real trade-off. See the `technical-design` skill when the problem has more than one credible design.
5. Split the work into increments that can be compiled and checked.
6. Implement the current increment. Do not add a layer, interface, dependency, or pattern without a current need.
7. Add unit tests for the business behavior, including failure and edge cases. Use MockK only at a real boundary.
8. Add an integration test when a boundary changed: HTTP entry, persistence, or an outbound HTTP call. Use WireMock for outbound HTTP. Do not use Testcontainers. Do not call real external APIs.
9. Run `./gradlew fastCheck` during the increment. Run `./gradlew check` before calling the feature done.
10. Review the diff against the original acceptance criteria. Remove unused code, speculative branches, and weak assertions.
11. Report what was delivered, what was verified and the actual command results, the assumptions, and the known limitations.

## Done

The acceptance criteria are met, the relevant tests exist, `./gradlew check` has been run, and the report distinguishes verified results from anything not executed.
