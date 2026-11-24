---
name: technical-design
description: Produces a lightweight technical design with alternatives, trade-offs, and a delivery plan. Use when the user asks for a design, an approach, an ADR, or a comparison of solutions before or during implementation.
---

# Technical design

Design the smallest solution that meets the known requirements. Do not turn the design into a speculative platform. Write an ADR in `docs/adr/` when the decision is hard to reverse or needs to be shared. Otherwise keep the design in the conversation.

## Contents

Cover only the sections that the problem needs.

1. **Problem.** Who is affected and what is failing or missing.
2. **Functional requirements.** Observable behavior and acceptance criteria.
3. **Non-functional requirements.** Only the ones stated or clearly implied by the context, such as latency, consistency, or audit.
4. **Constraints and assumptions.** Include assumptions that would change the design.
5. **Alternatives.** At least two when a real choice exists, including the option to do less. Say which requirements each one meets.
6. **Decision.** The chosen structure, boundaries, and important interfaces. Prefer no new module, layer, or dependency until one is required.
7. **Trade-offs.** What the choice makes easier and what it makes harder.
8. **Data.** Model, ownership, and consistency boundary when the problem has state.
9. **Failures.** Invalid input, dependency failure, timeout, and partial completion when those can happen.
10. **Security.** Data, access, and secrets touched by the design. Skip controls that do not apply.
11. **Observability.** The signals an operator needs for this design. Skip a telemetry platform when logs are enough.
12. **Tests.** Unit behavior, integration boundaries, and what WireMock must stub if an outbound HTTP call exists.
13. **Delivery plan.** Increments that can be reviewed independently.

## Decision rule

If two options meet the same requirements and the simpler one remains readable, testable, and safe, choose the simpler one. Name that choice in the design.
