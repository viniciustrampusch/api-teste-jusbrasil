# Code review report

## 1. Executive summary

What was reviewed, the verdict, and the highest severity that has evidence.

## 2. Requirements traceability

| Requirement or acceptance criterion | Evidence | Status |
| --- | --- | --- |
| | file:line or test name | Met / Partial / Missing / Not applicable |

## 3. Findings

Repeat per finding. Group by category. Use Confirmed defect, Potential risk, or Recommendation.

### [P2][Confirmed defect] Short title

- Category:
- Location: `path:line`
- Evidence:
- Impact:
- Suggested correction:

## 4. Severity summary

| ID | Severity | Kind | Title |
| --- | --- | --- | --- |
| | P0-P4 | Confirmed / Risk / Recommendation | |

## 5. Verification

| Check | Command | Result |
| --- | --- | --- |
| Unit tests | `./gradlew test` | Paste the real summary or "not run" and why |
| Integration tests | `./gradlew integrationTest` | |
| Formatting | `./gradlew spotlessCheck` | |
| Static analysis | `./gradlew detekt` | |
| Full gate | `./gradlew check` | |

## 6. Known limitations

What was outside the diff, what could not be executed, and which assumptions remain open.

## 7. Delivery assessment

Whether the change meets the original acceptance criteria, and what must happen before it is ready.

## 8. Remediation plan

### Mandatory

| Finding | Priority | Files | Approach | Tests |
| --- | --- | --- | --- | --- |
| | | | | |

### Optional

| Finding | Why it can wait | Files | Approach |
| --- | --- | --- | --- |
| | | | |

## 9. Pull request comments

Published comments, skipped comments, and the reason. Write "No open pull request" when that is the case.
