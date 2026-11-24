---
name: interview-mode
description: Runs a time-boxed programming interview of about 60 minutes. Use when the user is in a live coding interview, a timed coding exercise, or asks for interview mode. Optimizes for a working demo, clear technical decisions, and visible use of AI.
---

# Interview mode

Help the user solve an unfamiliar problem with AI as a productivity tool while the user keeps the technical decisions. This is not a reduced feature-delivery checklist and it is not a technical-design document. Success is a working solution the user can run, explain, and extend.

A 60-minute interview is the reference. Treat the split as a guide, not a script. Keep watching the remaining time and the real progress.

- Understand the problem and resolve ambiguities that would change the solution.
- Plan a short path.
- Implement the main flow.
- Test and validate it.
- Review and present it.

When time is short, make the main flow work, drop secondary behavior, skip nonessential documentation, avoid new dependencies, and keep the tests that protect the demo. The application must still run.

## Steps

1. Restate the problem. Separate explicit requirements from implied ones.
2. Ask only the questions that would change the solution. State the assumption and continue when the interviewer is not available to answer.
3. Define the minimum acceptance criteria for an end-to-end demo.
4. Cut the scope to the smallest slice that demonstrates that behavior.
5. Pick a proportional structure. A simple challenge does not need full DDD, Clean Architecture, extra modules, or extra infrastructure.
6. Name the technical risks and the simplifications.
7. State a short implementation plan before editing.
8. Implement in small steps the user can follow. Review generated code before relying on it. Question a decision that looks wrong.
9. Use the agent for implementation when that is faster. Delegate to a subagent only when parallel work has a clear payoff. Do not accept generated code without reading it.
10. Run the relevant tests and the application. Fix failures without weakening assertions.
11. Close with a demo script: what works, how it was verified, which assumptions were made, what was simplified, what is not implemented, and what would be next with more time.

## Communication

Encourage the user to say, in their own words:

- what is being built
- which assumptions and decisions were made, and why
- what was simplified or left out
- how the solution was validated

## Do not

- Apply every rule and skill by habit.
- Add architecture, dependencies, or speculative features that the demo does not need.
- Let a large unreviewed generation land.
- Hide limitations or unfinished behavior.
- Sacrifice a working demo for ceremony.

## Prompts that show control of the AI

Suggest short prompts such as:

- "Implement only the happy path of X. Do not add persistence."
- "Review this diff for bugs and unused abstraction. Do not edit it."
- "Add one test for the failure case we just found."
