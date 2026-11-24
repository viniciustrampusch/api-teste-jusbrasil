---
name: review-remediation
description: Fixes confirmed findings from a code review or pull-request comments, with tests and a status update. Use when the user asks to address, fix, or remediate review findings. Does not commit or change pull-request state.
---

# Review remediation

Apply approved corrections from a code-review report or from pull-request comments. Do not commit, push, merge, or change pull-request state unless the user explicitly asks.

## Classification

- **Bug fix:** confirmed incorrect behavior. Implement it.
- **Quality fix:** confirmed maintainability, security, or test gap that the review marked mandatory. Implement it.
- **Optional recommendation:** do not implement it unless the user approved that item.

## Steps

1. Take the review report or the PR comments as input. Merge duplicates.
2. Check the current code. Close findings that are already fixed and record the evidence.
3. Order the remaining mandatory findings by severity and impact.
4. Propose the implementation plan: files, approach, and the test for each item. Then implement the approved mandatory items. Keep each change small.
5. Add or update tests that fail if the defect returns. Do not weaken existing assertions.
6. Run the narrowest check that proves the fix, then `./gradlew check` when the change touches behavior, build, or tests.
7. Review the diff. Remove drive-by edits.
8. Update every finding status: Fixed, Still open, Already fixed, or Deferred. Deferred is only for optional items or items the user told you to leave.
9. If the findings came from a PR and `gh` can reply, post one reply per fixed thread with the test that demonstrates the fix. If they came from a report, publish an updated status report. Say which updates were posted and which could not be posted.

## Status report

| Finding | Previous severity | Status | Evidence |
| --- | --- | --- | --- |
| | P0-P4 | Fixed / Still open / Already fixed / Deferred | file:line and command result |

Do not mark a finding fixed without a code change or a test result that shows it.
